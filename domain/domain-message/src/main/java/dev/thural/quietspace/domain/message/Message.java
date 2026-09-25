package dev.thural.quietspace.domain.message;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.thural.quietspace.core.shared.entity.BaseEntity;
import dev.thural.quietspace.domain.chat.Chat;
import dev.thural.quietspace.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Message extends BaseEntity {

    @ManyToOne
    @JsonBackReference
    private Chat chat;

    @NotNull
    @ManyToOne
    @JsonBackReference
    private User sender;

    @NotNull
    @ManyToOne
    private User recipient;

    @NotBlank
    @Column(length = 999)
    private String text;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "photo_id", columnDefinition = "varchar(36)")
    private UUID photoId;

    @NotNull
    private Boolean isSeen;

    @PrePersist
    void initFields() {
        setIsSeen(false);
    }

    // Factory method with validation
    public static Message create(User sender, User recipient, Chat chat, String text) {
        if (sender == null || recipient == null || chat == null) {
            throw new IllegalArgumentException("Sender, recipient, and chat are required");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Message text cannot be empty");
        }
        if (sender.equals(recipient)) {
            throw new IllegalArgumentException("Sender and recipient cannot be the same user");
        }
        
        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setChat(chat);
        message.setText(text);
        message.setIsSeen(false);
        return message;
    }

    // Domain method for marking as seen (read receipt)
    public void markAsSeen() {
        this.isSeen = true;
    }

    // Domain method for checking if seen
    public boolean isSeen() {
        return Boolean.TRUE.equals(this.isSeen);
    }

}