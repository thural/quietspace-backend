-- Feed-visibility read model for domain-post (see ADR 005).
-- Denormalized from user privacy settings + follow graph via domain events
-- (UserPrivacyChangedEvent, UserFollowedEvent, UserUnfollowedEvent, UserRegisteredEvent).
-- Feed queries join these local tables instead of the user aggregate, keeping
-- pagination dense. Eventual consistency window (~outbox poll latency) is accepted
-- by product sign-off: a privacy flip can take effect with a short delay.

CREATE TABLE `post_author_visibility` (
  `author_id` varchar(36) NOT NULL,
  `is_private` bit NOT NULL DEFAULT 0,
  PRIMARY KEY (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `viewer_author_access` (
  `viewer_id` varchar(36) NOT NULL,
  `author_id` varchar(36) NOT NULL,
  PRIMARY KEY (`viewer_id`, `author_id`),
  KEY `idx_viewer_author_access_author` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Backfill: current privacy state (users without a settings row default to public).
INSERT INTO `post_author_visibility` (`author_id`, `is_private`)
SELECT `u`.`id`, COALESCE(`ps`.`is_private_account`, 0)
FROM `user` `u`
LEFT JOIN `profile_settings` `ps` ON `ps`.`user_id` = `u`.`id`
ON DUPLICATE KEY UPDATE `is_private` = VALUES(`is_private`);

-- Backfill: current follow edges (user_followings.user_id = follower, followings_id = followee).
INSERT INTO `viewer_author_access` (`viewer_id`, `author_id`)
SELECT `user_id`, `followings_id` FROM `user_followings`
ON DUPLICATE KEY UPDATE `viewer_id` = VALUES(`viewer_id`);
