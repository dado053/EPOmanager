package com.example.epostats;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsActivity extends AppCompatActivity {
    private int matchId, homeTeamId, awayTeamId;
    private TextView tvHomeName, tvAwayName, tvScore;
    private RecyclerView rvEvents;
    private ApiService apiService;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        // Λήψη δεδομένων από το Intent
        matchId = getIntent().getIntExtra("MATCH_ID", -1);
        homeTeamId = getIntent().getIntExtra("HOME_TEAM_ID", -1);
        awayTeamId = getIntent().getIntExtra("AWAY_TEAM_ID", -1);
        String homeName = getIntent().getStringExtra("HOME_TEAM_NAME");
        String awayName = getIntent().getStringExtra("AWAY_TEAM_NAME");

        tvHomeName = findViewById(R.id.tvHomeName);
        tvAwayName = findViewById(R.id.tvAwayName);
        tvScore = findViewById(R.id.tvScore);
        rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        tvHomeName.setText(homeName);
        tvAwayName.setText(awayName);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        loadMatchStats();

        //live ananeosi
        refreshRunnable = new Runnable() {
            @Override
            public void run() {

                loadMatchStats(); // re-fetch data
                handler.postDelayed(this, REFRESH_INTERVAL); // repeat
            }
        };

        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
    }
    private void loadMatchStats() {
        // Εμφάνιση Toast για να δούμε τι ID πήρε η Activity από το Intent
        //Toast.makeText(this, "Refreshing data...Ζητείται το Match ID: " + matchId, Toast.LENGTH_SHORT).show();

        apiService.getMatchEvents(matchId).enqueue(new Callback<List<MatchEvent>>() {
            @Override
            public void onResponse(Call<List<MatchEvent>> call, Response<List<MatchEvent>> response) {
                // 1. Έλεγχος αν ο server απάντησε με κωδικό επιτυχίας (200 OK)
                if (response.isSuccessful()) {
                    List<MatchEvent> events = response.body();

                    // 2. Έλεγχος αν η λίστα επέστρεψε άδεια από την PHP
                    if (events == null || events.isEmpty()) {
                        Toast.makeText(StatsActivity.this, "Ο server απάντησε, αλλά η λίστα events είναι ΑΔΕΙΑ για το ID: " + matchId, Toast.LENGTH_LONG).show();
                        tvScore.setText("0 - 0");
                        return;
                    }

                    int homeGoals = 0;
                    int awayGoals = 0;

                    for (MatchEvent event : events) {
                        if ("Γκολ".equalsIgnoreCase(event.getActionResult())) {
                            if (event.getTeamId() == homeTeamId) {
                                homeGoals++;
                            } else if (event.getTeamId() == awayTeamId) {
                                awayGoals++;
                            }
                        }
                    }

                    tvScore.setText(homeGoals + " - " + awayGoals);

                    EventAdapter adapter = new EventAdapter(events);
                    rvEvents.setAdapter(adapter);

                } else {
                    // 3. Αν ο server επέστρεψε σφάλμα (π.χ. 404, 500)
                    int statusCode = response.code();
                    Toast.makeText(StatsActivity.this, "Σφάλμα Server! Κωδικός: " + statusCode, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<MatchEvent>> call, Throwable t) {
                // 4. Αν απέτυχε πλήρως η σύνδεση (π.χ. λάθος URL, πρόβλημα Internet ή crash στο Gson)
                android.util.Log.e("RETROFIT_ERROR", "Αποτυχία: ", t);
                Toast.makeText(StatsActivity.this, "Αποτυχία σύνδεσης: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


}