package org.example;

public class ResolveResult {
    private final ResolveStatus status;
    private final String longUrl; // only for OK

    private ResolveResult(ResolveStatus status, String longUrl) {
        this.status = status;
        this.longUrl = longUrl;
    }

    public static ResolveResult ok(String longUrl) { return new ResolveResult(ResolveStatus.OK, longUrl); }
    public static ResolveResult nf() { return new ResolveResult(ResolveStatus.NOT_FOUND, null); }
    public static ResolveResult expired() { return new ResolveResult(ResolveStatus.EXPIRED, null); }
    public static ResolveResult limit() { return new ResolveResult(ResolveStatus.LIMIT_REACHED, null); }

    public ResolveStatus getStatus() { return status; }
    public String getLongUrl() { return longUrl; }
}