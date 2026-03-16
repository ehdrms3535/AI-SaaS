package com.example.saas.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PasswordResetMailService {

    private final JavaMailSender mailSender;
    private final AuthProperties authProperties;

    public void sendResetLink(String email, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(authProperties.getPasswordReset().getMailFrom())) {
            message.setFrom(authProperties.getPasswordReset().getMailFrom());
        }
        message.setTo(email);
        message.setSubject("[SaaS Reservation] 비밀번호 재설정 링크");
        message.setText("""
                안녕하세요.

                아래 링크에서 새 비밀번호를 설정해 주세요.

                %s

                링크는 30분 동안만 유효합니다.
                """.formatted(resetLink));
        mailSender.send(message);
    }
}
