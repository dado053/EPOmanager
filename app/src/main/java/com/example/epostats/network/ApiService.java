package com.example.epostats.network;

import com.example.epostats.models.Championship;
import com.example.epostats.models.Match;
import com.example.epostats.models.MatchEvent;
import com.example.epostats.models.Player;
import com.example.epostats.network.ApiResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @GET("get_matches.php")
    Call<List<Match>> getMatches(@Query("championship_id") int championshipId);

    @GET("api_get_championships.php")
    Call<List<Championship>> getChampionship();

    @GET("get_match_events.php")
    Call<List<MatchEvent>> getMatchEvents(
            @Query("match_id") int matchId);
    @GET("get_players.php")
    Call<List<Player>> getTeamPlayers(@Query("team_id") int teamId);

    @FormUrlEncoded
    @POST("api_add_event.php") // Ή όπως λέγεται το δικό σου endpoint για προσθήκη event
    Call<ApiResponse> addMatchEvent(
            @Field("match_id") int matchId,
            @Field("team_id") int teamId,
            @Field("event_minute") String eventMinute,
            @Field("action_type") String actionType,
            @Field("action_detail") String actionDetail,
            @Field("action_result") String actionResult,
            @Field("player_id") int playerId
    );

    @FormUrlEncoded
    @POST("api_start_match.php")
    Call<ApiResponse> startMatchTimer(
            @Field("match_id") int matchId,
            @Field("start_time") long startTime
    );

    @FormUrlEncoded
    @POST("api_update_injury_time.php")
    Call<ApiResponse> updateInjuryTime(
            @Field("match_id") int matchId,
            @Field("injury_1st_half") int injury1st,
            @Field("injury_2nd_half") int injury2nd
    );
}