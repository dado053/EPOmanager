package com.example.epostats;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchCardActivity extends AppCompatActivity {
    private int matchId, homeTeamId, awayTeamId, currentSelectedTeamId;
    private Button btnHome;
    private Button btnAway;
    private RecyclerView recyclerView;
    private PlayerAdapter adapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_card);
        boolean isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        matchId = getIntent().getIntExtra("MATCH_ID", -1);
        homeTeamId = getIntent().getIntExtra("HOME_TEAM_ID", -1);
        awayTeamId = getIntent().getIntExtra("AWAY_TEAM_ID", -1);

        btnHome = findViewById(R.id.btnHomeTeam);
        btnAway = findViewById(R.id.btnAwayTeam);
        Button btnOpenStats = findViewById(R.id.btnOpenStats);
        Button btnEdit = findViewById(R.id.btnEdit);
        recyclerView = findViewById(R.id.recyclerViewPlayers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        apiService = RetrofitClient.getClient().create(ApiService.class);

        btnHome.setText(getIntent().getStringExtra("HOME_TEAM_NAME"));
        btnAway.setText(getIntent().getStringExtra("AWAY_TEAM_NAME"));

        btnHome.setOnClickListener(v -> {
            highlightButton(btnHome, btnAway);
            currentSelectedTeamId = homeTeamId;
            loadPlayers(homeTeamId);
        });

        btnAway.setOnClickListener(v -> {
            highlightButton(btnAway, btnHome);
            currentSelectedTeamId = awayTeamId;
            loadPlayers(awayTeamId);
        });

        btnOpenStats.setOnClickListener(v -> {
            Intent intent = new Intent(MatchCardActivity.this, StatsActivity.class);
            intent.putExtra("MATCH_ID", matchId);
            intent.putExtra("HOME_TEAM_ID", homeTeamId);
            intent.putExtra("AWAY_TEAM_ID", awayTeamId);
            intent.putExtra("HOME_TEAM_NAME", getIntent().getStringExtra("HOME_TEAM_NAME"));
            intent.putExtra("AWAY_TEAM_NAME", getIntent().getStringExtra("AWAY_TEAM_NAME"));
            startActivity(intent);
        });


        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(MatchCardActivity.this, RecordStatsActivity.class);
            intent.putExtra("MATCH_ID", matchId);
            intent.putExtra("HOME_TEAM_ID", homeTeamId);
            intent.putExtra("AWAY_TEAM_ID", awayTeamId);
            intent.putExtra("HOME_TEAM_NAME", getIntent().getStringExtra("HOME_TEAM_NAME"));
            intent.putExtra("AWAY_TEAM_NAME", getIntent().getStringExtra("AWAY_TEAM_NAME"));
            intent.putExtra("SELECTED_TEAM_ID", currentSelectedTeamId);
            startActivity(intent);
        });
        if (isAdmin) {
            btnEdit.setVisibility(View.VISIBLE);
        } else {
            btnEdit.setVisibility(View.GONE);
        }

        btnHome.performClick();
    }

    private void loadPlayers(int teamId) {
        apiService.getTeamPlayers(teamId).enqueue(new Callback<List<Player>>() {
            @Override
            public void onResponse(Call<List<Player>> call, Response<List<Player>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Player> players = response.body();
                    for (int i = 0; i < players.size(); i++) {
                        if (i < 11) players.get(i).setStartingEleven(true);
                    }
                    adapter = new PlayerAdapter(MatchCardActivity.this, players, matchId, teamId);
                    recyclerView.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<List<Player>> call, Throwable t) {}
        });
    }

    private void highlightButton(Button active, Button inactive) {
        active.setBackgroundColor(Color.parseColor("#1a73e8"));
        active.setTextColor(Color.WHITE);
        inactive.setBackgroundColor(Color.parseColor("#888888"));
        inactive.setTextColor(Color.WHITE);
    }
}