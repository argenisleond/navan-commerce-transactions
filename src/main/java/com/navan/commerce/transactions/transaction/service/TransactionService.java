package com.navan.commerce.transactions.transaction.service;

import com.navan.commerce.transactions.transaction.domain.FlightDetails;
import com.navan.commerce.transactions.transaction.domain.HotelDetails;
import com.navan.commerce.transactions.transaction.domain.NavanFeeDetails;
import com.navan.commerce.transactions.transaction.domain.Transaction;
import com.navan.commerce.transactions.transaction.domain.TransactionRepository;
import com.navan.commerce.transactions.transaction.dto.CreateTransactionRequest;
import com.navan.commerce.transactions.transaction.dto.TransactionResponse;
import com.navan.commerce.transactions.transaction.mapper.TransactionResponseMapper;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final String EXTERNAL_ID_CONSTRAINT = "UK_TRANSACTIONS_EXTERNAL_ID";

    private final TransactionRepository repository;
    private final TransactionResponseMapper responseMapper;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        var transaction = new Transaction(
                request.externalId(),
                request.type(),
                request.currency(),
                request.occurredAt(),
                request.total()
        );

        request.taxLines().forEach(line ->
                transaction.addTaxLine(line.name(), line.rate(), line.amount()));
        request.feeLines().forEach(line ->
                transaction.addFeeLine(line.name(), line.amount()));

        addMetadata(transaction, request);
        try {
            repository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException exception) {
            if (isExternalIdConflict(exception)) {
                throw new DuplicateTransactionException(request.externalId(), exception);
            }
            throw exception;
        }

        return responseMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findIdByExternalId(String externalId) {
        return repository.findByExternalId(externalId).map(Transaction::getId);
    }

    private boolean isExternalIdConflict(Throwable exception) {
        var cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                var constraintName = constraintViolation.getConstraintName();
                return constraintName != null
                        && constraintName.toUpperCase(Locale.ROOT)
                        .contains(EXTERNAL_ID_CONSTRAINT);
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void addMetadata(Transaction transaction, CreateTransactionRequest request) {
        switch (request.type()) {
            case FLIGHT -> addFlightMetadata(transaction,
                    metadataAs(request, CreateTransactionRequest.FlightMetadata.class));
            case HOTEL -> addHotelMetadata(transaction,
                    metadataAs(request, CreateTransactionRequest.HotelMetadata.class));
            case RAIL -> addRailMetadata(transaction,
                    metadataAs(request, CreateTransactionRequest.RailMetadata.class));
            case NAVAN_FEE -> addNavanFeeMetadata(transaction,
                    metadataAs(request, CreateTransactionRequest.NavanFeeMetadata.class));
        }
    }

    private void addFlightMetadata(Transaction transaction,
                                   CreateTransactionRequest.FlightMetadata metadata) {
        var details = new FlightDetails(
                metadata.origin(), metadata.destination(), metadata.fare());
        metadata.coupons().forEach(coupon -> details.addCoupon(
                coupon.flightNumber(),
                coupon.carrier(),
                coupon.from(),
                coupon.to(),
                coupon.departure(),
                coupon.arrival()
        ));
        transaction.setFlightDetails(details);
    }

    private void addHotelMetadata(Transaction transaction,
                                  CreateTransactionRequest.HotelMetadata metadata) {
        var address = metadata.address();
        var details = new HotelDetails(
                metadata.name(),
                address.street(),
                address.city(),
                address.country(),
                metadata.checkIn(),
                metadata.checkOut(),
                metadata.nights(),
                metadata.roomRatePerNight()
        );
        metadata.extras().forEach(extra -> details.addExtra(extra.name(), extra.amount()));
        transaction.setHotelDetails(details);
    }

    private void addRailMetadata(Transaction transaction,
                                 CreateTransactionRequest.RailMetadata metadata) {
        metadata.legs().forEach(leg -> transaction.addRailLeg(
                leg.origin(),
                leg.destination(),
                leg.departure(),
                leg.arrival(),
                leg.trainNumber(),
                leg.travelClass()
        ));
    }

    private void addNavanFeeMetadata(Transaction transaction,
                                     CreateTransactionRequest.NavanFeeMetadata metadata) {
        transaction.setNavanFeeDetails(new NavanFeeDetails(
                metadata.feeKind(), metadata.description(), metadata.relatedExternalId()));
    }

    private <T extends CreateTransactionRequest.Metadata> T metadataAs(
            CreateTransactionRequest request, Class<T> expectedType) {
        if (!expectedType.isInstance(request.metadata())) {
            throw new InvalidTransactionMetadataException(
                    "Metadata does not match transaction type " + request.type());
        }
        return expectedType.cast(request.metadata());
    }

}
