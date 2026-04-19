package org.tonyqwe.cinemaweb.controller;

import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.entity.UserWallet;
import org.tonyqwe.cinemaweb.service.UserWalletService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/user/wallet")
public class UserWalletController {

    @Resource
    private UserWalletService userWalletService;

    /**
     * 获取用户钱包余额
     */
    @GetMapping
    public ResponseResult<?> getWalletBalance() {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseResult.error(401, "用户未登录");
            }

            UserWallet wallet = userWalletService.getWalletByUsername(username);
            if (wallet == null) {
                return ResponseResult.error(404, "钱包不存在");
            }

            return ResponseResult.success(wallet);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error(500, "获取钱包余额失败: " + e.getMessage());
        }
    }

    /**
     * 扣除钱包余额
     */
    @PostMapping("/deduct")
    public ResponseResult<?> deductBalance(@RequestBody Map<String, Object> request) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseResult.error(401, "用户未登录");
            }

            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String paymentMethod = (String) request.get("paymentMethod");
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseResult.error(400, "扣除金额必须大于0");
            }

            boolean success = userWalletService.deductBalance(username, amount, paymentMethod);
            if (success) {
                return ResponseResult.success("扣除成功");
            } else {
                return ResponseResult.error(400, "余额不足");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error(500, "扣除余额失败: " + e.getMessage());
        }
    }
}
