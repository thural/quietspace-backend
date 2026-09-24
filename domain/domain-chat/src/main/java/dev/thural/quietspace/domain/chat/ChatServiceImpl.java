package dev.thural.quietspace.domain.chat;

import dev.thural.quietspace.domain.chat.dto.ChatResponse;
import dev.thural.quietspace.domain.chat.dto.CreateChatRequest;
import dev.thural.quietspace.domain.chat.dto.UpdateChatRequest;
import dev.thural.quietspace.domain.chat.Chat;
import dev.thural.quietspace.domain.chat.ChatMapper;
import dev.thural.quietspace.domain.chat.ChatRepository;
import dev.thural.quietspace.domain.chat.ChatService;
import dev.thural.quietspace.domain.chat.port.ChatMessagePort;
import dev.thural.quietspace.core.shared.exception.CustomErrorException;
import dev.thural.quietspace.core.shared.exception.UnauthorizedException;
import dev.thural.quietspace.core.shared.exception.UserNotFoundException;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserMapper;
import dev.thural.quietspace.domain.user.UserService;
import dev.thural.quietspace.domain.user.dto.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final UserService userService;
    private final ChatRepository chatRepository;
    private final ChatMessagePort messagePort;
    private final ChatMapper chatMapper;
    private final UserMapper userMapper;


    @Override
    public List<ChatResponse> getChatsByUserId(UUID memberId) {
        User loggedUser = userService.getSignedUser();
        if (!loggedUser.getId().equals(memberId)) throw new UnauthorizedException("user mismatch with the chat member");
        return chatRepository.findAllByUsersId(memberId).stream().map(chatMapper::chatEntityToResponse).toList();
    }

    @Override
    public void deleteChatById(UUID chatId) {
        findChatEntityById(chatId);
        chatRepository.deleteById(chatId);
    }

    @Override
    @Transactional
    public UserResponse addMemberWithId(UUID memberId, UUID chatId) {
        Chat foundChat = findChatEntityById(chatId);
        User foundMember = userService.getUserById(memberId).orElseThrow(UserNotFoundException::new);
        foundChat.addMember(foundMember);
        return userMapper.toResponse(foundMember);
    }

    @Override
    @Transactional
    public List<UserResponse> removeMemberWithId(UUID memberId, UUID chatId) {
        Chat foundChat = findChatEntityById(chatId);
        User foundMember = getUserById(memberId);
        foundChat.removeMember(foundMember);
        chatRepository.save(foundChat);
        return foundChat.getUsers().stream().map(userMapper::toResponse).toList();
    }

    @Override
    public ChatResponse createChat(CreateChatRequest chatRequest) {
        List<User> userList = userService.getUsersFromIdList(chatRequest.getUserIds());
        User loggedUser = userService.getSignedUser();
        if (!userList.contains(loggedUser)) throw new UnauthorizedException("requesting user is not member of chat");
        boolean isChatDuplicate = chatRepository.findAllByUsersIn(userList).stream()
                .anyMatch(chat -> new HashSet<>(chat.getUsers()).containsAll(userList));
        if (isChatDuplicate) throw new CustomErrorException("a chat with same members already exists");
        Chat createdChat = chatRepository.save(chatMapper.chatRequestToEntity(chatRequest));
        userService.getUserById(chatRequest.getRecipientId()).orElseThrow();
        messagePort.postFirstMessage(createdChat.getId(), loggedUser.getId(), chatRequest.getRecipientId(),
                chatRequest.getText());
        return chatMapper.chatEntityToResponse(createdChat);
    }

    @Override
    public ChatResponse getChatById(UUID chatId) {
        Chat foundChat = findChatEntityById(chatId);
        return chatMapper.chatEntityToResponse(foundChat);
    }

    @Override
    @Transactional
    public ChatResponse updateChat(UUID chatId, UpdateChatRequest request) {
        Chat foundChat = findChatEntityById(chatId);
        if (request.getName() != null) foundChat.setName(request.getName());
        Chat saved = chatRepository.save(foundChat);
        return chatMapper.chatEntityToResponse(saved);
    }

    private User getUserById(UUID memberId) {
        return userService.getUserById(memberId).orElseThrow(() -> new UserNotFoundException("user not found"));
    }

    public Chat findChatEntityById(UUID chatId) {
        User loggedUser = userService.getSignedUser();
        Chat foundChat = chatRepository.findById(chatId).orElseThrow(EntityNotFoundException::new);
        if (!foundChat.getUsers().contains(loggedUser)) throw new UnauthorizedException("chat user mismatch");
        return foundChat;
    }

}
