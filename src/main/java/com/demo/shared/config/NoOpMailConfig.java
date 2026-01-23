package com.demo.shared.config;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;

/**
 * Provides a no-op JavaMailSender when mail auto-configuration is not available.
 * This prevents ApplicationContext failures in tests and non-mail deployments.
 */
@Configuration
public class NoOpMailConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender noOpJavaMailSender() {
        return new JavaMailSender() {
            @Override
            public MimeMessage createMimeMessage() {
                return new MimeMessage((Session) null);
            }

            @Override
            public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
                try {
                    return new MimeMessage(null, contentStream);
                } catch (Exception ex) {
                    throw new MailException("Failed to create MimeMessage", ex) {};
                }
            }

            @Override
            public void send(MimeMessage mimeMessage) throws MailException {
                // no-op
            }

            @Override
            public void send(MimeMessage... mimeMessages) throws MailException {
                // no-op
            }

            @Override
            public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
                // no-op
            }

            @Override
            public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
                // no-op
            }

            @Override
            public void send(SimpleMailMessage simpleMessage) throws MailException {
                // no-op
            }

            @Override
            public void send(SimpleMailMessage... simpleMessages) throws MailException {
                // no-op
            }
        };
    }
}