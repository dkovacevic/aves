package com.aves.server.notifications;

/**
 * Wrapper for exceptions thrown during sending notifications.
 */
public class NotificationException extends RuntimeException {
    public NotificationProvider provider;

    public NotificationException(String message, NotificationProvider provider, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }
}
