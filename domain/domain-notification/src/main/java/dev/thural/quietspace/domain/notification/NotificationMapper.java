package dev.thural.quietspace.domain.notification;

import dev.thural.quietspace.domain.notification.dto.NotificationResponse;
import dev.thural.quietspace.domain.notification.port.NotificationUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final NotificationUserPort userPort;

    public NotificationResponse toResponse(Notification notification) {
        // Validates the actor still exists, as before (throws when unknown).
        userPort.findUsernameById(notification.getActorId());

        var response = new NotificationResponse();
        BeanUtils.copyProperties(notification, response);
        response.setActorId(notification.getActorId());
        response.setType(notification.getNotificationType());
        return response;
    }

}
