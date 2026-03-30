package com.legends.data.model;

import jakarta.persistence.*;

/**
 * Score records one completed campaign run and the points earned.
 * Multiple scores per user are allowed so we can find their best.
 */
@Entity
@Table(name = "scores")
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scoreId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private int score;

    public Score() {}

    public Score(Long userId, String username, int score) {
        this.userId   = userId;
        this.username = username;
        this.score    = score;
    }

    public Long   getScoreId()             { return scoreId; }
    public Long   getUserId()              { return userId; }
    public void   setUserId(Long id)       { this.userId = id; }
    public String getUsername()            { return username; }
    public void   setUsername(String u)    { this.username = u; }
    public int    getScore()               { return score; }
    public void   setScore(int s)          { this.score = s; }
}
