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
            userWalletMapper.insert(wallet);
        }
        
        return wallet;
    }

    @Override
    public boolean deductBalance(String username, BigDecimal amount) {
        // 先获取钱包
        UserWallet wallet = getWalletByUsername(username);
        if (wallet == null) {
            return false;
        }

        // 检查余额是否足够
        if (wallet.getBalance().compareTo(amount) < 0) {
            return false;
        }

        // 扣除余额
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        LambdaUpdateWrapper<UserWallet> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserWallet::getUserId, wallet.getUserId())
                .set(UserWallet::getBalance, newBalance);

        return userWalletMapper.update(null, updateWrapper) > 0;
    }
}
