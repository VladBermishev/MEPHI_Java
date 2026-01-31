package org.example;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class UserProfile implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final Deque<String> notifications = new ArrayDeque<>();

    public UserProfile(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }

    public void pushNotification(String msg) {
        notifications.addLast(msg);
    }

    public List<String> popAllNotifications() {
        List<String> out = new ArrayList<>();
        while (!notifications.isEmpty()) out.add(notifications.removeFirst());
        return out;
    }
}
