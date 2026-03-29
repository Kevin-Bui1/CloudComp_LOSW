package com.legends.profile.dto;

/** Response returned to the client after a successful auth operation. */
public class AuthResponse {
    private boolean success;
    private String message;
    private Long userId;
    private String username;
    private int scores;
    private int rankings;
    private int campaignProgress;

    public AuthResponse() {}

    public static AuthResponse ok(Long userId, String username, int scores, int rankings, int progress) {
        AuthResponse r = new AuthResponse();
        r.success = true;
        r.message = "OK";
        r.userId = userId;
        r.username = username;
        r.scores = scores;
        r.rankings = rankings;
        r.campaignProgress = progress;
        return r;
    }

    public static AuthResponse error(String message) {
        AuthResponse r = new AuthResponse();
        r.success = false;
        r.message = message;
        return r;
    }

    public boolean isSuccess()              { return success; }
    public String getMessage()              { return message; }
    public Long getUserId()                 { return userId; }
    public String getUsername()             { return username; }
    public int getScores()                  { return scores; }
    public int getRankings()                { return rankings; }
    public int getCampaignProgress()        { return campaignProgress; }
}
