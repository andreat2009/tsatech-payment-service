package com.newproject.payment.events;

import java.time.OffsetDateTime;

public class DomainEvent {
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private OffsetDateTime timestamp;
    private Object payload;

    public DomainEvent() {
    }

    public DomainEvent(String eventType, String aggregateType, String aggregateId, OffsetDateTime timestamp, Object payload) {
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
