package dev.thural.quietspace.core.shared.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    void builder_setsAllFields() {
        var now = OffsetDateTime.now();
        var id = UUID.randomUUID();

        var entity = TestEntity.builder()
                .id(id)
                .version(1)
                .createdBy("user")
                .updatedBy("admin")
                .createDate(now)
                .updateDate(now)
                .build();

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getVersion()).isEqualTo(1);
        assertThat(entity.getCreatedBy()).isEqualTo("user");
        assertThat(entity.getUpdatedBy()).isEqualTo("admin");
        assertThat(entity.getCreateDate()).isEqualTo(now);
        assertThat(entity.getUpdateDate()).isEqualTo(now);
    }

    @Test
    void noArgsConstructor_createsEmptyEntity() {
        var entity = new TestEntity();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getVersion()).isNull();
    }

    @Test
    void allArgsConstructor_createsEntity() {
        var entity = TestEntity.builder()
                .id(UUID.randomUUID())
                .version(2)
                .createdBy("a")
                .updatedBy("b")
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        assertThat(entity.getVersion()).isEqualTo(2);
    }

    private static final class TestEntity extends BaseEntity {}
}