package org.example.reception.service;

import org.example.reception.client.BillingServiceAdapter;
import org.example.reception.client.MedicalRecordsClient;
import org.example.reception.dto.AppointmentResponse;
import org.example.reception.dto.CreateAppointmentRequest;
import org.example.reception.exception.ApiProblemException;
import org.example.reception.mapper.AppointmentMapper;
import org.example.reception.mapper.TimeSlotMapper;
import org.example.reception.model.Appointment;
import org.example.reception.model.TimeSlot;
import org.example.reception.model.TimeSlotStatus;
import org.example.reception.outbox.AppointmentOutboxService;
import org.example.reception.repository.AppointmentRepository;
import org.example.reception.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSlotOptimisticLockingIntegrationTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private BillingServiceAdapter billingServiceAdapter;

    @Mock
    private MedicalRecordsClient medicalRecordsClient;

    @Mock
    private AppointmentOutboxService appointmentOutboxService;

    @Spy
    private AppointmentMapper appointmentMapper = Mappers.getMapper(AppointmentMapper.class);

    @Spy
    private TimeSlotMapper timeSlotMapper = Mappers.getMapper(TimeSlotMapper.class);

    private TimeSlotReservationServiceImpl timeSlotReservationService;
    private AppointmentServiceImpl appointmentService;

    @BeforeEach
    void setUp() {
        timeSlotReservationService = new TimeSlotReservationServiceImpl(
                timeSlotRepository, appointmentRepository, timeSlotMapper);

        AppointmentPrerequisitesValidator validator = new AppointmentPrerequisitesValidator(
                medicalRecordsClient, billingServiceAdapter);

        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                billingServiceAdapter,
                appointmentMapper,
                validator,
                appointmentOutboxService,
                timeSlotReservationService
        );

        lenient().when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId("appt-" + System.nanoTime());
            }
            return a;
        });
    }

    @Test
    void shouldSuccessfullyBookAvailableSlotAndTransitionToBookedWithVersionIncrement() {
        LocalDateTime startTime = LocalDateTime.of(2030, 6, 15, 10, 0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        TimeSlot slot = TimeSlot.builder()
                .id("slot-100")
                .doctorId("doc-1")
                .startTime(startTime)
                .endTime(endTime)
                .roomNumber("204")
                .status(TimeSlotStatus.AVAILABLE)
                .version(0L)
                .build();

        when(timeSlotRepository.findById("slot-100")).thenReturn(Optional.of(slot));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> {
            TimeSlot s = inv.getArgument(0);
            s.setVersion(s.getVersion() != null ? s.getVersion() + 1 : 1L);
            return s;
        });

        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientId("pat-1")
                .patientFullName("Oksana Bondar")
                .timeSlotId("slot-100")
                .build();

        AppointmentResponse response = appointmentService.scheduleAppointment(request);

        assertThat(response).isNotNull();
        assertThat(response.timeSlotId()).isEqualTo("slot-100");
        assertThat(response.doctorId()).isEqualTo("doc-1");
        assertThat(response.appointmentDateTime()).isEqualTo(startTime);
        assertThat(response.roomNumber()).isEqualTo("204");
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.BOOKED);
        assertThat(slot.getHeldByPatientId()).isEqualTo("pat-1");
        assertThat(slot.getVersion()).isEqualTo(2L);
        verify(timeSlotRepository, times(2)).save(slot);
    }

    @Test
    void shouldFailWhenTwoPatientsConcurrentlyBookSameSlotViaOptimisticLocking() throws Exception {
        LocalDateTime startTime = LocalDateTime.of(2030, 6, 15, 14, 0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        AtomicLong currentDatabaseVersion = new AtomicLong(0L);
        AtomicReference<String> winningPatientId = new AtomicReference<>();

        when(timeSlotRepository.findById("slot-race")).thenAnswer(inv -> Optional.of(TimeSlot.builder()
                .id("slot-race")
                .doctorId("doc-1")
                .startTime(startTime)
                .endTime(endTime)
                .roomNumber("101")
                .status(TimeSlotStatus.AVAILABLE)
                .version(0L)
                .build()));

        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> {
            TimeSlot candidate = inv.getArgument(0);
            long expectedVersion = candidate.getVersion() != null ? candidate.getVersion() : 0L;
            boolean updated = currentDatabaseVersion.compareAndSet(expectedVersion, expectedVersion + 1);
            if (!updated) {
                throw new OptimisticLockingFailureException(
                        "Cannot save entity with version " + expectedVersion + " to collection time_slots. Concurrent modification.");
            }
            winningPatientId.compareAndSet(null, candidate.getHeldByPatientId());
            candidate.setVersion(currentDatabaseVersion.get());
            return candidate;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<AppointmentResponse> futurePatient1 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            return appointmentService.scheduleAppointment(CreateAppointmentRequest.builder()
                    .patientId("patient-alpha")
                    .patientFullName("Alpha Patient")
                    .timeSlotId("slot-race")
                    .build());
        });

        Future<AppointmentResponse> futurePatient2 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            return appointmentService.scheduleAppointment(CreateAppointmentRequest.builder()
                    .patientId("patient-beta")
                    .patientFullName("Beta Patient")
                    .timeSlotId("slot-race")
                    .build());
        });

        readyLatch.await();
        startLatch.countDown();

        int successes = 0;
        int optimisticLockFailures = 0;

        try {
            futurePatient1.get();
            successes++;
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof OptimisticLockingFailureException) {
                optimisticLockFailures++;
            }
        }

        try {
            futurePatient2.get();
            successes++;
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof OptimisticLockingFailureException) {
                optimisticLockFailures++;
            }
        }

        executor.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(optimisticLockFailures).isEqualTo(1);
        assertThat(winningPatientId.get()).isNotNull();
    }

    @Test
    void shouldRejectBookingWhenSlotIsAlreadyBooked() {
        LocalDateTime startTime = LocalDateTime.of(2030, 6, 15, 11, 0);
        TimeSlot bookedSlot = TimeSlot.builder()
                .id("slot-booked")
                .doctorId("doc-1")
                .startTime(startTime)
                .endTime(startTime.plusMinutes(30))
                .status(TimeSlotStatus.BOOKED)
                .heldByPatientId("pat-prior")
                .version(1L)
                .build();

        when(timeSlotRepository.findById("slot-booked")).thenReturn(Optional.of(bookedSlot));

        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientId("pat-new")
                .timeSlotId("slot-booked")
                .build();

        assertThatThrownBy(() -> appointmentService.scheduleAppointment(request))
                .isInstanceOf(ApiProblemException.class)
                .satisfies(ex -> {
                    ApiProblemException apiEx = (ApiProblemException) ex;
                    assertThat(apiEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiEx.getBody().getType().toString()).contains("slot-unavailable");
                });
    }

    @Test
    void shouldRejectBookingWhenSlotIsHeldByAnotherPatient() {
        LocalDateTime startTime = LocalDateTime.of(2030, 6, 15, 12, 0);
        TimeSlot heldSlot = TimeSlot.builder()
                .id("slot-held")
                .doctorId("doc-1")
                .startTime(startTime)
                .endTime(startTime.plusMinutes(30))
                .status(TimeSlotStatus.HELD)
                .heldByPatientId("pat-other")
                .version(1L)
                .build();

        when(timeSlotRepository.findById("slot-held")).thenReturn(Optional.of(heldSlot));

        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientId("pat-me")
                .timeSlotId("slot-held")
                .build();

        assertThatThrownBy(() -> appointmentService.scheduleAppointment(request))
                .isInstanceOf(ApiProblemException.class)
                .satisfies(ex -> {
                    ApiProblemException apiEx = (ApiProblemException) ex;
                    assertThat(apiEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiEx.getBody().getType().toString()).contains("slot-unavailable");
                });
    }

    @Test
    void shouldAllowBookingWhenSlotIsHeldBySamePatient() {
        LocalDateTime startTime = LocalDateTime.of(2030, 6, 15, 13, 0);
        TimeSlot heldSlot = TimeSlot.builder()
                .id("slot-held-mine")
                .doctorId("doc-1")
                .startTime(startTime)
                .endTime(startTime.plusMinutes(30))
                .status(TimeSlotStatus.HELD)
                .heldByPatientId("pat-me")
                .version(1L)
                .build();

        when(timeSlotRepository.findById("slot-held-mine")).thenReturn(Optional.of(heldSlot));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientId("pat-me")
                .timeSlotId("slot-held-mine")
                .build();

        AppointmentResponse response = appointmentService.scheduleAppointment(request);

        assertThat(response).isNotNull();
        assertThat(heldSlot.getStatus()).isEqualTo(TimeSlotStatus.BOOKED);
    }

    @Test
    void shouldRejectBookingWhenDoctorMismatch() {
        LocalDateTime startTime = LocalDateTime.of(2030, 6, 15, 15, 0);
        TimeSlot slot = TimeSlot.builder()
                .id("slot-doc-1")
                .doctorId("doc-1")
                .startTime(startTime)
                .endTime(startTime.plusMinutes(30))
                .status(TimeSlotStatus.AVAILABLE)
                .version(0L)
                .build();

        when(timeSlotRepository.findById("slot-doc-1")).thenReturn(Optional.of(slot));

        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientId("pat-1")
                .doctorId("doc-99")
                .timeSlotId("slot-doc-1")
                .build();

        assertThatThrownBy(() -> appointmentService.scheduleAppointment(request))
                .isInstanceOf(ApiProblemException.class)
                .satisfies(ex -> {
                    ApiProblemException apiEx = (ApiProblemException) ex;
                    assertThat(apiEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getBody().getType().toString()).contains("slot-mismatch");
                });
    }

    @Test
    void shouldRejectBookingWhenAppointmentDateTimeMismatch() {
        LocalDateTime startTime = LocalDateTime.of(2030, 6, 15, 16, 0);
        TimeSlot slot = TimeSlot.builder()
                .id("slot-time-1")
                .doctorId("doc-1")
                .startTime(startTime)
                .endTime(startTime.plusMinutes(30))
                .status(TimeSlotStatus.AVAILABLE)
                .version(0L)
                .build();

        when(timeSlotRepository.findById("slot-time-1")).thenReturn(Optional.of(slot));

        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientId("pat-1")
                .timeSlotId("slot-time-1")
                .appointmentDateTime(startTime.plusDays(1))
                .build();

        assertThatThrownBy(() -> appointmentService.scheduleAppointment(request))
                .isInstanceOf(ApiProblemException.class)
                .satisfies(ex -> {
                    ApiProblemException apiEx = (ApiProblemException) ex;
                    assertThat(apiEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getBody().getType().toString()).contains("slot-mismatch");
                });
    }
}
