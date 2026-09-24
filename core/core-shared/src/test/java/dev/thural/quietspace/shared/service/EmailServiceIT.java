package dev.thural.quietspace.shared.service;

import dev.thural.quietspace.shared.service.impl.SmtpEmailService;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceIT {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private SmtpEmailService emailService;

    @Test
    void sendHtmlEmail_shouldDeliverSuccessfully() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Hello</html>");

        Map<String, Object> variables = Map.of(
                "username", "Test User",
                "confirmationUrl", "http://localhost:3000/activate",
                "activationCode", "123456"
        );

        emailService.sendHtmlEmail("recipient@test.com", "Account Activation", "activate_account", variables);

        verify(templateEngine).process(eq("activate_account"), any(Context.class));
        verify(mailSender, timeout(3000)).send(mimeMessage);
    }
}
