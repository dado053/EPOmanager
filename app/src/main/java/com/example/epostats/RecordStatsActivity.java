package com.example.epostats;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecordStatsActivity extends AppCompatActivity {
    private EditText etMinute;
    private Spinner spinnerTeamSelection, spinnerActionType, spinnerActionDetail, spinnerActionResult, spinnerPlayer, spinnerAssistPlayer;
    private Button btnSubmit;
    private TextView tvHomeTeamName, tvAwayTeamName, tvScore;

    private int matchId, homeTeamId, awayTeamId;
    private String homeTeamName, awayTeamName;

    private int homeScore = 0;
    private int awayScore = 0;

    private List<String> allPlayerNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_stats);

        matchId = getIntent().getIntExtra("MATCH_ID", -1);
        homeTeamId = getIntent().getIntExtra("HOME_TEAM_ID", -1);
        awayTeamId = getIntent().getIntExtra("AWAY_TEAM_ID", -1);
        homeTeamName = getIntent().getStringExtra("HOME_TEAM_NAME");
        awayTeamName = getIntent().getStringExtra("AWAY_TEAM_NAME");

        tvHomeTeamName = findViewById(R.id.tvHomeTeamName);
        tvAwayTeamName = findViewById(R.id.tvAwayTeamName);
        tvScore = findViewById(R.id.tvScore);
        etMinute = findViewById(R.id.etMinute);
        spinnerTeamSelection = findViewById(R.id.spinnerTeamSelection);
        spinnerActionType = findViewById(R.id.spinnerActionType);
        spinnerActionDetail = findViewById(R.id.spinnerActionDetail);
        spinnerActionResult = findViewById(R.id.spinnerActionResult);
        spinnerPlayer = findViewById(R.id.spinnerPlayer);
        spinnerAssistPlayer = findViewById(R.id.spinnerAssistPlayer);
        btnSubmit = findViewById(R.id.btnSubmit);

        tvHomeTeamName.setText(homeTeamName);
        tvAwayTeamName.setText(awayTeamName);
        tvScore.setText(homeScore + " - " + awayScore);

        setupStaticSpinners();

        String[] teams = {"- Επιλέξτε Ομάδα -", homeTeamName, awayTeamName};
        ArrayAdapter<String> teamAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, teams);
        spinnerTeamSelection.setAdapter(teamAdapter);

        spinnerTeamSelection.setSelection(0, false);

        spinnerTeamSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    loadDynamicPlayers(homeTeamId);
                } else if (position == 2) {
                    loadDynamicPlayers(awayTeamId);
                } else {
                    allPlayerNames.clear();
                    allPlayerNames.add("- Επιλέξτε Παίκτη -");
                    ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(RecordStatsActivity.this, android.R.layout.simple_spinner_dropdown_item, allPlayerNames);
                    spinnerPlayer.setAdapter(emptyAdapter);
                    spinnerAssistPlayer.setAdapter(emptyAdapter);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerAssistPlayer.setEnabled(false);
        spinnerAssistPlayer.setAlpha(0.5f);

        spinnerActionResult.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getItemAtPosition(position).toString().equals("Γκολ")) {
                    spinnerAssistPlayer.setEnabled(true);
                    spinnerAssistPlayer.setAlpha(1.0f);
                    updateAssistSpinner();
                } else {
                    spinnerAssistPlayer.setEnabled(false);
                    spinnerAssistPlayer.setAlpha(0.5f);
                    spinnerAssistPlayer.setSelection(0);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerPlayer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerActionResult.getSelectedItem().toString().equals("Γκολ")) {
                    updateAssistSpinner();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSubmit.setOnClickListener(v -> sendDataToServer());
    }

    private void setupStaticSpinners() {
        String[] actions = {"-", "Σουτ", "Τάκλιν", "Πάσα", "Σέντρα", "Φάουλ", "Κόρνερ", "Πέναλτι", "Οφσάιντ", "Κλέψιμο", "Διώξιμο", "Μπλοκ"};
        String[] details = {"-", "Εντός Περιοχής", "Εκτός Περιοχής", "Κεφαλιά", "Δυνατό Σουτ", "Πλασέ", "Τετ-α-τετ", "Απευθείας Εκτέλεση", "Με Πάσα"};
        String[] results = {"-", "Γκολ", "Άουτ", "Απόκρουση Τερματοφύλακα", "Δοκάρι", "Κόκκινη κάρτα", "Κίτρινη κάρτα", "Επιτυχής", "Λάθος", "Κερδισμένο Κόρνερ"};

        spinnerActionType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, actions));
        spinnerActionDetail.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, details));
        spinnerActionResult.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, results));
    }

    private void loadDynamicPlayers(int teamId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getTeamPlayers(teamId).enqueue(new Callback<List<Player>>() {
            @Override
            public void onResponse(Call<List<Player>> call, Response<List<Player>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Player> players = response.body();

                    allPlayerNames.clear();
                    allPlayerNames.add("- Επιλέξτε Παίκτη -");

                    for (Player p : players) {
                        allPlayerNames.add(p.getName());
                    }

                    ArrayAdapter<String> playerAdapter = new ArrayAdapter<>(RecordStatsActivity.this, android.R.layout.simple_spinner_dropdown_item, allPlayerNames);
                    spinnerPlayer.setAdapter(playerAdapter);
                    updateAssistSpinner();
                }
            }
            @Override
            public void onFailure(Call<List<Player>> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }

    private void updateAssistSpinner() {
        if (allPlayerNames.isEmpty() || spinnerPlayer.getSelectedItem() == null) return;

        String currentScorer = spinnerPlayer.getSelectedItem().toString();
        String currentAssistSelection = "- Επιλέξτε Παίκτη -";
        if (spinnerAssistPlayer.getSelectedItem() != null) {
            currentAssistSelection = spinnerAssistPlayer.getSelectedItem().toString();
        }

        List<String> assistNames = new ArrayList<>();
        for (String name : allPlayerNames) {
            if (!name.equals(currentScorer) || name.equals("- Επιλέξτε Παίκτη -")) {
                assistNames.add(name);
            }
        }

        ArrayAdapter<String> assistAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, assistNames);
        spinnerAssistPlayer.setAdapter(assistAdapter);

        int position = assistNames.indexOf(currentAssistSelection);
        if (position >= 0) {
            spinnerAssistPlayer.setSelection(position);
        } else {
            spinnerAssistPlayer.setSelection(0);
        }
    }

    private void sendDataToServer() {
        if (spinnerTeamSelection.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Παρακαλώ επιλέξτε ομάδα!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (etMinute.getText().toString().trim().isEmpty()) {
            etMinute.setError("Το λεπτό είναι υποχρεωτικό!");
            etMinute.requestFocus();
            return;
        }

        if (spinnerActionResult.getSelectedItem().toString().equals("Γκολ") && spinnerPlayer.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Παρακαλώ επιλέξτε ποιος παίκτης σκόραρε!", Toast.LENGTH_LONG).show();
            return;
        }

        if (spinnerActionType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Παρακαλώ επιλέξτε είδος ενέργειας!", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://nikos.alwaysdata.net/api_add_event.php";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(this, "Επιτυχής Καταγραφή!", Toast.LENGTH_SHORT).show();

                    if (spinnerActionResult.getSelectedItem().toString().equals("Γκολ")) {
                        if (spinnerTeamSelection.getSelectedItemPosition() == 1) {
                            homeScore++;
                        } else if (spinnerTeamSelection.getSelectedItemPosition() == 2) {
                            awayScore++;
                        }
                        tvScore.setText(homeScore + " - " + awayScore);
                    }

                    resetForm();
                },
                error -> Toast.makeText(this, "Σφάλμα Δικτύου", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                int activeTeamId = (spinnerTeamSelection.getSelectedItemPosition() == 1) ? homeTeamId : awayTeamId;

                Map<String, String> params = new HashMap<>();
                params.put("event_minute", etMinute.getText().toString());
                params.put("action_type", spinnerActionType.getSelectedItem().toString());
                params.put("action_detail", spinnerActionDetail.getSelectedItem().toString());
                params.put("action_result", spinnerActionResult.getSelectedItem().toString());
                params.put("player_name", spinnerPlayer.getSelectedItem().toString());
                params.put("assist_player_name", spinnerAssistPlayer.getSelectedItem().toString());
                params.put("match_id", String.valueOf(matchId));
                params.put("team_id", String.valueOf(activeTeamId));
                return params;
            }
        };
        Volley.newRequestQueue(this).add(postRequest);
    }

    private void resetForm() {
        etMinute.setText("");
        spinnerTeamSelection.setSelection(0);
        spinnerActionType.setSelection(0);
        spinnerActionDetail.setSelection(0);
        spinnerActionResult.setSelection(0);
        spinnerPlayer.setSelection(0);
        spinnerAssistPlayer.setSelection(0);
    }
}