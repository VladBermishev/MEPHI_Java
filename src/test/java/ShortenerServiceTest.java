import org.example.*;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ShortenerServiceTest {

    static class FakeClock implements ShortenerService.Clock {
        private Instant now;
        FakeClock(Instant start) { this.now = start; }
        @Override public Instant now() { return now; }
        void plusSeconds(long s) { now = now.plusSeconds(s); }
    }

    @Test
    void differentUsersGetDifferentShortLinksForSameLongUrl() {
        InMemoryLinkRepository repo = new InMemoryLinkRepository();
        Base62Generator gen = new Base62Generator();
        FakeClock clock = new FakeClock(Instant.parse("2026-01-29T12:00:00Z"));
        ShortenerService svc = new ShortenerService(repo, gen, clock);

        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        LinkRecord a = svc.createShortLink(u1,  "https://example.com/x", null, Duration.ofHours(1), 7, "clck.local/");
        LinkRecord b = svc.createShortLink(u2, "https://example.com/x", null, Duration.ofHours(1), 7, "clck.local/");

        assertNotEquals(a.getShortUrl(), b.getShortUrl());
    }

    @Test
    void clickLimitBlocksAfterMaxClicksButAllowsHitClick() {
        InMemoryLinkRepository repo = new InMemoryLinkRepository();
        Base62Generator gen = new Base62Generator();
        FakeClock clock = new FakeClock(Instant.parse("2026-01-29T12:00:00Z"));
        ShortenerService svc = new ShortenerService(repo, gen, clock);

        UUID u1 = UUID.randomUUID();
        LinkRecord r = svc.createShortLink(u1, "https://example.com/limit", 2, Duration.ofHours(1), 7, "clck.local/");

        assertEquals(ResolveStatus.OK, svc.resolveAndRegisterClick(r.getShortUrl()).getStatus());
        assertEquals(ResolveStatus.OK, svc.resolveAndRegisterClick(r.getShortUrl()).getStatus());
        assertEquals(ResolveStatus.LIMIT_REACHED, svc.resolveAndRegisterClick(r.getShortUrl()).getStatus());

        List<String> notes = svc.popNotifications(u1);
        assertTrue(notes.stream().anyMatch(s -> s.contains("Click limit reached")));
    }

    @Test
    void expiredLinkIsRemovedAndNotifiesOwner() {
        InMemoryLinkRepository repo = new InMemoryLinkRepository();
        Base62Generator gen = new Base62Generator();
        FakeClock clock = new FakeClock(Instant.parse("2026-01-29T12:00:00Z"));
        ShortenerService svc = new ShortenerService(repo, gen, clock);

        UUID u1 = UUID.randomUUID();
        LinkRecord r = svc.createShortLink(u1, "https://example.com/ttl", null, Duration.ofSeconds(5), 7, "clck.local/");

        clock.plusSeconds(10);

        assertEquals(ResolveStatus.EXPIRED, svc.resolveAndRegisterClick(r.getShortUrl()).getStatus());

        List<String> notes = svc.popNotifications(u1);
        assertTrue(notes.stream().anyMatch(s -> s.contains("expired")));
    }
}