package dev.thural.quietspace.shared.service.impl;

import dev.thural.quietspace.shared.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Profile({"staging", "prod"})
@Service
@Slf4j
public class ResendEmailService implements EmailService {

    private final RestClient restClient;
    private final SpringTemplateEngine templateEngine;

    public ResendEmailService(
            @Value("${email.provider.url}") String baseUrl,
            @Value("${email.provider.key}") String apiKey,
            SpringTemplateEngine templateEngine
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlBody = templateEngine.process(templateName, context);

            Map<String, Object> body = Map.of(
                    "from", "contact@quietspace.live",
                    "to", new String[]{to},
                    "subject", subject,
                    "html", htmlBody
            );

            restClient.post()
                    .uri("/emails")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("sent email via Resend to {}", to);
        } catch (Exception e) {
            log.error("failed to send email via Resend to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email via Resend", e);
        }
    }
}
