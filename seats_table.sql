CREATE TABLE `seats` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '座位ID（主键）',
  `hall_id` bigint(20) NOT NULL COMMENT '影厅ID（外键，关联到halls表的id）',
  `row_number` int(11) NOT NULL COMMENT '行号',
  `column_number` int(11) NOT NULL COMMENT '列号',
  `seat_number` varchar(10) NOT NULL COMMENT '座位号（如A1、A2、B1等）',
  `seat_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '座位类型：1=普通座，2=VIP座，3=情侣座，4=轮椅座',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '座位状态：1=可选，2=已售，3=已锁定，4=维修中',
  `price` decimal(10,2) DEFAULT NULL COMMENT '座位价格（可选，如果影厅统一价格则为null）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_hall_id` (`hall_id`) COMMENT '按影厅ID检索的索引',
  KEY `idx_seat_number` (`seat_number`) COMMENT '按座位号检索的索引',
  CONSTRAINT `fk_seat_hall` FOREIGN KEY (`hall_id`) REFERENCES `halls` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表（影厅座位信息）';
