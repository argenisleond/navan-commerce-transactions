package com.navan.commerce.transactions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "navan_fee_details")
@Getter
@Setter(AccessLevel.PROTECTED)
public class NavanFeeDetails {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_kind", nullable = false, length = 32)
    private NavanFeeKind feeKind;

    @Column(length = 1000)
    private String description;

    @Column(name = "related_external_id")
    private String relatedExternalId;

    protected NavanFeeDetails() {
    }

    public NavanFeeDetails(NavanFeeKind feeKind, String description,
                                 String relatedExternalId) {
        this.feeKind = feeKind;
        this.description = description;
        this.relatedExternalId = relatedExternalId;
    }

    void attachTo(Transaction transaction) {
        this.transaction = transaction;
    }

    public void update(NavanFeeKind feeKind, String description, String relatedExternalId) {
        this.feeKind = feeKind;
        this.description = description;
        this.relatedExternalId = relatedExternalId;
    }

}
