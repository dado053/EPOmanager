package com.example.epostats.models;

import com.google.gson.annotations.SerializedName;

public class Match {
    @SerializedName("match_id")
    private int matchId;

    @SerializedName("matchday")
    private int matchday;

    @SerializedName("home_team_name")
    private String homeTeamName;

    @SerializedName("away_team_name")
    private String awayTeamName;

    @SerializedName("home_team_id")
    private int homeTeamId;

    @SerializedName("away_team_id")
    private int awayTeamId;

    public int getMatchId() { return matchId; }
    public int getMatchday() { return matchday; }
    public String getHomeTeamName() { return homeTeamName; }
    public String getAwayTeamName() { return awayTeamName; }
    public int getHomeTeamId() { return homeTeamId; }
    public int getAwayTeamId() { return awayTeamId; }
}