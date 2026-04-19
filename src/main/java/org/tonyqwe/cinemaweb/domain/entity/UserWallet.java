package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_wallets")
public class UserWallet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer userId;

    private BigDecimal balance;

    private BigDecimal alipayBalance;

    private BigDecimal wechatBalance;

    private BigDecimal creditBalance;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
