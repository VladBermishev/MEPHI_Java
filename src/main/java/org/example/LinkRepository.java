package org.example;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository {
    // Links
    boolean shortExists(String shortUrl);
    Optional<LinkRecord> findByLong(String longUrl);
    Optional<LinkRecord> findByShort(String shortUrl);
    void saveLink(LinkRecord record);
    void deleteByShort(String shortUrl);
    List<LinkRecord> findByOwner(UUID ownerId);
    List<LinkRecord> findAllLinks();

    // Users & notifications
    void ensureUser(UUID userId);
    void pushNotification(UUID userId, String message);
    List<String> popNotifications(UUID userId);

    // Persistence hooks (for file repo can persist, in-memory can no-op)
    void save();
    void load();
}