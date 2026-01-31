package org.example;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class LinkRecord implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final UUID ownerId;
    private final String longUrl;
    private final String shortUrl;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Integer maxClicks; // null => unlimited

    private int clicks;
    private boolean blockedByLimit;

    public LinkRecord(UUID ownerId,
                      String longUrl,
                      String shortUrl,
                      Instant createdAt,
                      Instant expiresAt,
                      Integer maxClicks) {
        this.ownerId = ownerId;
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.maxClicks = maxClicks;
        this.clicks = 0;
        this.blockedByLimit = false;
    }

    public UUID getOwnerId() { return ownerId; }
    public String getLongUrl() { return longUrl; }
    public String getShortUrl() { return shortUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Integer getMaxClicks() { return maxClicks; }
    public int getClicks() { return clicks; }
    public boolean isBlockedByLimit() { return blockedByLimit; }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void registerClick() {
        this.clicks++;
        if (maxClicks != null && clicks >= maxClicks) {
            this.blockedByLimit = true;
        }
    }
}