package dev.thural.quietspace.core.messaging.event;

public enum EventType {
    CONNECT,
    DISCONNECT,
    DELETE_MESSAGE,
    SEEN_MESSAGE,
    SEEN_NOTIFICATION,
    JOINED_CHAT,
    LEFT_CHAT,
    EXCEPTION
}