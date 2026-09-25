package dev.thural.quietspace.domain.user.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRequest {

    @NotNull
    private UUID id;

    @NotNull
    @NotBlank
    private String jwtToken;

}