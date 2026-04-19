package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.UserWallet;
import org.tonyqwe.cinemaweb.mapper.UserWalletMapper;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.service.UserWalletService;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Service
public class UserWalletServiceImpl implements UserWalletService {

    @Resource
    private UserWalletMapper userWalletMapper;

    @Resource
    private UserService userService;

    @Override
    public UserWallet getWalletByUsername(String username) {
        // 先根据用户名获取用户ID
        Integer userId = userService.getByUsername(username).getId();
        // 然后根据用户ID获取钱包
        UserWallet wallet = userWalletMapper.selectByUserId(userId);
        
        // 如果钱包不存在，创建一个新的钱包
        if (wallet == null) {
            wallet = new UserWallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setAlipayBalance(BigDecimal.ZERO);
            wallet.setWechatBalance(BigDecimal.ZERO);
            wallet.setCreditBalance(BigDecimal.ZERO);
            userWalletMapper.insert(wallet);
        }
        
        return wallet;
    }

    @Override
    public boolean deductBalance(String username, BigDecimal amount, String paymentMethod) {
        // 先获取钱包
        UserWallet wallet = getWalletByUsername(username);
        if (wallet == null) {
            return false;
        }

        // 检查对应支付方式的余额是否足够
        BigDecimal currentBalance = BigDecimal.ZERO;
        switch (paymentMethod) {
            case "alipay":
                currentBalance = wallet.getAlipayBalance();
                break;
            case "wechat":
                currentBalance = wallet.getWechatBalance();
                break;
            case "credit":
                currentBalance = wallet.getCreditBalance();
                break;
            default:
                currentBalance = wallet.getBalance();
                break;
        }

        if (currentBalance.compareTo(amount) < 0) {
            return false;
        }

        // 扣除对应支付方式的余额
        switch (paymentMethod) {
            case "alipay":
                wallet.setAlipayBalance(currentBalance.subtract(amount));
                break;
            case "wechat":
                wallet.setWechatBalance(currentBalance.subtract(amount));
                break;
            case "credit":
                wallet.setCreditBalance(currentBalance.subtract(amount));
                break;
            default:
                wallet.setBalance(currentBalance.subtract(amount));
                break;
        }

        // 更新总余额
        wallet.setBalance(
            wallet.getAlipayBalance().add(
                wallet.getWechatBalance().add(
                    wallet.getCreditBalance()
                )
            )
        );

        return userWalletMapper.updateById(wallet) > 0;
    }
}
