package dev.thural.quietspace.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    @NotNull(message = "username can not be null")
    @NotBlank(message = "username can not be blank")
    @Size(min = 1, max = 32, message = "at least 1 and max 32 characters expected")
    private String username;

    @NotEmpty(message = "firstname is mandatory")
    @NotNull(message = "firstname is mandatory")
    private String firstname;

    @NotEmpty(message = "lastname is mandatory")
    @NotNull(message = "lastname is mandatory")
    private String lastname;

    @Email(message = "email is not in valid format")
    @NotNull(message = "email can not be null")
    @NotBlank(message = "email can not be blank")
    @Size(max = 32, message = "max 32 characters expected for email")
    private String email;

    @NotNull(message = "password can not be null")
    @NotBlank(message = "password can nto be blank")
    @Size(min = 8, max = 32, message = "at least 8 and max 32 characters expected for password")
    private String password;

    @NotNull(message = "user role can not be null")
    @NotBlank(message = "user role can not be blank")
    @Size(min = 1, max = 16, message = "1 to 16 characters are expected for user role")
    private String role;

}