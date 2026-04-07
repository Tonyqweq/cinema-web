-- 电影与影院的多对多关系表
CREATE TABLE IF NOT EXISTS `cinema_movie_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cinema_id` BIGINT NOT NULL COMMENT '影院ID',
  `movie_id` BIGINT NOT NULL COMMENT '电影ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cinema_movie` (`cinema_id`, `movie_id`),
  KEY `idx_cinema_id` (`cinema_id`),
  KEY `idx_movie_id` (`movie_id`),
  CONSTRAINT `fk_cinema_movie_cinema` FOREIGN KEY (`cinema_id`) REFERENCES `cinemas` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_cinema_movie_movie` FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影与影院的多对多关系表';
