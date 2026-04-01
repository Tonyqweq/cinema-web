package org.tonyqwe.cinemaweb.service.impl;

import jakarta.annotation.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.service.EmailService;

@Service
public class EmailServiceImpl implements EmailService {

    @Resource
    private JavaMailSender mailSender;

    @Override
    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("2419231380@qq.com");
        message.setTo(email);
        message.setSubject("影院管理系统 - 验证码");
        message.setText("您的验证码是：" + code + "，有效期1分钟，请及时使用。");
        mailSender.send(message);
    }
}
