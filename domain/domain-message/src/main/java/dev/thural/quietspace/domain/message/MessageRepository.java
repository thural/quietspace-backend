package dev.thural.quietspace.message;

import dev.thural.quietspace.chat.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findAllByChatId(UUID chatId, Pageable pageable);

    Optional<Message> findFirstByChatOrderByCreateDateDesc(Chat chat);

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId AND m.id = :messageId")
    Optional<Message> findByMessageIdAndChatId(UUID messageId, UUID chatId);

    long countByRecipientIdAndIsSeen(UUID recipientId, Boolean isSeen);
}
