package org.example;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileLinkRepository implements LinkRepository {

    private static class Snapshot implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        Map<String, LinkRecord> linksByShort;
        Map<UUID, UserProfile> users;
    }

    private final String filePath;

    private final Map<String, LinkRecord> linksByShort = new ConcurrentHashMap<>();
    private final Map<UUID, UserProfile> users = new ConcurrentHashMap<>();

    public FileLinkRepository(String filePath) {
        this.filePath = filePath;
        if(Files.exists(Path.of(filePath)))
            load();
    }

    @Override
    public boolean shortExists(String shortUrl) {
        return linksByShort.containsKey(shortUrl);
    }

    @Override
    public Optional<LinkRecord> findByLong(String longUrl){
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

    @Override
    public void save() {
        Snapshot snap = new Snapshot();
        snap.linksByShort = new HashMap<>(linksByShort);
        snap.users = new HashMap<>(users);

        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)))) {
            oos.writeObject(snap);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save repository: " + e.getMessage(), e);
        }
    }

    @Override
    public void load() {
        File f = new File(filePath);
        if (!f.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            Object o = ois.readObject();
            Snapshot snap = (Snapshot) o;

            linksByShort.clear();
            users.clear();

            if (snap.linksByShort != null) linksByShort.putAll(snap.linksByShort);
            if (snap.users != null) users.putAll(snap.users);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load repository: " + e.getMessage(), e);
        }
    }
}