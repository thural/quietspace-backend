CREATE TABLE `processed_events` (
    `event_id` BINARY(16) NOT NULL,
    `event_type` VARCHAR(255) NOT NULL,
    `processed_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`event_id`),
    INDEX `idx_processed_events_event_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;