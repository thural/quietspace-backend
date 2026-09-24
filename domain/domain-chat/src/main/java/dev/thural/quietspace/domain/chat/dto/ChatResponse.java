package dev.thural.quietspace.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.thural.quietspace.message.dto.MessageResponse;
import dev.thural.quietspace.shared.model.BaseResponse;
import dev.thural.quietspace.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse extends BaseResponse {
    
    private List<UUID> userIds;
    private List<UserResponse> members;
    private MessageResponse recentMessage;

}
