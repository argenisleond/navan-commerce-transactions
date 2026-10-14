package com.navan.commerce.transactions.transaction.mapper;

import com.navan.commerce.transactions.transaction.domain.Transaction;
import com.navan.commerce.transactions.transaction.dto.TransactionPageResponse;
import com.navan.commerce.transactions.transaction.dto.TransactionResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionResponseMapper {

    public TransactionPageResponse toPage(List<Transaction> transactions, int limit,
                                          long offset, boolean hasMore) {
        var items = transactions.stream().map(this::toResponse).toList();
        return new TransactionPageResponse(items, limit, offset, hasMore);
    }

    public TransactionResponse toResponse(Transaction transaction) {
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
                metadata(transaction)
        );
    }

    private TransactionResponse.Metadata metadata(Transaction transaction) {
        return switch (transaction.getType()) {
            case FLIGHT -> flightMetadata(transaction);
            case HOTEL -> hotelMetadata(transaction);
            case RAIL -> railMetadata(transaction);
            case NAVAN_FEE -> navanFeeMetadata(transaction);
        };
    }

    private TransactionResponse.FlightMetadata flightMetadata(Transaction transaction) {
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

    private TransactionResponse.HotelMetadata hotelMetadata(Transaction transaction) {
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

    private TransactionResponse.RailMetadata railMetadata(Transaction transaction) {
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

    private TransactionResponse.NavanFeeMetadata navanFeeMetadata(Transaction transaction) {
        var details = transaction.getNavanFeeDetails();
        return new TransactionResponse.NavanFeeMetadata(
                details.getFeeKind(),
                details.getDescription(),
                details.getRelatedExternalId()
        );
    }
}
