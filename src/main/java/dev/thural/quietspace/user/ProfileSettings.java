package dev.thural.quietspace.user;
import dev.thural.quietspace.shared.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ProfileSettings extends BaseEntity implements Serializable {

    public ProfileSettings(User user) {
        this.user = user;
    }

    @NotNull
    @OneToOne
    User user;

    String bio;

    @Builder.Default
    List<User> blockedUsers = new ArrayList<>();

    @Builder.Default
    Boolean isPrivateAccount = false;
    @Builder.Default
    Boolean isNotificationsMuted = false;
    @Builder.Default
    Boolean isAllowPublicGroupChatInvite = true;
    @Builder.Default
    Boolean isAllowPublicMessageRequests = true;
    @Builder.Default
    Boolean isAllowPublicComments = true;
    @Builder.Default
    Boolean isHideLikeCounts = false;

    public void blockUser(User target) {
        if (blockedUsers.contains(target)) {
            throw new IllegalArgumentException("User is already blocked");
        }
        blockedUsers.add(target);
    }

    public void unblockUser(User target) {
        blockedUsers.remove(target);
    }

    public boolean isBlocked(User target) {
        return blockedUsers.contains(target);
    }
}
