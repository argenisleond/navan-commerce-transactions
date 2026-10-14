package com.navan.commerce.transactions.mapper;

import com.navan.commerce.transactions.domain.FlightDetails;
import com.navan.commerce.transactions.domain.HotelDetails;
import com.navan.commerce.transactions.domain.NavanFeeDetails;
import com.navan.commerce.transactions.domain.NavanFeeKind;
import com.navan.commerce.transactions.domain.Transaction;
import com.navan.commerce.transactions.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

final class MapperFixtures {

    static final Instant OCCURRED_AT = Instant.parse("2026-03-11T06:40:00Z");

    private MapperFixtures() {
    }

    static Transaction flight(String externalId, String currency, String total) {
        var transaction = transaction(externalId, TransactionType.FLIGHT, currency, total);
        transaction.addTaxLine("Airport tax", null, new BigDecimal("15.00"));
        transaction.addFeeLine("YQ", new BigDecimal("25.00"));
        var details = new FlightDetails("TXL", "LHR", new BigDecimal("180.00"));
        details.addCoupon("BA982", "BA", "TXL", "LHR",
                Instant.parse("2026-03-11T07:00:00Z"),
                Instant.parse("2026-03-11T08:20:00Z"));
        transaction.setFlightDetails(details);
        return transaction;
    }

    static Transaction hotel(String externalId, String currency, String total) {
        var transaction = transaction(externalId, TransactionType.HOTEL, currency, total);
        transaction.addTaxLine("VAT", new BigDecimal("0.070000"), new BigDecimal("21.70"));
        var details = new HotelDetails(
                "Hotel Mitte London", "12 Kingsway", "London", "GB",
                LocalDate.parse("2026-03-11"), LocalDate.parse("2026-03-13"),
                2, new BigDecimal("140.00"));
        details.addExtra("Breakfast", new BigDecimal("30.00"));
        transaction.setHotelDetails(details);
        return transaction;
    }

    static Transaction rail(String externalId, String currency, String total) {
        var transaction = transaction(externalId, TransactionType.RAIL, currency, total);
        transaction.addFeeLine("Booking fee", new BigDecimal("4.00"));
        transaction.addRailLeg(
                "London St Pancras", "Paris Nord",
                Instant.parse("2026-03-13T09:00:00Z"),
                Instant.parse("2026-03-13T12:05:00Z"), "EST9108", "standard");
        return transaction;
    }

    static Transaction navanFee(String externalId, String currency, String total) {
        var transaction = transaction(externalId, TransactionType.NAVAN_FEE, currency, total);
        transaction.setNavanFeeDetails(new NavanFeeDetails(
                NavanFeeKind.TRIP_FEE, "Navan trip fee", "flight-reference"));
        return transaction;
    }

    private static Transaction transaction(String externalId, TransactionType type,
                                           String currency, String total) {
        return new Transaction(externalId, type, currency, OCCURRED_AT, new BigDecimal(total));
    }
}
