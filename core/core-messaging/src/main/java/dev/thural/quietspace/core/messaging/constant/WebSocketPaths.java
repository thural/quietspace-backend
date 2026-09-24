package dev.thural.quietspace.websocket.constant;

public final class WebSocketPaths {

    private WebSocketPaths() {}

    public static final String PUBLIC_CHAT = "/public/chat";
    public static final String PRIVATE_CHAT = "/private/chat";
    public static final String CHAT_EVENT = PRIVATE_CHAT + "/event";
    public static final String LEAVE_CHAT = PRIVATE_CHAT + "/leave";
    public static final String JOIN_CHAT = PRIVATE_CHAT + "/join";
    public static final String DELETE_MESSAGE = PRIVATE_CHAT + "/delete/{messageId}";
    public static final String SEEN_MESSAGE = PRIVATE_CHAT + "/seen/{messageId}";

    public static final String SET_ONLINE_STATUS = "/user/setOnlineStatus";
    public static final String ONLINE_USERS = "/user/onlineUsers";

    public static final String NOTIFICATION_SUBJECT = "/private/notifications";
    public static final String NOTIFICATION_EVENT = NOTIFICATION_SUBJECT + "/event";
    public static final String NOTIFICATION_SEEN = NOTIFICATION_SUBJECT + "/seen/{notificationId}";

    public static final String TYPING_STATUS = PRIVATE_CHAT + "/typing";

    public static final String USER_PUBLIC = "/user/public";
    public static final String ERROR_EVENT = "/_error";
    public static final String SYSTEM_EVENT = "/system/event";
    public static final String UNREAD_COUNT = "/unread-count";

    public static final String PUBLIC_BROKER = "/public";
}
