package com.legends.profile.model;

import jakarta.persistence.*;

/**
 * UserProfile is the JPA entity that maps to the "users" table in MySQL.
 *
 * I use @Entity and @Table to tell Spring/Hibernate to manage this class as a
 * database table. The @Id + @GeneratedValue combo lets MySQL auto-generate the userId.
 *
 * Fields:
 *   username     — what the player uses to log in and to receive PvP invitations
 *   passwordHash — BCrypt hash of their password (NEVER the plain text)
 *   scores       — total score accumulated across all completed campaigns
 *   rankings     — their current position in the league table
 *   campaignProgress — which room they're on (0-30) in their active campaign
 */
@Entity
@Table(name = "users")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private int scores = 0;
    private int rankings = 0;
    private int campaignProgress = 0;

    public UserProfile() {}

    public UserProfile(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getUserId()                     { return userId; }
    public void setUserId(Long userId)          { this.userId = userId; }
    public String getUsername()                 { return username; }
    public void setUsername(String username)    { this.username = username; }
    public String getPasswordHash()             { return passwordHash; }
    public void setPasswordHash(String h)       { this.passwordHash = h; }
    public int getScores()                      { return scores; }
    public void setScores(int scores)           { this.scores = scores; }
    public int getRankings()                    { return rankings; }
    public void setRankings(int rankings)       { this.rankings = rankings; }
    public int getCampaignProgress()            { return campaignProgress; }
    public void setCampaignProgress(int p)      { this.campaignProgress = p; }
}
