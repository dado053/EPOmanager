package com.example.epostats.models;

import com.google.gson.annotations.SerializedName;

public class MatchEvent {
    private int id;
    @SerializedName("match_id") private int matchId;
    @SerializedName("team_id") private int teamId;
    @SerializedName("event_minute") private String eventMinute;
    @SerializedName("action_type") private String actionType;
    @SerializedName("action_detail") private String actionDetail;
    @SerializedName("action_result") private String actionResult;
    @SerializedName("player_name") private String playerName;
    @SerializedName("assist_player_name") private String assistPlayerName;

    // Getters
    public int getId() { return id; }
    public int getMatchId() { return matchId; }
    public int getTeamId() { return teamId; }
    public String getEventMinute() { return eventMinute; }
    public String getActionType() { return actionType; }
    public String getActionDetail() { return actionDetail; }
    public String getActionResult() { return actionResult; }
    public String getPlayerName() { return playerName; }
    public String getAssistPlayerName() { return assistPlayerName; }

    private long startTime;
    public long getStartTime() { return startTime; }
}