CREATE TABLE processed_events (
    id UUID NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_processed_events_event_id ON processed_events (event_id);
CREATE INDEX idx_processed_events_event_type ON processed_events (event_type);