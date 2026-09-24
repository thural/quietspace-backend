package dev.thural.quietspace.user;

import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.photo.dto.PhotoResponse;
import dev.thural.quietspace.shared.entity.BaseEntity;
import dev.thural.quietspace.user.dto.ProfileSettingsResponse;
import dev.thural.quietspace.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final PhotoService photoService;

    public UserResponse toResponse(User user) {
        PhotoResponse profilePhoto = getProfilePhoto(user);
        var response = new UserResponse();
        BeanUtils.copyProperties(user, response);
        response.setPhoto(profilePhoto);
        response.setIsFollower(user.getFollowers() != null && user.getFollowers().contains(user));
        response.setIsFollowing(user.getFollowings() != null && user.getFollowings().contains(user));
        response.setRole(user.getRole().name());
        ProfileSettings profileSettings = user.getProfileSettings();
        response.setIsPrivateAccount(profileSettings != null ? profileSettings.getIsPrivateAccount() : null);
        response.setBio(profileSettings != null ? profileSettings.getBio() : null);
        return response;
    }

    public UserResponse toProfileResponse(User user) {
        var response = new UserResponse();
        PhotoResponse profilePhoto = getProfilePhoto(user);
        BeanUtils.copyProperties(user, response);
        response.setPhoto(profilePhoto);
        response.setRole(user.getRole().name());
        ProfileSettings profileSettings = user.getProfileSettings();
        response.setIsPrivateAccount(profileSettings != null ? profileSettings.getIsPrivateAccount() : null);
        response.setBio(profileSettings != null ? profileSettings.getBio() : null);
        if (profileSettings != null) {
            var settings = new ProfileSettingsResponse();
            BeanUtils.copyProperties(profileSettings, settings);
            response.setSettings(settings);
        }
        return response;
    }

    public ProfileSettingsResponse toSettingsResponse(User user) {
        var response = new ProfileSettingsResponse();
        BeanUtils.copyProperties(user.getProfileSettings(), response);
        List<UUID> blockedUserIds = user.getProfileSettings().getBlockedUsers() != null
                ? user.getProfileSettings().getBlockedUsers().stream().map(BaseEntity::getId).toList()
                : null;
        response.setBlockedUserIds(blockedUserIds);
        return response;
    }

    private PhotoResponse getProfilePhoto(User user) {
        UUID photoId = user.getPhotoId();
        return photoId == null ? null
                : photoService.getPhotoById(photoId);
    }

}
