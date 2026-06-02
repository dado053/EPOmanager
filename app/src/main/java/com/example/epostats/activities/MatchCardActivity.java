package com.example.epostats.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.epostats.R;
import com.example.epostats.models.Player;
import com.example.epostats.network.ApiService;
import com.example.epostats.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchCardActivity extends AppCompatActivity {
    private int matchId, homeTeamId, awayTeamId;
    private RecyclerView recyclerView;
    private ApiService apiService;

    // Λίστες για να κρατήσουμε τους παίκτες
    private List<Player> homePlayers = new ArrayList<>();
    private List<Player> awayPlayers = new ArrayList<>();
    private int callsCompleted = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_card);

        boolean isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        matchId = getIntent().getIntExtra("MATCH_ID", -1);
        homeTeamId = getIntent().getIntExtra("HOME_TEAM_ID", -1);
        awayTeamId = getIntent().getIntExtra("AWAY_TEAM_ID", -1);

        String homeName = getIntent().getStringExtra("HOME_TEAM_NAME");
        String awayName = getIntent().getStringExtra("AWAY_TEAM_NAME");

        TextView tvHomeTitle = findViewById(R.id.tvHomeTitle);
        TextView tvAwayTitle = findViewById(R.id.tvAwayTitle);
        tvHomeTitle.setText(homeName);
        tvAwayTitle.setText(awayName);

        Button btnEdit = findViewById(R.id.btnEdit);
        recyclerView = findViewById(R.id.recyclerViewPlayers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        apiService = RetrofitClient.getClient().create(ApiService.class);

        if (isAdmin) {
            btnEdit.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(MatchCardActivity.this, RecordStatsActivity.class);
                intent.putExtra("MATCH_ID", matchId);
                intent.putExtra("HOME_TEAM_ID", homeTeamId);
                intent.putExtra("AWAY_TEAM_ID", awayTeamId);
                intent.putExtra("HOME_TEAM_NAME", homeName);
                intent.putExtra("AWAY_TEAM_NAME", awayName);
                startActivity(intent);
            });
        }

        // Κατεβάζουμε ταυτόχρονα και τις δύο ομάδες
        fetchTeam(homeTeamId, true);
        fetchTeam(awayTeamId, false);
    }

    private void fetchTeam(int teamId, boolean isHome) {
        apiService.getTeamPlayers(teamId).enqueue(new Callback<List<Player>>() {
            @Override
            public void onResponse(Call<List<Player>> call, Response<List<Player>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Player> team = response.body();
                    // Ορίζουμε τους πρώτους 11 ως βασικούς
                    for (int i = 0; i < team.size(); i++) {
                        team.get(i).setStartingEleven(i < 11);
                    }
                    if (isHome) homePlayers.addAll(team);
                    else awayPlayers.addAll(team);
                }
                checkAndBuildLineup();
            }

            @Override
            public void onFailure(Call<List<Player>> call, Throwable t) {
                checkAndBuildLineup();
            }
        });
    }

    // Περιμένει να κατέβουν ΚΑΙ οι 2 ομάδες πριν φτιάξει τη λίστα
    private synchronized void checkAndBuildLineup() {
        callsCompleted++;
        if (callsCompleted == 2) {
            buildSideBySideList();
        }
    }

    private void buildSideBySideList() {
        List<Player> homeStarters = new ArrayList<>();
        List<Player> homeBench = new ArrayList<>();
        for (Player p : homePlayers) {
            if (p.isStartingEleven()) homeStarters.add(p);
            else homeBench.add(p);
        }

        List<Player> awayStarters = new ArrayList<>();
        List<Player> awayBench = new ArrayList<>();
        for (Player p : awayPlayers) {
            if (p.isStartingEleven()) awayStarters.add(p);
            else awayBench.add(p);
        }

        List<LineupRow> rows = new ArrayList<>();

        // --- ΒΑΣΙΚΟΙ ---
        LineupRow starterHeader = new LineupRow();
        starterHeader.isHeader = true;
        starterHeader.headerText = "ΒΑΣΙΚΟΙ";
        rows.add(starterHeader);

        int maxStarters = Math.max(homeStarters.size(), awayStarters.size());
        for (int i = 0; i < maxStarters; i++) {
            LineupRow row = new LineupRow();
            if (i < homeStarters.size()) row.homePlayer = homeStarters.get(i);
            if (i < awayStarters.size()) row.awayPlayer = awayStarters.get(i);
            rows.add(row);
        }

        // --- ΑΝΑΠΛΗΡΩΜΑΤΙΚΟΙ ---
        LineupRow benchHeader = new LineupRow();
        benchHeader.isHeader = true;
        benchHeader.headerText = "ΑΝΑΠΛΗΡΩΜΑΤΙΚΟΙ";
        rows.add(benchHeader);

        int maxBench = Math.max(homeBench.size(), awayBench.size());
        for (int i = 0; i < maxBench; i++) {
            LineupRow row = new LineupRow();
            if (i < homeBench.size()) row.homePlayer = homeBench.get(i);
            if (i < awayBench.size()) row.awayPlayer = awayBench.get(i);
            rows.add(row);
        }

        LineupAdapter adapter = new LineupAdapter(rows);
        recyclerView.setAdapter(adapter);
    }

    // Το αντικείμενο που κρατάει τη γραμμή (Γηπεδούχος - Φιλοξενούμενος)
    static class LineupRow {
        boolean isHeader = false;
        String headerText;
        Player homePlayer;
        Player awayPlayer;
    }

    // Ο εσωτερικός Adapter που ζωγραφίζει την οθόνη
    static class LineupAdapter extends RecyclerView.Adapter<LineupAdapter.ViewHolder> {
        private final List<LineupRow> rows;

        LineupAdapter(List<LineupRow> rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lineup_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LineupRow row = rows.get(position);

            if (row.isHeader) {
                holder.tvSectionHeader.setVisibility(View.VISIBLE);
                holder.tvSectionHeader.setText(row.headerText);
                holder.layoutPlayersRow.setVisibility(View.GONE);
            } else {
                holder.tvSectionHeader.setVisibility(View.GONE);
                holder.layoutPlayersRow.setVisibility(View.VISIBLE);

                if (row.homePlayer != null) {
                    holder.tvHomePlayer.setText(row.homePlayer.getName());
                } else {
                    holder.tvHomePlayer.setText("");
                }

                if (row.awayPlayer != null) {
                    holder.tvAwayPlayer.setText(row.awayPlayer.getName());
                } else {
                    holder.tvAwayPlayer.setText("");
                }
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSectionHeader, tvHomePlayer, tvAwayPlayer;
            View layoutPlayersRow;

            ViewHolder(View v) {
                super(v);
                tvSectionHeader = v.findViewById(R.id.tvSectionHeader);
                tvHomePlayer = v.findViewById(R.id.tvHomePlayer);
                tvAwayPlayer = v.findViewById(R.id.tvAwayPlayer);
                layoutPlayersRow = v.findViewById(R.id.layoutPlayersRow);
            }
        }
    }
}