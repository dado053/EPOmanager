package com.example.epostats.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

import com.example.epostats.R;
import com.example.epostats.network.RetrofitClient;

public class StandingsActivity extends AppCompatActivity {

    private RecyclerView rvStandings, rvScorers;
    private DynamicStandingsApi api;
    private int championshipId = 1; // Δεν είναι πια final, αλλάζει δυναμικά!

    interface DynamicStandingsApi {
        @GET("api_get_standings.php")
        Call<List<ServerStanding>> getLiveStandings(@Query("championship_id") int champId);

        @GET("api_get_player_stats.php")
        Call<List<ServerScorer>> getLiveScorers(@Query("championship_id") int champId);
    }

    static class ServerStanding { String team_name; int points; }
    static class ServerScorer { String player_name; String team_name; int goals; int assists; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_standings);

        // Διάβασμα του ID από το Intent
        if (getIntent() != null) {
            championshipId = getIntent().getIntExtra("CHAMPIONSHIP_ID", 1);
        }

        TabLayout tabLayout = findViewById(R.id.tabLayoutStandings);
        LinearLayout layoutStandings = findViewById(R.id.layoutStandings);
        LinearLayout layoutScorers = findViewById(R.id.layoutScorers);
        rvStandings = findViewById(R.id.rvStandings);
        rvScorers = findViewById(R.id.rvScorers);

        rvStandings.setLayoutManager(new LinearLayoutManager(this));
        rvScorers.setLayoutManager(new LinearLayoutManager(this));

        tabLayout.addTab(tabLayout.newTab().setText("ΒΑΘΜΟΛΟΓΙΑ"));
        tabLayout.addTab(tabLayout.newTab().setText("ΣΚΟΡΕΡ"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutStandings.setVisibility(View.VISIBLE);
                    layoutScorers.setVisibility(View.GONE);
                } else {
                    layoutStandings.setVisibility(View.GONE);
                    layoutScorers.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        api = RetrofitClient.getClient().create(DynamicStandingsApi.class);

        fetchLiveStandings();
        fetchLiveScorers();
    }

    private void fetchLiveStandings() {
        api.getLiveStandings(championshipId).enqueue(new Callback<List<ServerStanding>>() {
            @Override
            public void onResponse(Call<List<ServerStanding>> call, Response<List<ServerStanding>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ServerStanding> list = response.body();
                    Collections.sort(list, (t1, t2) -> Integer.compare(t2.points, t1.points));
                    rvStandings.setAdapter(new StandingAdapter(list));
                }
            }
            @Override public void onFailure(Call<List<ServerStanding>> call, Throwable t) {
                Toast.makeText(StandingsActivity.this, "Σφάλμα βαθμολογίας", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchLiveScorers() {
        api.getLiveScorers(championshipId).enqueue(new Callback<List<ServerScorer>>() {
            @Override
            public void onResponse(Call<List<ServerScorer>> call, Response<List<ServerScorer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ServerScorer> list = response.body();
                    Collections.sort(list, (p1, p2) -> Integer.compare(p2.goals, p1.goals));
                    rvScorers.setAdapter(new ScorerAdapter(list));
                } else {
                    Toast.makeText(StandingsActivity.this, "Server Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<List<ServerScorer>> call, Throwable t) {
                // ΕΔΩ: Εμφανίζουμε όλο το σφάλμα στην οθόνη για να δούμε τι φταίει
                Toast.makeText(StandingsActivity.this, "Αιτία σφάλματος: " + t.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }

    static class StandingAdapter extends RecyclerView.Adapter<StandingAdapter.ViewHolder> {
        List<ServerStanding> list;
        StandingAdapter(List<ServerStanding> l) { list = l; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int v) { return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_standing, p, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) { ServerStanding i = list.get(pos); h.tvRank.setText(String.valueOf(pos+1)); h.tvTeamName.setText(i.team_name); h.tvPoints.setText(String.valueOf(i.points)); }
        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { TextView tvRank, tvTeamName, tvPoints; ViewHolder(View v) { super(v); tvRank=v.findViewById(R.id.tvRank); tvTeamName=v.findViewById(R.id.tvTeamName); tvPoints=v.findViewById(R.id.tvPoints); } }
    }

    static class ScorerAdapter extends RecyclerView.Adapter<ScorerAdapter.ViewHolder> {
        List<ServerScorer> list;
        ScorerAdapter(List<ServerScorer> l) { list = l; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int v) { return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_scorer, p, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) { ServerScorer i = list.get(pos); h.n.setText(i.player_name); h.t.setText(i.team_name); h.g.setText(String.valueOf(i.goals)); h.a.setText(String.valueOf(i.assists)); }
        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { TextView n, t, g, a; ViewHolder(View v) { super(v); n=v.findViewById(R.id.tvPlayerName); t=v.findViewById(R.id.tvTeamShort); g=v.findViewById(R.id.tvGoals); a=v.findViewById(R.id.tvAssists); } }
    }
}