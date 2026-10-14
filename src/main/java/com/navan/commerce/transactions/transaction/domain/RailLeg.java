package com.navan.commerce.transactions.transaction.domain;

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
@Table(name = "rail_legs")
@Getter
@Setter(AccessLevel.PROTECTED)
public class RailLeg {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private Instant departure;

    @Column(nullable = false)
    private Instant arrival;

    @Column(name = "train_number", length = 64)
    private String trainNumber;

    @Column(name = "travel_class", length = 64)
    private String travelClass;

    protected RailLeg() {
    }

    RailLeg(Transaction transaction, int lineOrder, String origin, String destination,
                  Instant departure, Instant arrival, String trainNumber, String travelClass) {
        this.id = UUID.randomUUID();
        this.transaction = transaction;
        this.lineOrder = lineOrder;
        this.origin = origin;
        this.destination = destination;
        this.departure = departure;
        this.arrival = arrival;
        this.trainNumber = trainNumber;
        this.travelClass = travelClass;
    }

}
