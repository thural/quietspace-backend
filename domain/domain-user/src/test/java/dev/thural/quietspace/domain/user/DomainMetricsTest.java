package dev.thural.quietspace.domain.user;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DomainMetricsTest {

    @Test
    void meterRegistry_isAvailable() {
        MeterRegistry registry = new SimpleMeterRegistry();
        
        registry.counter("domain.user.test.counter").increment();
        AtomicReference<Double> gaugeValue = new AtomicReference<>(42.0);
        registry.gauge("domain.user.test.gauge", gaugeValue, AtomicReference::get);
        registry.timer("domain.user.test.timer").record(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        });
        
        assertThat(registry.get("domain.user.test.counter").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("domain.user.test.gauge").gauge().value()).isEqualTo(42.0);
        assertThat(registry.get("domain.user.test.timer").timer().count()).isEqualTo(1);
    }
}