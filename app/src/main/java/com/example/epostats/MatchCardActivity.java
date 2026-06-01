package com.example.epostats;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.Comparator;
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

        // Γραφική απεικόνιση σε πλέγμα 2 στηλών
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

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

                    // 1. Ορίζουμε πρώτα ποιοι είναι βασικοί (οι πρώτοι 11 που έρχονται από τη βάση)
                    for (int i = 0; i < players.size(); i++) {
                        if (i < 11) {
                            players.get(i).setStartingEleven(true);
                        } else {
                            players.get(i).setStartingEleven(false);
                        }
                    }

                    // 2. ΤΑΞΙΝΟΜΗΣΗ (Βασικοί Πάνω -> Μετά Ταξινόμηση ανά Θέση)
                    Collections.sort(players, new Comparator<Player>() {
                        @Override
                        public int compare(Player p1, Player p2) {
                            // Πρώτος Κανόνας: Οι βασικοί μπαίνουν πάνω από τον πάγκο
                            if (p1.isStartingEleven() && !p2.isStartingEleven()) return -1;
                            if (!p1.isStartingEleven() && p2.isStartingEleven()) return 1;

                            // Δεύτερος Κανόνας: Αν είναι και οι δύο βασικοί (ή πάγκος), ταξινομούμε ανά θέση
                            return getPositionWeight(p1.getPosition()) - getPositionWeight(p2.getPosition());
                        }

                        // Μέθοδος που δίνει το "βάρος" στη θέση για τη σωστή σειρά
                        private int getPositionWeight(String position) {
                            if (position == null) return 5;
                            switch (position) {
                                case "Τερματοφύλακας": return 1;
                                case "Αμυντικός": return 2;
                                case "Μέσος": return 3;
                                case "Επιθετικός": return 4;
                                default: return 5;
                            }
                        }
                    });

                    // 3. Στέλνουμε την ταξινομημένη λίστα στον Adapter
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