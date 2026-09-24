package dev.thural.quietspace.user.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSettingsRequest {

    String bio;

    Boolean isPrivateAccount;
    Boolean isNotificationsMuted;
    Boolean isAllowPublicGroupChatInvite;
    Boolean isAllowPublicMessageRequests;
    Boolean isAllowPublicComments;
    Boolean isHideLikeCounts;

}