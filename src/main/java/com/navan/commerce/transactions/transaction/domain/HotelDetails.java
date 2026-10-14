package com.navan.commerce.transactions.transaction.domain;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hotel_details")
@Getter
@Setter(AccessLevel.PROTECTED)
public class HotelDetails {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false)
    private String name;

    @Column(name = "address_street", nullable = false)
    private String addressStreet;

    @Column(name = "address_city", nullable = false)
    private String addressCity;

    @Column(name = "address_country", nullable = false)
    private String addressCountry;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private int nights;

    @Column(name = "room_rate_per_night", nullable = false, precision = 19, scale = 2)
    private BigDecimal roomRatePerNight;

    @OneToMany(mappedBy = "hotelDetails", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<HotelExtra> extras = new ArrayList<>();

    protected HotelDetails() {
    }

    public HotelDetails(String name, String addressStreet, String addressCity,
                              String addressCountry, LocalDate checkIn, LocalDate checkOut,
                              int nights, BigDecimal roomRatePerNight) {
        this.name = name;
        this.addressStreet = addressStreet;
        this.addressCity = addressCity;
        this.addressCountry = addressCountry;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.roomRatePerNight = roomRatePerNight;
    }

    void attachTo(Transaction transaction) {
        this.transaction = transaction;
    }

    public void addExtra(String name, BigDecimal amount) {
        extras.add(new HotelExtra(this, extras.size(), name, amount));
    }

    public List<HotelExtra> getExtras() {
        return List.copyOf(extras);
    }
}
