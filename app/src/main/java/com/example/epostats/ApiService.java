package com.example.epostats;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @GET("get_matches.php")
    Call<List<Match>> getMatches();
    @GET("get_match_events.php")
    Call<List<MatchEvent>> getMatchEvents(
            @Query("match_id") int matchId);
    @GET("get_players.php")
    Call<List<Player>> getTeamPlayers(@Query("team_id") int teamId);

    @FormUrlEncoded
    @POST("api_add_event.php")
    Call<ApiResponse> addMatchEvent(
            @Field("match_id") int matchId,
            @Field("team_id") int teamId,
            @Field("event_minute") int minute,
            @Field("action_type") String type,
            @Field("action_detail") String detail,
            @Field("action_result") String result,
            @Field("player_id") int playerId
    );
}