package org.tonyqwe.cinemaweb.service;

public interface EmailService {

    void sendVerificationCode(String email, String code);
}
