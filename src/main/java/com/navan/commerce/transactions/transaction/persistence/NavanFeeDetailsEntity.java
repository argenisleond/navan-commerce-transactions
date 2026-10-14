package com.navan.commerce.transactions.transaction.persistence;

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
public class NavanFeeDetailsEntity {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "transaction_id")
    private TransactionEntity transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_kind", nullable = false, length = 32)
    private NavanFeeKind feeKind;

    @Column(length = 1000)
    private String description;

    @Column(name = "related_external_id")
    private String relatedExternalId;

    protected NavanFeeDetailsEntity() {
    }

    public NavanFeeDetailsEntity(NavanFeeKind feeKind, String description,
                                 String relatedExternalId) {
        this.feeKind = feeKind;
        this.description = description;
        this.relatedExternalId = relatedExternalId;
    }

    void attachTo(TransactionEntity transaction) {
        this.transaction = transaction;
    }

}
