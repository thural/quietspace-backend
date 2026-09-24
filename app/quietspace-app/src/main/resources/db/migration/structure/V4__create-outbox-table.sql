CREATE TABLE `outbox_events` (
    `id` BINARY(16) NOT NULL,
    `aggregate_type` VARCHAR(255) NOT NULL,
    `aggregate_id` BINARY(16) NOT NULL,
    `event_type` VARCHAR(255) NOT NULL,
    `payload` JSON NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `published_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_outbox_unpublished` (`published_at`),
    INDEX `idx_outbox_aggregate` (`aggregate_type`, `aggregate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;