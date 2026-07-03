package com.r35157.libs.notification;

import java.io.IOException;

public interface AddressedNotifier<
        D extends NotificationDestination,
        M extends NotificationMessage>
{
    void push(D destination, M message) throws IOException;

    default BoundNotifier<M> bind(D destination) {
        return message -> push(destination, message);
    }
}