package com.navan.commerce.transactions.transaction.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
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
@Table(name = "transactions")
@Getter
@Setter(AccessLevel.PROTECTED)
public class Transaction {

    @Id
    private UUID id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionType type;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<TransactionTaxLine> taxLines = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<TransactionFeeLine> feeLines = new ArrayList<>();

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private FlightDetails flightDetails;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private HotelDetails hotelDetails;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<RailLeg> railLegs = new ArrayList<>();

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private NavanFeeDetails navanFeeDetails;

    protected Transaction() {
    }

    public Transaction(String externalId, TransactionType type, String currency,
                       Instant occurredAt, BigDecimal total) {
        this.id = UUID.randomUUID();
        this.externalId = externalId;
        this.type = type;
        this.currency = currency;
        this.occurredAt = occurredAt;
        this.total = total;
    }

    public void addTaxLine(String name, BigDecimal rate, BigDecimal amount) {
        taxLines.add(new TransactionTaxLine(this, taxLines.size(), name, rate, amount));
    }

    public void replaceTaxLines(List<TaxLineValues> replacements) {
        var retained = Math.min(taxLines.size(), replacements.size());
        for (var index = 0; index < retained; index++) {
            var line = replacements.get(index);
            taxLines.get(index).update(line.name(), line.rate(), line.amount());
        }
        while (taxLines.size() > replacements.size()) {
            taxLines.removeLast();
        }
        for (var index = retained; index < replacements.size(); index++) {
            var line = replacements.get(index);
            addTaxLine(line.name(), line.rate(), line.amount());
        }
    }

    public void addFeeLine(String name, BigDecimal amount) {
        feeLines.add(new TransactionFeeLine(this, feeLines.size(), name, amount));
    }

    public void replaceFeeLines(List<FeeLineValues> replacements) {
        var retained = Math.min(feeLines.size(), replacements.size());
        for (var index = 0; index < retained; index++) {
            var line = replacements.get(index);
            feeLines.get(index).update(line.name(), line.amount());
        }
        while (feeLines.size() > replacements.size()) {
            feeLines.removeLast();
        }
        for (var index = retained; index < replacements.size(); index++) {
            var line = replacements.get(index);
            addFeeLine(line.name(), line.amount());
        }
    }

    public void setFlightDetails(FlightDetails flightDetails) {
        this.flightDetails = flightDetails;
        flightDetails.attachTo(this);
    }

    public void setHotelDetails(HotelDetails hotelDetails) {
        this.hotelDetails = hotelDetails;
        hotelDetails.attachTo(this);
    }

    public void addRailLeg(String origin, String destination, Instant departure, Instant arrival,
                           String trainNumber, String travelClass) {
        railLegs.add(new RailLeg(this, railLegs.size(), origin, destination, departure,
                arrival, trainNumber, travelClass));
    }

    public void replaceRailLegs(List<RailLegValues> replacements) {
        var retained = Math.min(railLegs.size(), replacements.size());
        for (var index = 0; index < retained; index++) {
            var leg = replacements.get(index);
            railLegs.get(index).update(
                    leg.origin(), leg.destination(), leg.departure(), leg.arrival(),
                    leg.trainNumber(), leg.travelClass());
        }
        while (railLegs.size() > replacements.size()) {
            railLegs.removeLast();
        }
        for (var index = retained; index < replacements.size(); index++) {
            var leg = replacements.get(index);
            addRailLeg(leg.origin(), leg.destination(), leg.departure(), leg.arrival(),
                    leg.trainNumber(), leg.travelClass());
        }
    }

    public void updateOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public void updateTotal(BigDecimal total) {
        this.total = total;
    }

    public void setNavanFeeDetails(NavanFeeDetails navanFeeDetails) {
        this.navanFeeDetails = navanFeeDetails;
        navanFeeDetails.attachTo(this);
    }

    public List<TransactionTaxLine> getTaxLines() {
        return List.copyOf(taxLines);
    }

    public List<TransactionFeeLine> getFeeLines() {
        return List.copyOf(feeLines);
    }

    public List<RailLeg> getRailLegs() {
        return List.copyOf(railLegs);
    }

    public record TaxLineValues(String name, BigDecimal rate, BigDecimal amount) {
    }

    public record FeeLineValues(String name, BigDecimal amount) {
    }

    public record RailLegValues(
            String origin,
            String destination,
            Instant departure,
            Instant arrival,
            String trainNumber,
            String travelClass
    ) {
    }
}
