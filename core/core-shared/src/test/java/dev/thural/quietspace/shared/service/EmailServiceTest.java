package dev.thural.quietspace.shared.service;

import dev.thural.quietspace.shared.service.impl.SmtpEmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private SmtpEmailService emailService;

    @Test
    void sendHtmlEmail_givenAllParams_shouldSendMimeMessage() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Hello TestUser</html>");

        Map<String, Object> variables = Map.of(
                "username", "TestUser",
                "confirmationUrl", "http://localhost:8080/activate",
                "activationCode", "123456"
        );

        emailService.sendHtmlEmail("recipient@example.com", "Account Activation", "activate_account", variables);

        verify(templateEngine).process(eq("activate_account"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendHtmlEmail_whenMailSenderFails_shouldPropagate() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Hello</html>");
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                emailService.sendHtmlEmail("recipient@example.com", "Subject", "template", Map.of())
        ).isInstanceOf(RuntimeException.class);
    }
}
