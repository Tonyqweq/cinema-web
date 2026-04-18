package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.tonyqwe.cinemaweb.domain.entity.UserWallet;

public interface UserWalletMapper extends BaseMapper<UserWallet> {

    /**
     * 根据用户ID获取钱包
     */
    UserWallet selectByUserId(Integer userId);
}
