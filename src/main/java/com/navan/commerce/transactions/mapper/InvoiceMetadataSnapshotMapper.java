package com.navan.commerce.transactions.mapper;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.navan.commerce.transactions.domain.NavanFeeKind;
import com.navan.commerce.transactions.domain.Transaction;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMetadataSnapshotMapper {

    public String toJson(Transaction transaction) {
        return (switch (transaction.getType()) {
            case FLIGHT -> flightMetadata(transaction);
            case HOTEL -> hotelMetadata(transaction);
            case RAIL -> railMetadata(transaction);
            case NAVAN_FEE -> navanFeeMetadata(transaction);
        }).toString();
    }

    private ObjectNode flightMetadata(Transaction transaction) {
        var details = transaction.getFlightDetails();
        var metadata = objectNode();
        metadata.put("origin", details.getOrigin());
        metadata.put("destination", details.getDestination());
        metadata.put("fare", details.getFare());
        var coupons = metadata.putArray("coupons");
        details.getCoupons().forEach(coupon -> {
            var node = coupons.addObject();
            node.put("flight_number", coupon.getFlightNumber());
            putNullable(node, "carrier", coupon.getCarrier());
            node.put("from", coupon.getFrom());
            node.put("to", coupon.getTo());
            node.put("departure", coupon.getDeparture().toString());
            node.put("arrival", coupon.getArrival().toString());
        });
        return metadata;
    }

    private ObjectNode hotelMetadata(Transaction transaction) {
        var details = transaction.getHotelDetails();
        var metadata = objectNode();
        metadata.put("name", details.getName());
        var address = metadata.putObject("address");
        address.put("street", details.getAddressStreet());
        address.put("city", details.getAddressCity());
        address.put("country", details.getAddressCountry());
        metadata.put("check_in", details.getCheckIn().toString());
        metadata.put("check_out", details.getCheckOut().toString());
        metadata.put("nights", details.getNights());
        metadata.put("room_rate_per_night", details.getRoomRatePerNight());
        var extras = metadata.putArray("extras");
        details.getExtras().forEach(extra -> {
            var node = extras.addObject();
            node.put("name", extra.getName());
            node.put("amount", extra.getAmount());
        });
        return metadata;
    }

    private ObjectNode railMetadata(Transaction transaction) {
        var metadata = objectNode();
        var legs = metadata.putArray("legs");
        transaction.getRailLegs().forEach(leg -> {
            var node = legs.addObject();
            node.put("origin", leg.getOrigin());
            node.put("destination", leg.getDestination());
            node.put("departure", leg.getDeparture().toString());
            node.put("arrival", leg.getArrival().toString());
            putNullable(node, "train_number", leg.getTrainNumber());
            putNullable(node, "class", leg.getTravelClass());
        });
        return metadata;
    }

    private ObjectNode navanFeeMetadata(Transaction transaction) {
        var details = transaction.getNavanFeeDetails();
        var metadata = objectNode();
        metadata.put("fee_kind", feeKind(details.getFeeKind()));
        putNullable(metadata, "description", details.getDescription());
        putNullable(metadata, "related_external_id", details.getRelatedExternalId());
        return metadata;
    }

    private ObjectNode objectNode() {
        return JsonNodeFactory.instance.objectNode();
    }

    private void putNullable(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private String feeKind(NavanFeeKind feeKind) {
        return switch (feeKind) {
            case TRIP_FEE -> "trip_fee";
            case AGENT_CALL_FEE -> "agent_call_fee";
        };
    }
}
