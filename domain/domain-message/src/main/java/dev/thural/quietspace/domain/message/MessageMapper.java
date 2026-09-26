package dev.thural.quietspace.domain.message;

import dev.thural.quietspace.domain.chat.Chat;
import dev.thural.quietspace.domain.chat.ChatRepository;
import dev.thural.quietspace.domain.message.dto.MessageRequest;
import dev.thural.quietspace.domain.message.dto.MessageResponse;
import dev.thural.quietspace.domain.photo.PhotoService;
import dev.thural.quietspace.domain.photo.dto.PhotoResponse;
import dev.thural.quietspace.domain.user.api.UserQueryPort;
import dev.thural.quietspace.domain.user.api.dto.UserSummaryDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final UserQueryPort userQueryPort;
    private final ChatRepository chatRepository;
    private final PhotoService photoService;

    public Message toEntity(MessageRequest request) {
        var message = new Message();
        BeanUtils.copyProperties(request, message);
        message.setChat(findChatById(request.getChatId()));
        message.setSenderId(requireUserId(request.getSenderId()));
        message.setRecipientId(requireUserId(request.getRecipientId()));
        return message;
    }

    public MessageResponse toResponse(Message message) {
        var response = new MessageResponse();
        BeanUtils.copyProperties(message, response);
        response.setChatId(message.getChat().getId());
        response.setSenderId(message.getSenderId());
        response.setSenderName(resolveUsername(message.getSenderId()));
        response.setRecipientId(message.getRecipientId());
        PhotoResponse messagePhoto = message.getPhotoId() == null ? null
                : photoService.getPhotoById(message.getPhotoId());
        response.setPhoto(messagePhoto);
        return response;
    }

    private Chat findChatById(UUID chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(EntityNotFoundException::new);
    }

    private UUID requireUserId(UUID userId) {
        return userQueryPort.getUserSummary(userId).map(UserSummaryDTO::id)
                .orElseThrow(EntityNotFoundException::new);
    }

    private String resolveUsername(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userQueryPort.getUserSummary(userId).map(UserSummaryDTO::username).orElse(null);
    }
}
