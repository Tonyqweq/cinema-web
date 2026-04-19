package org.tonyqwe.cinemaweb.service;

import org.tonyqwe.cinemaweb.domain.entity.UserWallet;

import java.math.BigDecimal;

public interface UserWalletService {

    /**
     * 根据用户名获取钱包
     */
    UserWallet getWalletByUsername(String username);

    /**
     * 扣除钱包余额
     */
    boolean deductBalance(String username, BigDecimal amount, String paymentMethod);
}
