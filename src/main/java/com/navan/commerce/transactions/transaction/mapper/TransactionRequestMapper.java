package com.navan.commerce.transactions.transaction.mapper;

import com.navan.commerce.transactions.transaction.domain.FlightDetails;
import com.navan.commerce.transactions.transaction.domain.HotelDetails;
import com.navan.commerce.transactions.transaction.domain.NavanFeeDetails;
import com.navan.commerce.transactions.transaction.domain.Transaction;
import com.navan.commerce.transactions.transaction.domain.TransactionType;
import com.navan.commerce.transactions.transaction.dto.CreateTransactionRequest;
import com.navan.commerce.transactions.transaction.dto.PatchTransactionRequest;
import com.navan.commerce.transactions.transaction.exception.InvalidTransactionMetadataException;
import org.springframework.stereotype.Component;

@Component
public class TransactionRequestMapper {

    public Transaction toTransaction(CreateTransactionRequest request) {
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
        return transaction;
    }

    public void update(Transaction transaction, PatchTransactionRequest request) {
        if (request.occurredAt() != null) {
            transaction.updateOccurredAt(request.occurredAt());
        }
        if (request.total() != null) {
            transaction.updateTotal(request.total());
        }

        replaceCollections(transaction, request);
        replaceMetadata(transaction, request);
    }

    private void replaceCollections(Transaction transaction, PatchTransactionRequest request) {
        if (request.taxLines() != null) {
            var replacements = request.taxLines().stream()
                    .map(line -> new Transaction.TaxLineValues(
                            line.name(), line.rate(), line.amount()))
                    .toList();
            transaction.replaceTaxLines(replacements);
        }
        if (request.feeLines() != null) {
            var replacements = request.feeLines().stream()
                    .map(line -> new Transaction.FeeLineValues(line.name(), line.amount()))
                    .toList();
            transaction.replaceFeeLines(replacements);
        }
    }

    private void replaceMetadata(Transaction transaction, PatchTransactionRequest request) {
        if (request.metadata() == null) {
            return;
        }

        switch (transaction.getType()) {
            case FLIGHT -> {
                var metadata = metadataAs(request.metadata(), transaction.getType(),
                        CreateTransactionRequest.FlightMetadata.class);
                var coupons = metadata.coupons().stream()
                        .map(coupon -> new FlightDetails.CouponValues(
                                coupon.flightNumber(), coupon.carrier(), coupon.from(), coupon.to(),
                                coupon.departure(), coupon.arrival()))
                        .toList();
                transaction.getFlightDetails().replace(
                        metadata.origin(), metadata.destination(), metadata.fare(), coupons);
            }
            case HOTEL -> {
                var metadata = metadataAs(request.metadata(), transaction.getType(),
                        CreateTransactionRequest.HotelMetadata.class);
                var address = metadata.address();
                var extras = metadata.extras().stream()
                        .map(extra -> new HotelDetails.ExtraValues(extra.name(), extra.amount()))
                        .toList();
                transaction.getHotelDetails().replace(
                        metadata.name(), address.street(), address.city(), address.country(),
                        metadata.checkIn(), metadata.checkOut(), metadata.nights(),
                        metadata.roomRatePerNight(), extras);
            }
            case RAIL -> {
                var metadata = metadataAs(request.metadata(), transaction.getType(),
                        CreateTransactionRequest.RailMetadata.class);
                var legs = metadata.legs().stream()
                        .map(leg -> new Transaction.RailLegValues(
                                leg.origin(), leg.destination(), leg.departure(), leg.arrival(),
                                leg.trainNumber(), leg.travelClass()))
                        .toList();
                transaction.replaceRailLegs(legs);
            }
            case NAVAN_FEE -> {
                var metadata = metadataAs(request.metadata(), transaction.getType(),
                        CreateTransactionRequest.NavanFeeMetadata.class);
                transaction.getNavanFeeDetails().update(
                        metadata.feeKind(), metadata.description(), metadata.relatedExternalId());
            }
        }
    }

    private void addMetadata(Transaction transaction, CreateTransactionRequest request) {
        switch (request.type()) {
            case FLIGHT -> addFlightMetadata(transaction,
                    metadataAs(request.metadata(), request.type(),
                            CreateTransactionRequest.FlightMetadata.class));
            case HOTEL -> addHotelMetadata(transaction,
                    metadataAs(request.metadata(), request.type(),
                            CreateTransactionRequest.HotelMetadata.class));
            case RAIL -> addRailMetadata(transaction,
                    metadataAs(request.metadata(), request.type(),
                            CreateTransactionRequest.RailMetadata.class));
            case NAVAN_FEE -> addNavanFeeMetadata(transaction,
                    metadataAs(request.metadata(), request.type(),
                            CreateTransactionRequest.NavanFeeMetadata.class));
        }
    }

    private void addFlightMetadata(Transaction transaction,
                                   CreateTransactionRequest.FlightMetadata metadata) {
        var details = new FlightDetails(
                metadata.origin(), metadata.destination(), metadata.fare());
        metadata.coupons().forEach(coupon -> details.addCoupon(
                coupon.flightNumber(), coupon.carrier(), coupon.from(), coupon.to(),
                coupon.departure(), coupon.arrival()));
        transaction.setFlightDetails(details);
    }

    private void addHotelMetadata(Transaction transaction,
                                  CreateTransactionRequest.HotelMetadata metadata) {
        var address = metadata.address();
        var details = new HotelDetails(
                metadata.name(), address.street(), address.city(), address.country(),
                metadata.checkIn(), metadata.checkOut(), metadata.nights(),
                metadata.roomRatePerNight());
        metadata.extras().forEach(extra -> details.addExtra(extra.name(), extra.amount()));
        transaction.setHotelDetails(details);
    }

    private void addRailMetadata(Transaction transaction,
                                 CreateTransactionRequest.RailMetadata metadata) {
        metadata.legs().forEach(leg -> transaction.addRailLeg(
                leg.origin(), leg.destination(), leg.departure(), leg.arrival(),
                leg.trainNumber(), leg.travelClass()));
    }

    private void addNavanFeeMetadata(Transaction transaction,
                                     CreateTransactionRequest.NavanFeeMetadata metadata) {
        transaction.setNavanFeeDetails(new NavanFeeDetails(
                metadata.feeKind(), metadata.description(), metadata.relatedExternalId()));
    }

    private <T extends CreateTransactionRequest.Metadata> T metadataAs(
            CreateTransactionRequest.Metadata metadata, TransactionType transactionType,
            Class<T> expectedType) {
        if (!expectedType.isInstance(metadata)) {
            throw new InvalidTransactionMetadataException(
                    "Metadata does not match transaction type " + transactionType);
        }
        return expectedType.cast(metadata);
    }
}
