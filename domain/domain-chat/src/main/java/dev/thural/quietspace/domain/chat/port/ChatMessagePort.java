package dev.thural.quietspace.domain.chat.port;

import dev.thural.quietspace.domain.chat.dto.ChatMessageView;

import java.util.Optional;
import java.util.UUID;

/**
 * Driven port decoupling domain-chat from domain-message.
 *
 * <p>Defined by the consumer (domain-chat), implemented by the message domain.
 * Keeps the module dependency pointing a single way: message &rarr; chat.</p>
 */
public interface ChatMessagePort {

    /**
     * Find the most recent message of a chat.
     *
     * @param chatId the chat id
     * @return the most recent message view, or empty when the chat has no messages
     */
    Optional<ChatMessageView> findLastMessage(UUID chatId);

    /**
     * Post the initial message of a newly created chat.
     *
     * @param chatId      the chat id
     * @param senderId    the sender user id
     * @param recipientId the recipient user id
     * @param text        the message text
     * @return the posted message view
     */
    ChatMessageView postFirstMessage(UUID chatId, UUID senderId, UUID recipientId, String text);
}
