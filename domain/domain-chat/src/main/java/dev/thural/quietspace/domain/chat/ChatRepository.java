package dev.thural.quietspace.chat;

import dev.thural.quietspace.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
    List<Chat> findAllByUsersId(UUID userId);

    @Query("SELECT c FROM Chat c JOIN c.users u WHERE u IN :users")
    List<Chat> findAllByUsersIn(@Param("users") Collection<User> users);
}
