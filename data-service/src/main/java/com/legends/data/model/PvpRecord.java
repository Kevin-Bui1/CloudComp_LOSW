package com.legends.data.model;

import jakarta.persistence.*;

/**
 * PvpRecord stores the cumulative win/loss count for a user.
 * One row per user; upserted when a match result comes in.
 */
@Entity
@Table(name = "pvp_records")
public class PvpRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String username;

    private int wins   = 0;
    private int losses = 0;

    public PvpRecord() {}

    public PvpRecord(Long userId, String username) {
        this.userId   = userId;
        this.username = username;
    }

    public Long   getId()                  { return id; }
    public Long   getUserId()              { return userId; }
    public void   setUserId(Long id)       { this.userId = id; }
    public String getUsername()            { return username; }
    public void   setUsername(String u)    { this.username = u; }
    public int    getWins()                { return wins; }
    public void   setWins(int w)           { this.wins = w; }
    public int    getLosses()              { return losses; }
    public void   setLosses(int l)         { this.losses = l; }
    public void   incrementWins()          { this.wins++; }
    public void   incrementLosses()        { this.losses++; }
}
