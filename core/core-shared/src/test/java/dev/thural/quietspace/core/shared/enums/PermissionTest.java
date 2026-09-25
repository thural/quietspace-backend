package dev.thural.quietspace.core.shared.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionTest {

    @Test
    void allPermissions_haveCorrectStringValues() {
        assertThat(Permission.ADMIN_READ.toString()).isEqualTo("admin:read");
        assertThat(Permission.ADMIN_UPDATE.toString()).isEqualTo("admin:update");
        assertThat(Permission.ADMIN_CREATE.toString()).isEqualTo("admin:create");
        assertThat(Permission.ADMIN_DELETE.toString()).isEqualTo("admin:delete");
        assertThat(Permission.USER_READ.toString()).isEqualTo("user:read");
        assertThat(Permission.USER_UPDATE.toString()).isEqualTo("user:update");
        assertThat(Permission.USER_CREATE.toString()).isEqualTo("user:create");
        assertThat(Permission.USER_DELETE.toString()).isEqualTo("user:delete");
    }

    @Test
    void values_hasEightEntries() {
        assertThat(Permission.values()).hasSize(8);
    }

    @Test
    void valueOf_returnsCorrectEnum() {
        assertThat(Permission.valueOf("ADMIN_READ")).isEqualTo(Permission.ADMIN_READ);
    }
}