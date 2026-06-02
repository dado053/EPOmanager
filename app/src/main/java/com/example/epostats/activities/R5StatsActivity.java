package com.example.epostats.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.epostats.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class  R5StatsActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://nikos.alwaysdata.net/epo_project/";

    private android.widget.Spinner spinnerChampionships;
    private LinearLayout standingsContainer;
    private LinearLayout teamStatsContainer;
    private LinearLayout playerStatsContainer;
    private TextView titleChampionship;

    private R5ApiService apiService;
    private final List<Championship> championships = new ArrayList<>();
    private int selectedChampionshipId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_r5_stats);

        spinnerChampionships = findViewById(R.id.spinnerChampionships);
        standingsContainer = findViewById(R.id.standingsContainer);
        teamStatsContainer = findViewById(R.id.teamStatsContainer);
        playerStatsContainer = findViewById(R.id.playerStatsContainer);
        titleChampionship = findViewById(R.id.titleChampionship);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(R5ApiService.class);

        loadChampionships();
    }

    private void loadChampionships() {
        apiService.getChampionships().enqueue(new Callback<List<Championship>>() {
            @Override
            public void onResponse(Call<List<Championship>> call, Response<List<Championship>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showToast("Δεν φορτώθηκαν τα πρωταθλήματα.");
                    loadAllStats();
                    return;
                }

                championships.clear();
                championships.addAll(response.body());

                if (championships.isEmpty()) {
                    showToast("Δεν υπάρχουν πρωταθλήματα.");
                    loadAllStats();
                    return;
                }

                ArrayAdapter<Championship> adapter = new ArrayAdapter<>(
                        R5StatsActivity.this,
                        android.R.layout.simple_spinner_item,
                        championships
                );

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerChampionships.setAdapter(adapter);

                selectedChampionshipId = championships.get(0).id;
                titleChampionship.setText("Πρωτάθλημα: " + championships.get(0).name);

                spinnerChampionships.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Championship selected = championships.get(position);
                        selectedChampionshipId = selected.id;
                        titleChampionship.setText("Πρωτάθλημα: " + selected.name);
                        loadAllStats();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Championship>> call, Throwable t) {
                showToast("Σφάλμα σύνδεσης: " + t.getMessage());
                loadAllStats();
            }
        });
    }

    private void loadAllStats() {
        loadStandings();
        loadTeamStats();
        loadPlayerStats();
    }

    private void loadStandings() {
        standingsContainer.removeAllViews();
        addHeader(standingsContainer, "#  Ομάδα                 ΑΓ  Ν  Ι  Η  ΓΥ  ΓΚ  ΔΓ  Β");

        apiService.getStandings(selectedChampionshipId).enqueue(new Callback<List<Standing>>() {
            @Override
            public void onResponse(Call<List<Standing>> call, Response<List<Standing>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    addMessage(standingsContainer, "Δεν βρέθηκε βαθμολογία.");
                    return;
                }

                List<Standing> standings = response.body();

                if (standings.isEmpty()) {
                    addMessage(standingsContainer, "Δεν υπάρχουν δεδομένα βαθμολογίας.");
                    return;
                }

                for (int i = 0; i < standings.size(); i++) {
                    Standing s = standings.get(i);

                    String line = String.format(
                            "%2d. %-20s %2d %2d %2d %2d %3d %3d %3d %3d",
                            i + 1,
                            cut(s.team_name, 20),
                            s.played,
                            s.wins,
                            s.draws,
                            s.losses,
                            s.goals_for,
                            s.goals_against,
                            s.goal_difference,
                            s.points
                    );

                    addRow(standingsContainer, line);
                }
            }

            @Override
            public void onFailure(Call<List<Standing>> call, Throwable t) {
                addMessage(standingsContainer, "Σφάλμα: " + t.getMessage());
            }
        });
    }

    private void loadTeamStats() {
        teamStatsContainer.removeAllViews();
        addHeader(teamStatsContainer, "Ομάδα                 ΓΚ  ΣΟΥΤ  ΣΤΟΧ  ΠΑΣ  ΑΣ  ΦΑΟΥΛ  ΛΑΘ  ΚΟΡ  ΚΑΡΤ");

        apiService.getTeamStats(selectedChampionshipId).enqueue(new Callback<List<TeamStats>>() {
            @Override
            public void onResponse(Call<List<TeamStats>> call, Response<List<TeamStats>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    addMessage(teamStatsContainer, "Δεν βρέθηκαν ομαδικά στατιστικά.");
                    return;
                }

                List<TeamStats> stats = response.body();

                if (stats.isEmpty()) {
                    addMessage(teamStatsContainer, "Δεν υπάρχουν ομαδικά στατιστικά.");
                    return;
                }

                for (TeamStats s : stats) {
                    String line = String.format(
                            "%-20s %2d %5d %5d %4d %3d %6d %4d %4d %4d",
                            cut(s.team_name, 20),
                            s.goals,
                            s.shots,
                            s.shots_on_target,
                            s.passes,
                            s.assists,
                            s.fouls,
                            s.mistakes,
                            s.corners,
                            s.yellow_cards + s.red_cards
                    );

                    addRow(teamStatsContainer, line);
                }
            }

            @Override
            public void onFailure(Call<List<TeamStats>> call, Throwable t) {
                addMessage(teamStatsContainer, "Σφάλμα: " + t.getMessage());
            }
        });
    }

    private void loadPlayerStats() {
        playerStatsContainer.removeAllViews();
        addHeader(playerStatsContainer, "Παίκτης               Ομάδα          ΓΚ  ΑΣ  ΣΟΥΤ  ΠΑΣ  ΦΑΟΥΛ  ΛΑΘ  ΚΑΡΤ");

        apiService.getPlayerStats(selectedChampionshipId).enqueue(new Callback<List<PlayerStats>>() {
            @Override
            public void onResponse(Call<List<PlayerStats>> call, Response<List<PlayerStats>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    addMessage(playerStatsContainer, "Δεν βρέθηκαν ατομικά στατιστικά.");
                    return;
                }

                List<PlayerStats> stats = response.body();

                if (stats.isEmpty()) {
                    addMessage(playerStatsContainer, "Δεν υπάρχουν ατομικά στατιστικά.");
                    return;
                }

                for (PlayerStats s : stats) {
                    String line = String.format(
                            "%-20s %-13s %2d %3d %5d %4d %6d %4d %4d",
                            cut(s.player_name, 20),
                            cut(s.team_name, 13),
                            s.goals,
                            s.assists,
                            s.shots,
                            s.passes,
                            s.fouls,
                            s.mistakes,
                            s.yellow_cards + s.red_cards
                    );

                    addRow(playerStatsContainer, line);
                }
            }

            @Override
            public void onFailure(Call<List<PlayerStats>> call, Throwable t) {
                addMessage(playerStatsContainer, "Σφάλμα: " + t.getMessage());
            }
        });
    }

    private void addHeader(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tv.setTextSize(13);
        tv.setPadding(8, 10, 8, 10);
        container.addView(tv);
    }

    private void addRow(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextSize(13);
        tv.setPadding(8, 8, 8, 8);
        container.addView(tv);
    }

    private void addMessage(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setPadding(8, 8, 8, 8);
        container.addView(tv);
    }

    private String cut(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 1) + "…";
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    interface R5ApiService {
        @GET("api_get_championships.php")
        Call<List<Championship>> getChampionships();

        @GET("api_get_standings.php")
        Call<List<Standing>> getStandings(@Query("championship_id") int championshipId);

        @GET("api_get_team_stats.php")
        Call<List<TeamStats>> getTeamStats(@Query("championship_id") int championshipId);

        @GET("api_get_player_stats.php")
        Call<List<PlayerStats>> getPlayerStats(@Query("championship_id") int championshipId);
    }

    static class Championship {
        int id;
        String name;

        @Override
        public String toString() {
            return name;
        }
    }

    static class Standing {
        int team_id;
        String team_name;
        int played;
        int wins;
        int draws;
        int losses;
        int goals_for;
        int goals_against;
        int goal_difference;
        int points;
    }

    static class TeamStats {
        int team_id;
        String team_name;
        int goals;
        int shots;
        int shots_on_target;
        int passes;
        int assists;
        int fouls;
        int mistakes;
        int corners;
        int tackles;
        int crosses;
        int yellow_cards;
        int red_cards;
    }

    static class PlayerStats {
        int player_id;
        String player_name;
        int team_id;
        String team_name;
        int goals;
        int assists;
        int shots;
        int passes;
        int fouls;
        int mistakes;
        int yellow_cards;
        int red_cards;
    }
}