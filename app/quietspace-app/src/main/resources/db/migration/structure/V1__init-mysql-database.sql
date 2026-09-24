CREATE DATABASE IF NOT EXISTS quietspace;

CREATE TABLE `chat` (
  `id` varchar(36) NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user` (
  `account_locked` bit(1) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `status_type` tinyint DEFAULT NULL,
  `version` int NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `date_of_birth` datetime(6) DEFAULT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `photo_id` binary(16) DEFAULT NULL,
  `email` varchar(32) NOT NULL,
  `username` varchar(32) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `firstname` varchar(255) DEFAULT NULL,
  `lastname` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role` tinyint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ob8kqyqqgmefl0aco34akdtpe` (`email`),
  UNIQUE KEY `UK_sb8bbouer5wak8vyiiy4pf2bx` (`username`),
  CONSTRAINT `user_chk_1` CHECK ((`status_type` between 0 and 1)),
  CONSTRAINT `user_chk_2` CHECK ((`role` between 0 and 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `post` (
  `id` varchar(36) NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `text` varchar(999) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `user_id` varchar(36) NOT NULL,
  `photo_id` binary(16) DEFAULT NULL,
  `repost_id` varchar(255) DEFAULT NULL,
  `repost_text` varchar(255) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK72mt33dhhs48hf9gcqrq4fxte` (`user_id`),
  CONSTRAINT `FK72mt33dhhs48hf9gcqrq4fxte` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `comment` (
  `id` varchar(36) NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `parent_id` binary(16) DEFAULT NULL,
  `text` varchar(999) NOT NULL,
  `post_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8kcum44fvpupyw6f5baccx25c` (`user_id`),
  KEY `FKs1slvnkuemjsq2kj4h3vhx7i1` (`post_id`),
  CONSTRAINT `FK8kcum44fvpupyw6f5baccx25c` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKs1slvnkuemjsq2kj4h3vhx7i1` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `message` (
  `id` varchar(36) NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `is_seen` bit NOT NULL,
  `text` varchar(999) DEFAULT NULL,
  `chat_id` varchar(36) DEFAULT NULL,
  `recipient_id` varchar(36) NOT NULL,
  `sender_id` varchar(36) NOT NULL,
  `photo_id` binary(16) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKcnj2qaf5yc36v2f90jw2ipl9b` (`sender_id`),
  KEY `FKiup8wew331d92o7u3k8d918o3` (`recipient_id`),
  KEY `FKmejd0ykokrbuekwwgd5a5xt8a` (`chat_id`),
  CONSTRAINT `FKcnj2qaf5yc36v2f90jw2ipl9b` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKiup8wew331d92o7u3k8d918o3` FOREIGN KEY (`recipient_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKmejd0ykokrbuekwwgd5a5xt8a` FOREIGN KEY (`chat_id`) REFERENCES `chat` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notification` (
  `id` varchar(36) NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `actor_id` binary(16) NOT NULL,
  `content_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `is_seen` bit NOT NULL,
  `content_type` enum('COMMENT','MESSAGE','POST','USER') DEFAULT NULL,
  `notification_type` enum('COMMENT','COMMENT_REACTION','COMMENT_REPLY','FOLLOW_REQUEST','MENTION','POST_REACTION','REPOST') DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `photo` (
  `id` varchar(36) NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `entity_type` tinyint DEFAULT NULL,
  `entity_id` binary(16) DEFAULT NULL,
  `user_id` binary(16) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `data` longblob,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `poll` (
  `id` varchar(36) NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `due_date` datetime(6) DEFAULT NULL,
  `post_id` varchar(36) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_p245s3319v7hiqj4ca6inlb9f` (`post_id`),
  CONSTRAINT `FKj59dhk7k0402xqckwgwwp46tg` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `poll_option` (
  `id` varchar(36) NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `label` varchar(999) NOT NULL,
  `poll_id` varchar(36) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK81hniv4vvdii51krao5f84hen` (`poll_id`),
  CONSTRAINT `FK81hniv4vvdii51krao5f84hen` FOREIGN KEY (`poll_id`) REFERENCES `poll` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `poll_option_votes` (
  `poll_option_id` varchar(36) NOT NULL,
  `votes` binary(16) DEFAULT NULL,
  UNIQUE KEY `UK1ndfx2qol0cyfe4msf1rb7im` (`poll_option_id`, `votes`),
  KEY `FK6t2onukcbcqwf6o9trdwfqkwk` (`poll_option_id`),
  CONSTRAINT `FK6t2onukcbcqwf6o9trdwfqkwk` FOREIGN KEY (`poll_option_id`) REFERENCES `poll_option` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `profile_settings` (
  `id` varchar(36) NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `blocked_users` json DEFAULT NULL,
  `is_allow_public_comments` bit DEFAULT NULL,
  `is_allow_public_group_chat_invite` bit DEFAULT NULL,
  `is_allow_public_message_requests` bit DEFAULT NULL,
  `is_hide_like_counts` bit DEFAULT NULL,
  `is_notifications_muted` bit DEFAULT NULL,
  `is_private_account` bit DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_profile_settings_user` (`user_id`),
  CONSTRAINT `FK_profile_settings_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reaction` (
  `id` varchar(36) NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `content_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `username` varchar(255) NOT NULL,
  `content_type` enum('COMMENT','MESSAGE','POST','USER') DEFAULT NULL,
  `reaction_type` enum('DISLIKE','LIKE') DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `token` (
  `id` varchar(36) NOT NULL,
  `create_date` datetime(6) NOT NULL,
  `update_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `expire_date` datetime(6) DEFAULT NULL,
  `validate_date` datetime(6) DEFAULT NULL,
  `user_id` varchar(36) NOT NULL,
  `email` varchar(255) NOT NULL,
  `token` varchar(600) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_pddrhgwxnms2aceeku9s2ewy5` (`token`),
  KEY `FKe32ek7ixanakfqsdaokm4q9y2` (`user_id`),
  CONSTRAINT `FKe32ek7ixanakfqsdaokm4q9y2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_chat` (
  `chat_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  KEY `FK_user_chat_chat` (`chat_id`),
  KEY `FK_user_chat_user` (`user_id`),
  CONSTRAINT `FK_user_chat_chat` FOREIGN KEY (`chat_id`) REFERENCES `chat` (`id`),
  CONSTRAINT `FK_user_chat_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_followings` (
  `user_id` varchar(36) NOT NULL,
  `followings_id` varchar(36) NOT NULL,
  KEY `FK_user_followings_user` (`user_id`),
  KEY `FK_user_followings_following` (`followings_id`),
  CONSTRAINT `FK_user_followings_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_user_followings_following` FOREIGN KEY (`followings_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_saved_posts` (
  `post_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  KEY `FK_user_saved_posts_post` (`post_id`),
  KEY `FK_user_saved_posts_user` (`user_id`),
  CONSTRAINT `FK_user_saved_posts_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`),
  CONSTRAINT `FK_user_saved_posts_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
