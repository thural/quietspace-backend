package dev.thural.quietspace.websocket.event;

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