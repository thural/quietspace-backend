package dev.thural.quietspace.core.shared.security;

import dev.thural.quietspace.core.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Token extends BaseEntity {

    @NotBlank
    @Column(length = 600, unique = true)
    private String token;

    @Column(length = 36, unique = true)
    private String jti;

    @Email
    @NotBlank
    private String email;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID userId;

    private OffsetDateTime expireDate;
    private OffsetDateTime validateDate;

    private boolean used;
}