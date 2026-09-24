package dev.thural.quietspace.config;

import org.jspecify.annotations.NonNull;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

@Component
public class OffsetDateTimeProvider implements DateTimeProvider {

    @Override
    @NonNull
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(OffsetDateTime.now());
    }
}
