package com.navan.commerce.transactions.transaction.service;

import com.navan.commerce.transactions.transaction.domain.FlightDetails;
import com.navan.commerce.transactions.transaction.domain.HotelDetails;
import com.navan.commerce.transactions.transaction.domain.NavanFeeDetails;
import com.navan.commerce.transactions.transaction.domain.Transaction;
import com.navan.commerce.transactions.transaction.domain.TransactionRepository;
import com.navan.commerce.transactions.transaction.dto.CreateTransactionRequest;
import com.navan.commerce.transactions.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;

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
        repository.saveAndFlush(transaction);

        return toResponse(transaction);
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

    private TransactionResponse toResponse(Transaction transaction) {
        var taxLines = transaction.getTaxLines().stream()
                .map(line -> new TransactionResponse.TaxLine(
                        line.getName(), line.getRate(), line.getAmount()))
                .toList();
        var feeLines = transaction.getFeeLines().stream()
                .map(line -> new TransactionResponse.FeeLine(line.getName(), line.getAmount()))
                .toList();

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getExternalId(),
                transaction.getCurrency(),
                transaction.getOccurredAt(),
                transaction.getTotal(),
                taxLines,
                feeLines,
                metadataView(transaction)
        );
    }

    private TransactionResponse.Metadata metadataView(Transaction transaction) {
        return switch (transaction.getType()) {
            case FLIGHT -> flightView(transaction);
            case HOTEL -> hotelView(transaction);
            case RAIL -> railView(transaction);
            case NAVAN_FEE -> navanFeeView(transaction);
        };
    }

    private TransactionResponse.FlightMetadata flightView(Transaction transaction) {
        var details = transaction.getFlightDetails();
        var coupons = details.getCoupons().stream()
                .map(coupon -> new TransactionResponse.FlightCoupon(
                        coupon.getFlightNumber(),
                        coupon.getCarrier(),
                        coupon.getFrom(),
                        coupon.getTo(),
                        coupon.getDeparture(),
                        coupon.getArrival()
                ))
                .toList();
        return new TransactionResponse.FlightMetadata(
                details.getOrigin(), details.getDestination(), details.getFare(), coupons);
    }

    private TransactionResponse.HotelMetadata hotelView(Transaction transaction) {
        var details = transaction.getHotelDetails();
        var address = new TransactionResponse.Address(
                details.getAddressStreet(),
                details.getAddressCity(),
                details.getAddressCountry()
        );
        var extras = details.getExtras().stream()
                .map(extra -> new TransactionResponse.HotelExtra(extra.getName(), extra.getAmount()))
                .toList();
        return new TransactionResponse.HotelMetadata(
                details.getName(),
                address,
                details.getCheckIn(),
                details.getCheckOut(),
                details.getNights(),
                details.getRoomRatePerNight(),
                extras
        );
    }

    private TransactionResponse.RailMetadata railView(Transaction transaction) {
        var legs = transaction.getRailLegs().stream()
                .map(leg -> new TransactionResponse.RailLeg(
                        leg.getOrigin(),
                        leg.getDestination(),
                        leg.getDeparture(),
                        leg.getArrival(),
                        leg.getTrainNumber(),
                        leg.getTravelClass()
                ))
                .toList();
        return new TransactionResponse.RailMetadata(legs);
    }

    private TransactionResponse.NavanFeeMetadata navanFeeView(Transaction transaction) {
        var details = transaction.getNavanFeeDetails();
        return new TransactionResponse.NavanFeeMetadata(
                details.getFeeKind(),
                details.getDescription(),
                details.getRelatedExternalId()
        );
    }
}
