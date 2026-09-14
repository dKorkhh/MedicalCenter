# Medical Center System

## Вибір бізнес-домену
Обрана предметна область - медичний сервіс з можливістю онлайн запису до лікаря. Система є платформою для управління записами на прийом до клініки, яка об'єднує планування розкладу лікарів, проведення фінансових операцій, ведення електронних медичних карток, управління ідентифікацією та інформування користувачів.

**Архітектурний стиль:** Event-Driven Microservices з брокером повідомлень як центральним транспортним шаром. Кожен мікросервіс є автономним, має власну базу даних та спілкується з іншими виключно через події або API-запити.

## Ключові бізнес-сценарії
* Перший сценарій описує оформлення запису, де пацієнт обирає вільний слот у розкладі лікаря, система ініціює перевірку оплати, фіксує бронювання та надсилає підтвердження. 
* Другий сценарій охоплює скасування запису пацієнтом, що призводить до звільнення слота та автоматичного повернення коштів. 
* Третій сценарій стосується завершення візиту: лікар заповнює медичну картку, а білінг формує фінальний акт.

## Виділення Bounded Contexts
Існуючі домени: 
* Домен Reception виступає операційним ядром, яке централізовано зберігає профілі пацієнтів та лікарів, управляє розкладами і виступає головним оркестратором процесів бронювання часу. 
* Домен Billing ізолює фінансові транзакції та обробку рахунків. 
* Домен Medical Records відповідає за збереження історії візитів як окремих документів.
* Домен Authorization + Auntification - це збереження пошти, логінів, хешів паролів, базових ролей користувачів та генерація токенів доступу. 
* Домен Notification слугує виключно для відправки сповіщень.

## Опис рішень 
Система створена на основі незалежних ікросервісів з event-driven взаємодією через брокер повідомлень. Кожен домен відпоідає тільки за свою частину функціоналу і ініціалізує події замість синхроних колів API. Синхронна взаємодія існує тільки між Reception та Billing при ініціації оплати та поверненні коштів, оскільки Reception не може зберегти Appointment без підтвердженого результату.  
Для автономності сервісів застосований Snapshot-патерн: у момент події кожен сервіс фіксує локальну копію необхідних даних замість того, щоб запитувати їх в іншого сервісу.
Reception виступає оркестратором - він керує розкладом, слотами та станом Appointment і публікує ключові події системи. Auth винесений в окремий сервіс, оскільки зберігає технічні дані автентифікації, не пов'язані з жодним бізнес-доменом.

## Схема контекстів (Context Map)
 
> Діаграма описує статичні відносини між контекстами: хто є Upstream (Supplier) і хто Downstream (Customer), а також характер взаємодії.
 
```mermaid
graph TD
    Auth["Auth\n(Authorization & Authentication)\n──────────────────\nUserCredentials\nRole\nRefreshToken"]
 
    Reception["Reception\n(Core Orchestrator)\n──────────────────\nPatientProfile\nDoctorProfile\nSchedule / TimeSlot\nAppointment\nPayment Snapshot"]
 
    Billing["Billing\n──────────────────\nPayment\nInvoice\nRefund\nAppointment Snapshot"]
 
    MedRecords["Medical Records\n──────────────────\nMedicalRecord\nVisitRecord\nVisit Snapshot"]
 
    Notification["Notification\n──────────────────\nNotification\nNotificationTemplate\nDeliveryAttempt\nRecipient Snapshot"]
 
    MessageBroker[["Message Broker"]]
 
    Auth -->|"userId + roles"| Reception
 
    Reception -->|"sync: ініціює оплату"| Billing
    Billing -->|"sync: результат оплати / повернення\n[зберігається як Payment Snapshot]"| Reception
 
    Reception -->|"AppointmentBooked\nAppointmentCancelled\nAppointmentCompleted"| MessageBroker
 
    Billing -->|"PaymentSucceeded\nRefundIssued"| MessageBroker
 
    MessageBroker -->|"AppointmentCompleted"| MedRecords
    MessageBroker -->|"AppointmentCompleted"| Billing
    MessageBroker -->|"AppointmentBooked\nAppointmentCancelled\nAppointmentCompleted\nPaymentSucceeded\nRefundIssued"| Notification
```

## Event-Driven Architecture: 
 
> Sequence Diagrams для кожного зі сценаріїв
 
### Сценарій 1 - Оформлення запису
 
```mermaid
sequenceDiagram
    actor Patient
    participant Reception
    participant Billing
    participant Broker as Message Broker
    participant Notification
 
    Patient->>Reception: POST /appointments (slotId, doctorId)
    Reception->>Billing: POST /payments (sync)
    Billing-->>Reception: { paymentId, status: "confirmed", amount }
    Reception->>Reception: зберегти Appointment + Payment Snapshot
    Reception->>Broker: publish AppointmentBooked
    Broker->>Notification: consume AppointmentBooked
    Notification->>Patient: Email "Запис підтверджено"
```
 
### Сценарій 2 - Скасування запису
 
```mermaid
sequenceDiagram
    actor Patient
    participant Reception
    participant Billing
    participant Broker as Message Broker
    participant Notification
 
    Patient->>Reception: DELETE /appointments/{id}
    Reception->>Billing: POST /refunds (sync)
    Billing-->>Reception: { refundId, status: "refunded" }
    Reception->>Reception: оновити Appointment + Payment Snapshot
    Reception->>Broker: publish AppointmentCancelled
    Broker->>Notification: consume AppointmentCancelled
    Broker->>Billing: consume AppointmentCancelled (якщо потрібно фіналізувати рефанд)
    Notification->>Patient: Email "Запис скасовано, кошти повернено"
```
 
### Сценарій 3 - Завершення візиту
 
```mermaid
sequenceDiagram
    actor Doctor
    participant Reception
    participant Broker as Message Broker
    participant Billing
    participant MedRecords as Medical Records
    participant Notification
 
    Doctor->>Reception: PATCH /appointments/{id}/complete
    Reception->>Broker: publish AppointmentCompleted\n{ appointmentId, patientId, doctorId, serviceTitle, amount, date }
    Broker->>Billing: consume AppointmentCompleted
    Billing->>Billing: оновити Appointment Snapshot → сформувати Invoice
    Broker->>MedRecords: consume AppointmentCompleted
    MedRecords->>MedRecords: створити Visit Snapshot → MedicalRecord
    Broker->>Notification: consume AppointmentCompleted
    Notification->>Doctor: Email "Медична картка збережена"
    Notification->>Patient: Email "Ваш візит завершено"
```
