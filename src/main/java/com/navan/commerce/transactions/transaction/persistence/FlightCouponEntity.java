package com.navan.commerce.transactions.transaction.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flight_coupons")
@Getter
@Setter(AccessLevel.PROTECTED)
public class FlightCouponEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private FlightDetailsEntity flightDetails;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(name = "flight_number", nullable = false, length = 64)
    private String flightNumber;

    @Column(length = 64)
    private String carrier;

    @Column(name = "from_location", nullable = false)
    private String from;

    @Column(name = "to_location", nullable = false)
    private String to;

    @Column(nullable = false)
    private Instant departure;

    @Column(nullable = false)
    private Instant arrival;

    protected FlightCouponEntity() {
    }

    FlightCouponEntity(FlightDetailsEntity flightDetails, int lineOrder, String flightNumber,
                       String carrier, String from, String to, Instant departure, Instant arrival) {
        this.id = UUID.randomUUID();
        this.flightDetails = flightDetails;
        this.lineOrder = lineOrder;
        this.flightNumber = flightNumber;
        this.carrier = carrier;
        this.from = from;
        this.to = to;
        this.departure = departure;
        this.arrival = arrival;
    }

}
