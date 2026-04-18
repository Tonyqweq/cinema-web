CREATE TABLE IF NOT EXISTS `movie_reviews` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `movie_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `username` varchar(50) NOT NULL,
  `rating` int(11) NOT NULL COMMENT '评分 1-5',
  `comment` text,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_movie_id` (`movie_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_movie` (`user_id`,`movie_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
