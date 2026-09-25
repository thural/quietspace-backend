package dev.thural.quietspace.core.shared.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void values_containsExpectedRoles() {
        assertThat(Role.values()).containsExactlyInAnyOrder(
                Role.USER,
                Role.ADMIN
        );
    }

    @Test
    void valueOf_returnsCorrectEnum() {
        assertThat(Role.valueOf("USER")).isEqualTo(Role.USER);
    }

    @Test
    void getAuthorities_includesRoleAuthority() {
        var authorities = Role.USER.getAuthorities();
        assertThat(authorities).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }
}