package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.service.FileUploadService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

@RestController
@RequestMapping("/api/user")
public class UserAvatarController {

    @Resource
    private FileUploadService fileUploadService;

    @Resource
    private UserService userService;

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public ResponseResult<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseResult.error("用户未登录");
            }
            String username = authentication.getName();
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseResult.error("用户不存在");
            }

            // 上传文件到Minio
            String avatarUrl = fileUploadService.uploadAvatar(file);

            // 更新用户头像
            user.setAvatar(avatarUrl);
            userService.updateById(user);

            return ResponseResult.success(avatarUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户信息（包含头像）
     */
    @GetMapping("/info")
    public ResponseResult<?> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseResult.error("用户未登录");
        }
        String username = authentication.getName();
        SysUsers user = userService.getByUsername(username);
        if (user == null) {
            return ResponseResult.error("用户不存在");
        }

        return ResponseResult.success(user);
    }
}