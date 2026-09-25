package dev.thural.quietspace.core.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeProviderTest {

    private final OffsetDateTimeProvider provider = new OffsetDateTimeProvider();

    @Test
    void getNow_returnsPresentOptional() {
        Optional<TemporalAccessor> now = provider.getNow();

        assertThat(now).isPresent();
        assertThat(now.get()).isInstanceOf(OffsetDateTime.class);
    }

    @Test
    void getNow_returnsCurrentTime() {
        var before = OffsetDateTime.now();
        var now = provider.getNow().get();
        var after = OffsetDateTime.now();

        assertThat(((OffsetDateTime) now).isAfter(before) || ((OffsetDateTime) now).isEqual(before)).isTrue();
        assertThat(((OffsetDateTime) now).isBefore(after) || ((OffsetDateTime) now).isEqual(after)).isTrue();
    }

    @Test
    void implementsDateTimeProvider() {
        assertThat(provider).isInstanceOf(DateTimeProvider.class);
    }
}