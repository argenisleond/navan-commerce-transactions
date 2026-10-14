package com.navan.commerce.transactions.transaction.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "flight_details")
@Getter
@Setter(AccessLevel.PROTECTED)
public class FlightDetailsEntity {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "transaction_id")
    private TransactionEntity transaction;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fare;

    @OneToMany(mappedBy = "flightDetails", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<FlightCouponEntity> coupons = new ArrayList<>();

    protected FlightDetailsEntity() {
    }

    public FlightDetailsEntity(String origin, String destination, BigDecimal fare) {
        this.origin = origin;
        this.destination = destination;
        this.fare = fare;
    }

    void attachTo(TransactionEntity transaction) {
        this.transaction = transaction;
    }

    public void addCoupon(String flightNumber, String carrier, String from, String to,
                          Instant departure, Instant arrival) {
        coupons.add(new FlightCouponEntity(this, coupons.size(), flightNumber, carrier,
                from, to, departure, arrival));
    }

    public List<FlightCouponEntity> getCoupons() {
        return List.copyOf(coupons);
    }
}
