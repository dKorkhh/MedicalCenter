package org.example.reception.dto;

import org.example.reception.client.dto.InvoiceDto;

public record AppointmentDetailsResponse(AppointmentResponse appointment, InvoiceDto invoice) {
}
