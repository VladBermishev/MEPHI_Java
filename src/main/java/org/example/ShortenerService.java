package org.example;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class ShortenerService {

    private final LinkRepository repo;
    private final ShortCodeGenerator generator;
    private final Clock clock;

    public interface Clock {
        Instant now();
    }

    public static class SystemClock implements Clock {
        @Override public Instant now() { return Instant.now(); }
    }

    public ShortenerService(LinkRepository repo, ShortCodeGenerator generator) {
        this(repo, generator, new SystemClock());
    }

    public ShortenerService(LinkRepository repo, ShortCodeGenerator generator, Clock clock) {
        this.repo = repo;
        this.generator = generator;
        this.clock = clock;
    }

    public LinkRecord createShortLink(UUID userId,
                                      String longUrl,
                                      Integer maxClicks,
                                      Duration ttl,
                                      int codeLen,
                                      String prefix) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(longUrl, "longUrl");
        Objects.requireNonNull(ttl, "ttl");
        Objects.requireNonNull(prefix, "prefix");
        if (codeLen <= 0) throw new IllegalArgumentException("codeLen must be > 0");
        try{
            URI.create(longUrl).toURL();
        }catch (MalformedURLException e){
            throw new IllegalArgumentException("invalid URL");
        }

        repo.ensureUser(userId);
        /*
        Optional<LinkRecord> searchResult = repo.findByLong(longUrl);
        if(searchResult.isPresent())
            return searchResult.get();
        */
        String shortUrl;
        do {
            String code = generator.nextCode(codeLen);
            shortUrl = Paths.get(prefix, code).toString();
        } while (repo.shortExists(shortUrl));

        Instant now = clock.now();
        Instant exp = now.plus(ttl);

        LinkRecord record = new LinkRecord(userId, longUrl, shortUrl, now, exp, (maxClicks != null && maxClicks > 0) ? maxClicks : null);
        repo.saveLink(record);
        repo.save();
        return record;
    }

    public ResolveResult resolveAndRegisterClick(String shortUrl) {
        Objects.requireNonNull(shortUrl, "shortUrl");
        Optional<LinkRecord> opt = repo.findByShort(shortUrl);
        if (opt.isEmpty()) return ResolveResult.nf();

        LinkRecord r = opt.get();
        Instant now = clock.now();

        if (r.isExpired(now)) {
            repo.deleteByShort(shortUrl);
            repo.pushNotification(r.getOwnerId(), now + " — Link expired and was removed: " + shortUrl);
            repo.save();
            return ResolveResult.expired();
        }

        if (r.isBlockedByLimit()) {
            repo.pushNotification(r.getOwnerId(), now + " — Click limit was reached for: " + shortUrl + " (blocked)");
            return ResolveResult.limit();
        }
        r.registerClick();
        if (r.isBlockedByLimit())
            repo.pushNotification(r.getOwnerId(), now + " — Click limit reached for: " + shortUrl + " (blocked)");

        repo.save();
        return ResolveResult.ok(r.getLongUrl());
    }

    public List<LinkRecord> listLinksByUser(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        return repo.findByOwner(userId);
    }

    public boolean deleteLink(UUID userId, String shortUrl) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(shortUrl, "shortUrl");
        Optional<LinkRecord> opt = repo.findByShort(shortUrl);
        if (opt.isEmpty()) return false;
        if (!opt.get().getOwnerId().equals(userId)) return false;
        repo.deleteByShort(shortUrl);
        repo.save();
        return true;
    }
    public int cleanupExpiredLinks() {
        Instant now = clock.now();
        List<LinkRecord> all = repo.findAllLinks();
        int removed = 0;
        for (LinkRecord r : all) {
            if (r.isExpired(now)) {
                repo.deleteByShort(r.getShortUrl());
                repo.pushNotification(r.getOwnerId(), now + " — Link expired and was auto-removed: " + r.getShortUrl());
                removed++;
            }
        }
        if (removed > 0) repo.save();
        return removed;
    }

    public List<String> popNotifications(UUID userId) {
        return repo.popNotifications(userId);
    }
}