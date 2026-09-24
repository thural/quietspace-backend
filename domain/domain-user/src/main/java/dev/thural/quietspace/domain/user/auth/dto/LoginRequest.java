package dev.thural.quietspace.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginRequest {

    @Email
    @NotNull
    @NotBlank
    @Size(min = 1, max = 256)
    private String email;

    @NotNull
    @NotBlank
    @Size(min = 8, max = 32)
    private String password;

}
