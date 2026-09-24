CREATE TABLE outbox_events (
    id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload VARCHAR(10000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (published_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id);