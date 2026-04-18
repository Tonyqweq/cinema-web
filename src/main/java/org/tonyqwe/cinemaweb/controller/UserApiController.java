package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.service.MinioService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

@RestController
@RequestMapping("/api/user")
public class UserApiController {

    @Resource
    private UserService userService;

    @Resource
    private MinioService minioService;

    /**
     * 上传用户头像
     */
    @PostMapping("/avatar")
    public ResponseResult<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 上传文件到MinIO
            String avatarUrl = minioService.uploadFile(file);
            
            // 获取当前用户
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = null;
            if (principal instanceof String) {
                // 从JwtAuthenticationFilter中，principal是用户名字符串
                username = (String) principal;
            } else if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            }
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseResult.error(401, "用户未登录");
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseResult.error(404, "用户不存在");
            }
            
            // 更新用户头像
            user.setAvatar(avatarUrl);
            userService.updateById(user);
            
            return ResponseResult.success(avatarUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error(500, "头像上传失败：" + e.getMessage());
        }
    }
}