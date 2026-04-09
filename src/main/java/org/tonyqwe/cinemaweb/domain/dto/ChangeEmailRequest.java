package org.tonyqwe.cinemaweb.domain.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class ChangeEmailRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "请输入正确的邮箱格式")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String verificationCode;
}
