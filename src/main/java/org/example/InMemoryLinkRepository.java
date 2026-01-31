package org.example;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLinkRepository implements LinkRepository {
    private final Map<String, LinkRecord> linksByShort = new ConcurrentHashMap<>();
    private final Map<UUID, UserProfile> users = new ConcurrentHashMap<>();

    @Override
    public boolean shortExists(String shortUrl) {
        return linksByShort.containsKey(shortUrl);
    }

    @Override
    public Optional<LinkRecord> findByLong(String longUrl) {
        return linksByShort.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getLongUrl().equals(longUrl))
                .findFirst().map(Map.Entry::getValue);
    }

    @Override
    public Optional<LinkRecord> findByShort(String shortUrl) {
        return Optional.ofNullable(linksByShort.get(shortUrl));
    }

    @Override
    public void saveLink(LinkRecord record) {
        linksByShort.put(record.getShortUrl(), record);
    }

    @Override
    public void deleteByShort(String shortUrl) {
        linksByShort.remove(shortUrl);
    }

    @Override
    public List<LinkRecord> findByOwner(UUID ownerId) {
        List<LinkRecord> out = new ArrayList<>();
        for (LinkRecord r : linksByShort.values()) {
            if (r.getOwnerId().equals(ownerId)) out.add(r);
        }
        out.sort(Comparator.comparing(LinkRecord::getCreatedAt));
        return out;
    }

    @Override
    public List<LinkRecord> findAllLinks() {
        return new ArrayList<>(linksByShort.values());
    }

    @Override
    public void ensureUser(UUID userId) {
        users.computeIfAbsent(userId, UserProfile::new);
    }

    @Override
    public void pushNotification(UUID userId, String message) {
        ensureUser(userId);
        users.get(userId).pushNotification(message);
    }

    @Override
    public List<String> popNotifications(UUID userId) {
        ensureUser(userId);
        return users.get(userId).popAllNotifications();
    }

    @Override public void save() { /* no-op */ }
    @Override public void load() { /* no-op */ }
}