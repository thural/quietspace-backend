package dev.thural.quietspace.websocket.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.thural.quietspace.shared.enums.StatusType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRepresentation {

    private String username;
    private String firstname;
    private String lastname;
    private StatusType statusType;

    @Email(message = "invalid email format")
    @NotEmpty(message = "email can not be empty")
    @NotNull(message = "email is required")
    @Size(min = 1, max = 32)
    private String email;

}
