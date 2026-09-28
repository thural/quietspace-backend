package dev.thural.quietspace.domain.user;

import dev.thural.quietspace.core.shared.util.OffsetDateTimeProvider;
import dev.thural.quietspace.domain.user.audit.ApplicationAuditAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditionConfiguration {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new ApplicationAuditAware();
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return new OffsetDateTimeProvider();
    }

}
