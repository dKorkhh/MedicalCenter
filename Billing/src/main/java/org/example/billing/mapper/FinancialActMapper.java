package org.example.billing.mapper;

import org.example.billing.dto.FinancialActResponse;
import org.example.billing.dto.PaymentSnapshotResponse;
import org.example.billing.model.FinancialAct;
import org.example.billing.model.PaymentSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FinancialActMapper {

    FinancialActResponse toResponse(FinancialAct financialAct);

    List<FinancialActResponse> toResponseList(List<FinancialAct> financialActs);

    PaymentSnapshotResponse toSnapshotResponse(PaymentSnapshot paymentSnapshot);
}
