package dev.thural.quietspace.notification;

import dev.thural.quietspace.notification.dto.NotificationResponse;
import dev.thural.quietspace.shared.exception.UserNotFoundException;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final UserRepository userRepository;

    public NotificationResponse toResponse(Notification notification) {
        User foundUser = userRepository.findById(notification.getActorId()).orElseThrow(UserNotFoundException::new);

        var response = new NotificationResponse();
        BeanUtils.copyProperties(notification, response);
        response.setActorId(foundUser.getId());
        response.setType(notification.getNotificationType());
        return response;
    }

}
