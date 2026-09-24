package dev.thural.quietspace.message;

import dev.thural.quietspace.chat.Chat;
import dev.thural.quietspace.chat.ChatRepository;
import dev.thural.quietspace.message.dto.MessageRequest;
import dev.thural.quietspace.message.dto.MessageResponse;
import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.photo.dto.PhotoResponse;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final PhotoService photoService;

    public Message toEntity(MessageRequest request) {
        var message = new Message();
        BeanUtils.copyProperties(request, message);
        message.setChat(findChatById(request.getChatId()));
        message.setSender(findUserById(request.getSenderId()));
        message.setRecipient(findUserById(request.getRecipientId()));
        return message;
    }

    public MessageResponse toResponse(Message message) {
        var response = new MessageResponse();
        BeanUtils.copyProperties(message, response);
        response.setChatId(message.getChat().getId());
        response.setSenderId(message.getSender().getId());
        response.setSenderName(message.getSender().getName());
        response.setRecipientId(message.getRecipient().getId());
        PhotoResponse messagePhoto = message.getPhotoId() == null ? null
                : photoService.getPhotoById(message.getPhotoId());
        response.setPhoto(messagePhoto);
        return response;
    }

    private Chat findChatById(UUID chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(EntityNotFoundException::new);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(EntityNotFoundException::new);
    }
}
