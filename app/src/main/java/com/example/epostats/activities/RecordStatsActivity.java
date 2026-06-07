package com.example.epostats.activities;

import android.graphics.Color;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.epostats.R;
import com.example.epostats.models.Player;
import com.example.epostats.models.MatchEvent;
import com.example.epostats.network.ApiService;
import com.example.epostats.network.ApiResponse;
import com.example.epostats.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecordStatsActivity extends AppCompatActivity {

    private TextView tvScore, tvMatchTimer, tvExtraTimer;
    private Button btnSelectHome, btnSelectAway, btnSubstitution;
    private LinearLayout containerDynamicButtons, layoutSummaryChips;

    private int matchId, homeTeamId, awayTeamId;
    private String homeTeamName, awayTeamName;
    private int homeScore = 0, awayScore = 0;

    // States
    private int selectedTeamId = -1;
    private String selectedTeamName = "";
    private List<Player> cachedPlayers = new ArrayList<>();

    private String selectedPosition = "";
    private Player selectedPlayer = null;
    private String selectedActionType = "";
    private String subActionContext = "";

    // Substitution States
    private boolean isSubstitutionMode = false;
    private boolean isChoosingSubIn = true;
    private Player playerSubIn = null;

    // Timer / Injury Time Variables
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable updateTimerThread;
    private long globalStartTimeMillis = 0L;
    private boolean isTimerRunning = false;

    private int injury1stHalf = 0;
    private int injury2ndHalf = 0;
    private boolean is1stHalfInjuryPrompted = false;
    private boolean is2ndHalfInjuryPrompted = false;
    private String currentFormattedMinuteString = "1";
    private boolean isMatchFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_stats);

        matchId = getIntent().getIntExtra("MATCH_ID", -1);
        homeTeamId = getIntent().getIntExtra("HOME_TEAM_ID", -1);
        awayTeamId = getIntent().getIntExtra("AWAY_TEAM_ID", -1);
        homeTeamName = getIntent().getStringExtra("HOME_TEAM_NAME");
        awayTeamName = getIntent().getStringExtra("AWAY_TEAM_NAME");

        tvScore = findViewById(R.id.tvScore);
        tvMatchTimer = findViewById(R.id.tvMatchTimer);
        tvExtraTimer = findViewById(R.id.tvExtraTimer);
        btnSelectHome = findViewById(R.id.btnSelectHome);
        btnSelectAway = findViewById(R.id.btnSelectAway);
        btnSubstitution = findViewById(R.id.btnSubstitution);
        containerDynamicButtons = findViewById(R.id.containerDynamicButtons);
        layoutSummaryChips = findViewById(R.id.layoutSummaryChips);

        btnSelectHome.setText(homeTeamName);
        btnSelectAway.setText(awayTeamName);

        SharedPreferences prefs = getSharedPreferences("MatchData_" + matchId, MODE_PRIVATE);
        homeScore = prefs.getInt("home_score", 0);
        awayScore = prefs.getInt("away_score", 0);
        tvScore.setText(homeScore + " - " + awayScore);

        globalStartTimeMillis = prefs.getLong("global_start_time", 0L);
        injury1stHalf = prefs.getInt("injury_1st_half", 0);
        injury2ndHalf = prefs.getInt("injury_2nd_half", 0);

        updateTimerThread = new Runnable() {
            public void run() {
                if (globalStartTimeMillis > 0 && !isMatchFinished) {
                    long elapsedMillis = System.currentTimeMillis() - globalStartTimeMillis;
                    int totalSecs = (int) (elapsedMillis / 1000);
                    int totalMins = totalSecs / 60;
                    int secs = totalSecs % 60;

                    // Α' ΗΜΙΧΡΟΝΟ (00:00 - 45:00)
                    if (totalMins < 45) {
                        tvMatchTimer.setText(String.format("%02d:%02d", totalMins, secs));
                        tvExtraTimer.setVisibility(View.GONE);
                        currentFormattedMinuteString = String.valueOf(totalMins + 1);
                    }
                    // ΚΑΘΥΣΤΕΡΗΣΕΙΣ Α' ΗΜΙΧΡΟΝΟΥ
                    else if (totalMins >= 45 && totalMins < 45 + injury1stHalf) {
                        tvMatchTimer.setText("45:00");
                        tvExtraTimer.setVisibility(View.VISIBLE);
                        int extraSecs = totalSecs - (45 * 60);
                        int extraMins = extraSecs / 60;
                        int extraSecsPart = extraSecs % 60;
                        tvExtraTimer.setText(String.format("+%02d:%02d", extraMins, extraSecsPart));
                        currentFormattedMinuteString = "45+" + (extraMins + 1);
                    }
                    // Β' ΗΜΙΧΡΟΝΟ (45:00 - 90:00 LINEAR)
                    else if (totalMins >= 45 + injury1stHalf && totalMins < 90 + injury1stHalf) {
                        tvExtraTimer.setVisibility(View.GONE);
                        int adjustedMins = totalMins - injury1stHalf;
                        tvMatchTimer.setText(String.format("%02d:%02d", adjustedMins, secs));
                        currentFormattedMinuteString = String.valueOf(adjustedMins + 1);
                    }
                    // ΚΑΘΥΣΤΕΡΗΣΕΙΣ Β' ΗΜΙΧΡΟΝΟΥ ΚΑΙ ΛΗΞΗ
                    else {
                        int finalLimitMinutes = 90 + injury1stHalf + injury2ndHalf;

                        if (totalMins >= finalLimitMinutes) {
                            isMatchFinished = true;
                            tvMatchTimer.setText("90:00");
                            tvExtraTimer.setText(" (ΛΗΞΗ)");
                            tvExtraTimer.setTextColor(Color.parseColor("#E53935"));
                            lockMatchDueToFullTime();
                            return;
                        }

                        tvMatchTimer.setText("90:00");
                        tvExtraTimer.setVisibility(View.VISIBLE);
                        int extraSecs = totalSecs - ((90 + injury1stHalf) * 60);
                        int extraMins = extraSecs / 60;
                        int extraSecsPart = extraSecs % 60;
                        tvExtraTimer.setText(String.format("+%02d:%02d", extraMins, extraSecsPart));
                        currentFormattedMinuteString = "90+" + (extraMins + 1);

                        if (totalMins == 90 + injury1stHalf && secs == 0 && !is2ndHalfInjuryPrompted) {
                            is2ndHalfInjuryPrompted = true;
                            showInjuryTimeInputDialog(2);
                        }
                    }

                    if (totalMins == 45 && secs == 0 && !is1stHalfInjuryPrompted) {
                        is1stHalfInjuryPrompted = true;
                        showInjuryTimeInputDialog(1);
                    }
                }
                timerHandler.postDelayed(this, 1000);
            }
        };

        if (globalStartTimeMillis > 0L) {
            timerHandler.postDelayed(updateTimerThread, 0);
            isTimerRunning = true;
        }

        syncMatchDataFromServer();
        updateSummaryBar();

        btnSelectHome.setOnClickListener(v -> selectTeam(homeTeamId, true));
        btnSelectAway.setOnClickListener(v -> selectTeam(awayTeamId, false));
        btnSubstitution.setOnClickListener(v -> startSubstitutionFlow());
    }

    private void showInjuryTimeInputDialog(final int half) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(half == 1 ? "Καθυστερήσεις 1ου Ημιχρόνου" : "Καθυστερήσεις 2ου Ημιχρόνου");
        builder.setMessage("Ορίστε τα λεπτά των καθυστερήσεων (Βάλε 0 για άμεση λήξη):");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("π.χ. 4, 8, 10");
        builder.setView(input);

        builder.setPositiveButton("Ορισμός", (dialog, which) -> {
            String value = input.getText().toString().trim();
            int mins = 0;
            if (!value.isEmpty()) {
                try { mins = Integer.parseInt(value); } catch (Exception ignored) {}
            }

            SharedPreferences prefs = getSharedPreferences("MatchData_" + matchId, MODE_PRIVATE);
            if (half == 1) {
                injury1stHalf = mins;
                prefs.edit().putInt("injury_1st_half", injury1stHalf).apply();
            } else {
                injury2ndHalf = mins;
                prefs.edit().putInt("injury_2nd_half", injury2ndHalf).apply();

                if (injury2ndHalf == 0) {
                    isMatchFinished = true;
                    tvMatchTimer.setText("90:00");
                    tvExtraTimer.setText(" (ΛΗΞΗ)");
                    tvExtraTimer.setVisibility(View.VISIBLE);
                    lockMatchDueToFullTime();
                }
            }

            ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
            apiService.updateInjuryTime(matchId, injury1stHalf, injury2ndHalf).enqueue(new Callback<ApiResponse>() {
                @Override public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {}
                @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
            });

            Toast.makeText(RecordStatsActivity.this, "Ορίστηκαν +" + mins + " λεπτά καθυστερήσεων!", Toast.LENGTH_SHORT).show();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void lockMatchDueToFullTime() {
        containerDynamicButtons.removeAllViews();
        layoutSummaryChips.removeAllViews();
        btnSubstitution.setVisibility(View.GONE);
        btnSelectHome.setEnabled(false);
        btnSelectAway.setEnabled(false);

        TextView tvEnded = new TextView(this);
        tvEnded.setText("🏁 Ο ΑΓΩΝΑΣ ΕΛΗΞΕ\nΔεν επιτρέπονται άλλες καταγραφές στατιστικών.");
        tvEnded.setTextColor(Color.parseColor("#E53935"));
        tvEnded.setTextSize(18);
        tvEnded.setGravity(android.view.Gravity.CENTER);
        tvEnded.setPadding(32, 64, 32, 32);
        tvEnded.setTypeface(null, android.graphics.Typeface.BOLD);

        containerDynamicButtons.addView(tvEnded);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.addMatchEvent(matchId, 0, "90", "Λήξη Αγώνα", "Τελικό Σφύριγμα Διαιτητή", "Τέλος", 0)
                .enqueue(new Callback<ApiResponse>() {
                    @Override public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {}
                    @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
                });
    }

    private void syncMatchDataFromServer() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getMatchEvents(matchId).enqueue(new Callback<List<MatchEvent>>() {
            @Override
            public void onResponse(Call<List<MatchEvent>> call, Response<List<MatchEvent>> response) {
                SharedPreferences prefs = getSharedPreferences("MatchData_" + matchId, MODE_PRIVATE);

                if (response.isSuccessful() && (response.body() == null || response.body().isEmpty())) {
                    homeScore = 0; awayScore = 0; globalStartTimeMillis = 0L; injury1stHalf = 0; injury2ndHalf = 0; isMatchFinished = false;
                    is1stHalfInjuryPrompted = false; is2ndHalfInjuryPrompted = false;
                    if (isTimerRunning) { timerHandler.removeCallbacks(updateTimerThread); isTimerRunning = false; }
                    tvScore.setText("0 - 0"); tvMatchTimer.setText("00:00"); tvExtraTimer.setVisibility(View.GONE);
                    prefs.edit().clear().apply();
                    return;
                }

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<MatchEvent> events = response.body();
                    int computedHomeGoals = 0; int computedAwayGoals = 0;
                    for (MatchEvent e : events) {
                        if ("Γκολ".equalsIgnoreCase(e.getActionResult())) {
                            if (e.getTeamId() == homeTeamId) computedHomeGoals++; else computedAwayGoals++;
                        }
                        if ("Λήξη Αγώνα".equalsIgnoreCase(e.getActionType())) {
                            isMatchFinished = true;
                            tvMatchTimer.setText("90:00");
                            tvExtraTimer.setText(" (ΛΗΞΗ)");
                            tvExtraTimer.setVisibility(View.VISIBLE);
                            lockMatchDueToFullTime();
                        }
                    }
                    if (!isMatchFinished) {
                        homeScore = computedHomeGoals; awayScore = computedAwayGoals;
                        tvScore.setText(homeScore + " - " + awayScore);
                        prefs.edit().putInt("home_score", homeScore).putInt("away_score", awayScore).apply();

                        long serverTime = events.get(0).getStartTime();
                        if (serverTime > 0) {
                            if (Math.abs(globalStartTimeMillis - serverTime) > 2000) {
                                globalStartTimeMillis = serverTime;
                                prefs.edit().putLong("global_start_time", globalStartTimeMillis).apply();
                                if (!isTimerRunning) {
                                    timerHandler.removeCallbacks(updateTimerThread);
                                    timerHandler.postDelayed(updateTimerThread, 0);
                                    isTimerRunning = true;
                                }
                            }
                        }
                    }
                }
            }
            @Override public void onFailure(Call<List<MatchEvent>> call, Throwable t) {}
        });
    }

    private void selectTeam(int teamId, boolean isHome) {
        if (isMatchFinished) return;

        if (!isTimerRunning && globalStartTimeMillis == 0) {
            globalStartTimeMillis = System.currentTimeMillis();
            SharedPreferences prefs = getSharedPreferences("MatchData_" + matchId, MODE_PRIVATE);
            prefs.edit().putLong("global_start_time", globalStartTimeMillis).apply();

            ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
            apiService.startMatchTimer(matchId, globalStartTimeMillis).enqueue(new Callback<ApiResponse>() {
                @Override public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {}
                @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
            });
            timerHandler.postDelayed(updateTimerThread, 0);
            isTimerRunning = true;
        }

        selectedTeamId = teamId;
        selectedTeamName = isHome ? homeTeamName : awayTeamName;

        selectedPosition = ""; selectedPlayer = null; selectedActionType = ""; subActionContext = ""; isSubstitutionMode = false;

        btnSelectHome.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isHome ? "#E53935" : "#0A2230")));
        btnSelectAway.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(!isHome ? "#E53935" : "#0A2230")));

        btnSubstitution.setVisibility(View.VISIBLE);
        btnSubstitution.setText("🔄 ΠΡΑΓΜΑΤΟΠΟΙΗΣΗ ΑΛΛΑΓΗΣ (" + selectedTeamName + ")");

        updateSummaryBar();

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getTeamPlayers(teamId).enqueue(new Callback<List<Player>>() {
            @Override
            public void onResponse(Call<List<Player>> call, Response<List<Player>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedPlayers = response.body();
                    renderPositionsState();
                }
            }
            @Override public void onFailure(Call<List<Player>> call, Throwable t) {}
        });
    }

    private void renderPositionsState() {
        if (isMatchFinished) return;
        containerDynamicButtons.removeAllViews(); subActionContext = ""; updateSummaryBar();
        String[] positions = {"ΤΕΡΜΑΤΟΦΥΛΑΚΕΣ", "ΑΜΥΝΤΙΚΟΙ", "ΜΕΣΟΙ", "ΕΠΙΘΕΤΙΚΟΙ"};
        for (String pos : positions) {
            Button btn = createStandardButton(pos, "#0A2230");
            btn.setOnClickListener(v -> { selectedPosition = pos; renderPlayersState(); });
            containerDynamicButtons.addView(btn);
        }
    }

    private void renderPlayersState() {
        containerDynamicButtons.removeAllViews(); updateSummaryBar();
        int addedCount = 0;
        if (isSubstitutionMode) {
            if (isChoosingSubIn) {
                for (int i = 11; i < cachedPlayers.size(); i++) {
                    Player p = cachedPlayers.get(i);
                    if (belongsToPosition(p, selectedPosition)) {
                        Button btnP = createStandardButton("[ΑΝΑΠΛ] " + p.getName(), "#163344");
                        btnP.setOnClickListener(v -> { playerSubIn = p; isChoosingSubIn = false; selectedPosition = ""; renderPositionsState(); });
                        containerDynamicButtons.addView(btnP); addedCount++;
                    }
                }
            } else {
                int limit = Math.min(cachedPlayers.size(), 11);
                for (int i = 0; i < limit; i++) {
                    Player p = cachedPlayers.get(i);
                    if (belongsToPosition(p, selectedPosition)) {
                        Button btnP = createStandardButton("[ΒΑΣΙΚΟΣ] " + p.getName(), "#163344");
                        btnP.setOnClickListener(v -> executeSubstitution(p));
                        containerDynamicButtons.addView(btnP); addedCount++;
                    }
                }
            }
        } else {
            int limit = Math.min(cachedPlayers.size(), 11);
            for (int i = 0; i < limit; i++) {
                Player p = cachedPlayers.get(i);
                if (belongsToPosition(p, selectedPosition)) {
                    Button btnP = createStandardButton(p.getName(), "#163344");
                    btnP.setOnClickListener(v -> { selectedPlayer = p; renderActionsState(); });
                    containerDynamicButtons.addView(btnP); addedCount++;
                }
            }
        }
        if (addedCount == 0) {
            TextView tv = new TextView(this); tv.setText("Δεν βρέθηκαν παίκτες."); tv.setTextColor(Color.GRAY); tv.setPadding(16, 16, 16, 16); containerDynamicButtons.addView(tv);
        }
    }

    private void renderActionsState() {
        containerDynamicButtons.removeAllViews(); subActionContext = ""; updateSummaryBar();
        String[] actions = {"Σουτ", "Πάσα", "Τάκλιν", "Σέντρα", "Φάουλ", "Κόρνερ", "Πέναλτι"};
        for (String action : actions) {
            Button btnA = createStandardButton(action, "#0A2230");
            btnA.setOnClickListener(v -> { selectedActionType = action; renderResultsState(); });
            containerDynamicButtons.addView(btnA);
        }
    }

    private void renderResultsState() {
        containerDynamicButtons.removeAllViews(); updateSummaryBar();
        switch (selectedActionType) {
            case "Σουτ": renderShotOptions(); break;
            case "Πάσα":
            case "Σέντρα": renderPassAndCrossFlow(); break;
            case "Τάκλιν": renderTackleOptions(); break;
            case "Κόρνερ": renderCornerOptions(); break;
            case "Πέναλτι": renderPenaltyOptions(); break;
            case "Φάουλ": renderFoulSubOptions(); break;
        }
    }

    private void renderShotOptions() {
        String[] results = {"Γκολ", "Άουτ", "Δοκάρι", "Απόκρουση"};
        for (String result : results) {
            Button btn = createStandardButton(result, result.equals("Γκολ") ? "#2E7D32" : "#163344");
            btn.setOnClickListener(v -> sendLiveEvent(result)); containerDynamicButtons.addView(btn);
        }
    }

    private void renderPassAndCrossFlow() {
        Button btnBad = createStandardButton("❌ Άστοχη " + selectedActionType, "#E53935");
        btnBad.setOnClickListener(v -> sendLiveEvent("Άστοχη")); containerDynamicButtons.addView(btnBad);

        TextView tvLabel = new TextView(this); tvLabel.setText("➔ Επιλέξτε παίκτη για υποδοχή μπάλας:"); tvLabel.setTextColor(Color.parseColor("#85A2B6")); tvLabel.setTextSize(14); tvLabel.setPadding(8, 16, 8, 12); containerDynamicButtons.addView(tvLabel);

        int limit = Math.min(cachedPlayers.size(), 11);
        for (int i = 0; i < limit; i++) {
            Player receiver = cachedPlayers.get(i);
            if (receiver.getId() != selectedPlayer.getId()) {
                Button btnReceiver = createStandardButton(receiver.getName(), "#163344");
                btnReceiver.setOnClickListener(v -> {
                    if (selectedActionType.equals("Πάσα")) renderPostReceiverOptions(receiver);
                    else renderPostCrossReceiverOptions(receiver);
                });
                containerDynamicButtons.addView(btnReceiver);
            }
        }
    }

    private void renderPostReceiverOptions(Player receiver) {
        containerDynamicButtons.removeAllViews(); subActionContext = "Υποδοχή: " + receiver.getName(); updateSummaryBar();
        TextView label = new TextView(this); label.setText("➔ Επιλέξτε την επόμενη ενέργεια του " + receiver.getName() + ":"); label.setTextColor(Color.parseColor("#85A2B6")); label.setTextSize(14); label.setPadding(8, 16, 8, 12); containerDynamicButtons.addView(label);

        Button btnShot = createStandardButton("👟 Σουτ", "#163344"); btnShot.setOnClickListener(v -> sendPassChainThenRedirect(receiver, "Σουτ")); containerDynamicButtons.addView(btnShot);
        Button btnPass = createStandardButton("➔ Πάσα", "#163344"); btnPass.setOnClickListener(v -> sendPassChainThenRedirect(receiver, "Πάσα")); containerDynamicButtons.addView(btnPass);
        Button btnCross = createStandardButton("📐 Σέντρα", "#163344"); btnCross.setOnClickListener(v -> sendPassChainThenRedirect(receiver, "Σέντρα")); containerDynamicButtons.addView(btnCross);
        Button btnFoul = createStandardButton("⚠️ Φάουλ", "#163344"); btnFoul.setOnClickListener(v -> sendPassChainThenRedirect(receiver, "Φάουλ")); containerDynamicButtons.addView(btnFoul);
    }

    private void sendPassChainThenRedirect(Player receiver, String nextAction) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        String detailText = "Πάσα στον " + receiver.getName();
        apiService.addMatchEvent(matchId, selectedTeamId, currentFormattedMinuteString, "Πάσα", detailText, "Επιτυχής", selectedPlayer.getId())
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (response.isSuccessful()) { selectedPlayer = receiver; selectedActionType = nextAction; subActionContext = ""; renderResultsState(); }
                    }
                    @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
                });
    }

    private void renderPostCrossReceiverOptions(Player receiver) {
        containerDynamicButtons.removeAllViews(); subActionContext = "Υποδοχή Σέντρας: " + receiver.getName(); updateSummaryBar();

        Button btnHeader = createStandardButton("👤 Κεφαλιά", "#163344"); btnHeader.setOnClickListener(v -> renderCrossFinishingMatrix(receiver, "Κεφαλιά")); containerDynamicButtons.addView(btnHeader);
        Button btnShot = createStandardButton("👟 Σουτ", "#163344"); btnShot.setOnClickListener(v -> renderCrossFinishingMatrix(receiver, "Σουτ")); containerDynamicButtons.addView(btnShot);
        Button btnPass = createStandardButton("➔ Πάσα", "#163344"); btnPass.setOnClickListener(v -> sendCrossChainThenRedirect(receiver, "Πάσα")); containerDynamicButtons.addView(btnPass);
        Button btnCross = createStandardButton("📐 Σέντρα", "#163344"); btnCross.setOnClickListener(v -> sendCrossChainThenRedirect(receiver, "Σέντρα")); containerDynamicButtons.addView(btnCross);
        Button btnStolen = createStandardButton("🛡️ Κλέψιμο μπάλας (Άμυνα)", "#0A2230"); btnStolen.setOnClickListener(v -> sendCrossFinalEvent(receiver, "Χάσιμο μπάλας", "Κλέψιμο μπάλας")); containerDynamicButtons.addView(btnStolen);
    }

    private void renderCrossFinishingMatrix(Player receiver, String style) {
        containerDynamicButtons.removeAllViews(); subActionContext = style + " (" + receiver.getName() + ")"; updateSummaryBar();
        String[] outcomes = {"Γκολ", "Άουτ", "Δοκάρι", "Απόκρουση"};
        for (String out : outcomes) {
            Button btn = createStandardButton(out, out.equals("Γκολ") ? "#2E7D32" : "#163344");
            btn.setOnClickListener(v -> sendCrossFinalEvent(receiver, style, out)); containerDynamicButtons.addView(btn);
        }
    }

    private void sendCrossFinalEvent(Player receiver, String style, String outcome) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        String detailText = "Σέντρα ➔ " + style + " από τον " + receiver.getName();
        apiService.addMatchEvent(matchId, selectedTeamId, currentFormattedMinuteString, "Σέντρα", detailText, outcome, receiver.getId())
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (response.isSuccessful()) { Toast.makeText(RecordStatsActivity.this, "Καταγράφηκε!", Toast.LENGTH_SHORT).show(); if (outcome.equals("Γκολ")) updateScoreboard(); resetToInitialState(); }
                    }
                    @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
                });
    }

    private void sendCrossChainThenRedirect(Player receiver, String nextAction) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        String detailText = "Σέντρα στον " + receiver.getName();
        apiService.addMatchEvent(matchId, selectedTeamId, currentFormattedMinuteString, "Σέντρα", detailText, "Επιτυχής", selectedPlayer.getId())
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (response.isSuccessful()) { selectedPlayer = receiver; selectedActionType = nextAction; subActionContext = ""; renderResultsState(); }
                    }
                    @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
                });
    }

    private void renderTackleOptions() {
        Button btnSteal = createStandardButton("🛡️ Κλέψιμο μπάλας", "#2E7D32"); btnSteal.setOnClickListener(v -> sendLiveEvent("Κλέψιμο μπάλας")); containerDynamicButtons.addView(btnSteal);
        Button btnFoul = createStandardButton("⚠️ Φάουλ", "#163344"); btnFoul.setOnClickListener(v -> { subActionContext = "Φάουλ"; renderFoulSubOptions(); }); containerDynamicButtons.addView(btnFoul);
    }

    private void renderFoulSubOptions() {
        containerDynamicButtons.removeAllViews(); updateSummaryBar();
        Button btnNormal = createStandardButton("Απλό Φάουλ", "#163344"); btnNormal.setOnClickListener(v -> sendLiveEvent("Απλό Φάουλ")); containerDynamicButtons.addView(btnNormal);
        Button btnYellow = createStandardButton("Κίτρινη κάρτα", "#163344"); btnYellow.setOnClickListener(v -> sendLiveEvent("Κίτρινη κάρτα")); containerDynamicButtons.addView(btnYellow);
        Button btnRed = createStandardButton("Κόκκινη κάρτα", "#163344"); btnRed.setOnClickListener(v -> sendLiveEvent("Κόκκινη κάρτα")); containerDynamicButtons.addView(btnRed);
    }

    private void renderCornerOptions() {
        Button btnLongCorner = createStandardButton("📐 Σέντρα (Μακρινό Κόρνερ)", "#163344"); btnLongCorner.setOnClickListener(v -> renderLongCornerFlow()); containerDynamicButtons.addView(btnLongCorner);
        Button btnShortCorner = createStandardButton("📐 Πάσα (Κοντινό Κόρνερ)", "#163344"); btnShortCorner.setOnClickListener(v -> renderShortCornerFlow()); containerDynamicButtons.addView(btnShortCorner);
    }

    private void renderLongCornerFlow() {
        containerDynamicButtons.removeAllViews(); subActionContext = "Σέντρα"; updateSummaryBar();
        Button btnBad = createStandardButton("❌ Άστοχη Σέντρα Κόρνερ", "#E53935"); btnBad.setOnClickListener(v -> sendLiveEvent("Άστοχη")); containerDynamicButtons.addView(btnBad);

        TextView tvLabel = new TextView(this); tvLabel.setText("➔ Επιλέξτε παίκτη για υποδοχή της σέντρας:"); tvLabel.setTextColor(Color.parseColor("#85A2B6")); tvLabel.setTextSize(14); containerDynamicButtons.addView(tvLabel);

        int limit = Math.min(cachedPlayers.size(), 11);
        for (int i = 0; i < limit; i++) {
            Player receiver = cachedPlayers.get(i);
            if (receiver.getId() != selectedPlayer.getId()) {
                Button btnReceiver = createStandardButton(receiver.getName(), "#163344"); btnReceiver.setOnClickListener(v -> renderLongCornerPlayerChoices(receiver)); containerDynamicButtons.addView(btnReceiver);
            }
        }
    }

    private void renderLongCornerPlayerChoices(Player receiver) {
        containerDynamicButtons.removeAllViews(); subActionContext = "Σέντρα ➔ " + receiver.getName(); updateSummaryBar();
        Button btnHeader = createStandardButton("👤 Κεφαλιά", "#163344"); btnHeader.setOnClickListener(v -> renderLongCornerFinishing(receiver, "Κεφαλιά")); containerDynamicButtons.addView(btnHeader);
        Button btnShot = createStandardButton("👟 Σουτ", "#163344"); btnShot.setOnClickListener(v -> renderLongCornerFinishing(receiver, "Σουτ")); containerDynamicButtons.addView(btnShot);
    }

    private void renderLongCornerFinishing(Player receiver, String actionStyle) {
        containerDynamicButtons.removeAllViews(); subActionContext = actionStyle + " (" + receiver.getName() + ")"; updateSummaryBar();
        String[] outcomes = {"Γκολ", "Άουτ", "Δοκάρι", "Απόκρουση"};
        for (String out : outcomes) {
            Button btn = createStandardButton(out, out.equals("Γκολ") ? "#2E7D32" : "#163344"); btn.setOnClickListener(v -> sendLongCornerFinalEvent(receiver, actionStyle, out)); containerDynamicButtons.addView(btn);
        }
    }

    private void sendLongCornerFinalEvent(Player receiver, String style, String outcome) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        String detailText = "Κόρνερ (Σέντρα) ➔ " + style + " από τον " + receiver.getName();
        apiService.addMatchEvent(matchId, selectedTeamId, currentFormattedMinuteString, "Κόρνερ (Σέντρα)", detailText, outcome, receiver.getId())
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (response.isSuccessful()) { if (outcome.equals("Γκολ")) updateScoreboard(); resetToInitialState(); }
                    }
                    @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
                });
    }

    private void renderShortCornerFlow() {
        containerDynamicButtons.removeAllViews(); subActionContext = "Πάσα"; updateSummaryBar();
        Button btnBad = createStandardButton("❌ Άστοχη Πάσα Κόρνερ", "#E53935"); btnBad.setOnClickListener(v -> sendLiveEvent("Άστοχη")); containerDynamicButtons.addView(btnBad);

        TextView tvLabel = new TextView(this); tvLabel.setText("➔ Επιλέξτε παίκτη για κοντινή υποδοχή:"); tvLabel.setTextColor(Color.parseColor("#85A2B6")); tvLabel.setTextSize(14); containerDynamicButtons.addView(tvLabel);

        int limit = Math.min(cachedPlayers.size(), 11);
        for (int i = 0; i < limit; i++) {
            Player receiver = cachedPlayers.get(i);
            if (receiver.getId() != selectedPlayer.getId()) {
                Button btnReceiver = createStandardButton(receiver.getName(), "#163344"); btnReceiver.setOnClickListener(v -> renderShortCornerPostReceiverOptions(receiver)); containerDynamicButtons.addView(btnReceiver);
            }
        }
    }

    private void renderShortCornerPostReceiverOptions(Player receiver) {
        containerDynamicButtons.removeAllViews(); subActionContext = "Υποδοχή: " + receiver.getName(); updateSummaryBar();

        Button btnShot = createStandardButton("👟 Σουτ", "#163344");
        btnShot.setOnClickListener(v -> {
            subActionContext = "Υποδοχή: " + receiver.getName() + " ➔ Σουτ"; containerDynamicButtons.removeAllViews(); updateSummaryBar();
            String[] shotResults = {"Γκολ", "Άουτ", "Δοκάρι", "Απόκρουση"};
            for (String res : shotResults) {
                Button btn = createStandardButton(res, res.equals("Γκολ") ? "#2E7D32" : "#163344"); btn.setOnClickListener(view -> sendShortCornerCustomFinalEvent(receiver, "Σουτ", res)); containerDynamicButtons.addView(btn);
            }
        });
        containerDynamicButtons.addView(btnShot);

        Button btnPass = createStandardButton("➔ Πάσα", "#163344");
        btnPass.setOnClickListener(v -> {
            subActionContext = "Υποδοχή: " + receiver.getName() + " ➔ Πάσα"; containerDynamicButtons.removeAllViews(); updateSummaryBar();
            int listLimit = Math.min(cachedPlayers.size(), 11);
            for (int i = 0; i < listLimit; i++) {
                Player innerReceiver = cachedPlayers.get(i);
                if (innerReceiver.getId() != receiver.getId()) {
                    Button btnInner = createStandardButton(innerReceiver.getName(), "#163344"); btnInner.setOnClickListener(view -> { selectedActionType = "Πάσα"; selectedPlayer = receiver; sendPassEventChain(innerReceiver, "Επιτυχής"); }); containerDynamicButtons.addView(btnInner);
                }
            }
        });
        containerDynamicButtons.addView(btnPass);

        Button btnCross = createStandardButton("📐 Σέντρα", "#163344");
        btnCross.setOnClickListener(v -> {
            subActionContext = "Υποδοχή: " + receiver.getName() + " ➔ Σέντρα"; containerDynamicButtons.removeAllViews(); updateSummaryBar();
            int listLimit = Math.min(cachedPlayers.size(), 11);
            for (int i = 0; i < listLimit; i++) {
                Player crossReceiver = cachedPlayers.get(i);
                if (crossReceiver.getId() != receiver.getId()) {
                    Button btnInner = createStandardButton(crossReceiver.getName(), "#163344");
                    btnInner.setOnClickListener(view -> {
                        containerDynamicButtons.removeAllViews(); subActionContext = "Σέντρα ➔ " + crossReceiver.getName(); updateSummaryBar();
                        Button btnHead = createStandardButton("👤 Κεφαλιά", "#163344"); btnHead.setOnClickListener(view2 -> renderShortCornerFinishingMatrix(crossReceiver, "Κεφαλιά")); containerDynamicButtons.addView(btnHead);
                        Button btnFoot = createStandardButton("👟 Σουτ", "#163344"); btnFoot.setOnClickListener(view2 -> renderShortCornerFinishingMatrix(crossReceiver, "Σουτ")); containerDynamicButtons.addView(btnFoot);
                    });
                    containerDynamicButtons.addView(btnInner);
                }
            }
        });
        containerDynamicButtons.addView(btnCross);

        Button btnFoul = createStandardButton("⚠️ Φάουλ", "#163344");
        btnFoul.setOnClickListener(v -> {
            subActionContext = "Φάουλ"; containerDynamicButtons.removeAllViews(); updateSummaryBar();
            String[] types = {"Απλό Φάουλ", "Κίτρινη κάρτα", "Κόκκινη κάρτα"};
            for (String t : types) { Button btn = createStandardButton(t, "#163344"); btn.setOnClickListener(view -> sendShortCornerCustomFinalEvent(receiver, "Φάουλ", t)); containerDynamicButtons.addView(btn); }
        });
        containerDynamicButtons.addView(btnFoul);
    }

    private void renderShortCornerFinishingMatrix(Player crossReceiver, String style) {
        containerDynamicButtons.removeAllViews(); subActionContext = style + " (" + crossReceiver.getName() + ")"; updateSummaryBar();
        String[] outcomes = {"Γκολ", "Άουτ", "Δοκάρι", "Απόκρουση"};
        for (String out : outcomes) {
            Button btn = createStandardButton(out, out.equals("Γκολ") ? "#2E7D32" : "#163344"); btn.setOnClickListener(v -> sendShortCornerCustomFinalEvent(crossReceiver, "Σέντρα (" + style + ")", out)); containerDynamicButtons.addView(btn);
        }
    }

    private void sendShortCornerCustomFinalEvent(Player execPlayer, String typeLabel, String outcome) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        String detailText = "Κοντινό Κόρνερ ➔ " + typeLabel + " από τον " + execPlayer.getName();
        apiService.addMatchEvent(matchId, selectedTeamId, currentFormattedMinuteString, "Κόρνερ (Πάσα)", detailText, outcome, execPlayer.getId())
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (response.isSuccessful()) { if (outcome.equals("Γκολ")) updateScoreboard(); resetToInitialState(); }
                    }
                    @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
                });
    }

    private void renderPenaltyOptions() {
        String[] AppResults = {"Γκολ", "Απόκρουση", "Άουτ", "Δοκάρι"};
        for (String result : AppResults) {
            Button btn = createStandardButton(result, result.equals("Γκολ") ? "#2E7D32" : "#163344"); btn.setOnClickListener(v -> sendLiveEvent(result)); containerDynamicButtons.addView(btn);
        }
    }

    private void sendPassEventChain(Player receiver, String result) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        String detailText = selectedActionType + " στον " + receiver.getName();
        apiService.addMatchEvent(matchId, selectedTeamId, currentFormattedMinuteString, selectedActionType, detailText, result, selectedPlayer.getId())
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (response.isSuccessful()) {
                            if (result.equals("Επιτυχής")) { selectedPlayer = receiver; selectedActionType = ""; subActionContext = ""; renderActionsState(); }
                            else { resetToInitialState(); }
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
                });
    }

    private void sendLiveEvent(String actionResult) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        // Εδώ σιγουρεύουμε ότι το type περιλαμβάνει το context για να το διαβάζει σωστά ο Φίλαθλος
        String finalAction = selectedActionType;
        if (!subActionContext.isEmpty()) {
            finalAction = selectedActionType + " (" + subActionContext + ")";
        }
        // Αν είναι κεφαλιά, βεβαιώσου ότι το subActionContext την περιλαμβάνει!

        apiService.addMatchEvent(matchId, selectedTeamId, currentFormattedMinuteString, finalAction, "Terminal Pro Live", actionResult, selectedPlayer.getId())
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (response.isSuccessful()) { if (actionResult.equals("Γκολ")) updateScoreboard(); resetToInitialState(); }
                    }
                    @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
                });
    }

    private void updateScoreboard() {
        if (selectedTeamId == homeTeamId) homeScore++; else awayScore++;
        tvScore.setText(homeScore + " - " + awayScore);
        SharedPreferences prefs = getSharedPreferences("MatchData_" + matchId, MODE_PRIVATE);
        prefs.edit().putInt("home_score", homeScore).putInt("away_score", awayScore).apply();
    }

    private void resetToInitialState() { selectedPosition = ""; selectedPlayer = null; selectedActionType = ""; subActionContext = ""; renderPositionsState(); }
    private void startSubstitutionFlow() { isSubstitutionMode = true; isChoosingSubIn = true; playerSubIn = null; selectedPosition = ""; selectedPlayer = null; selectedActionType = ""; subActionContext = ""; renderPositionsState(); }

    private void executeSubstitution(Player playerSubOut) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        String subDetail = "Μπήκε: " + playerSubIn.getName() + " ➔ Βγήκε: " + playerSubOut.getName();
        apiService.addMatchEvent(matchId, selectedTeamId, currentFormattedMinuteString, "Αλλαγή", subDetail, "Επιτυχής", playerSubIn.getId())
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (response.isSuccessful()) {
                            int idxIn = cachedPlayers.indexOf(playerSubIn); int idxOut = cachedPlayers.indexOf(playerSubOut);
                            if (idxIn != -1 && idxOut != -1) { cachedPlayers.set(idxOut, playerSubIn); cachedPlayers.set(idxIn, playerSubOut); }
                            isSubstitutionMode = false; selectedPosition = ""; renderPositionsState();
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
                });
    }

    private void updateSummaryBar() {
        layoutSummaryChips.removeAllViews();
        if (isSubstitutionMode) {
            addSummaryChip("🔄 ΑΛΛΑΓΗ", v -> { isSubstitutionMode = false; selectedPosition = ""; renderPositionsState(); }); addArrowSpacer();
            if (playerSubIn != null) { addSummaryChip("Μπαίνει: " + playerSubIn.getName(), v -> { isChoosingSubIn = true; playerSubIn = null; selectedPosition = ""; renderPositionsState(); }); addArrowSpacer(); }
            return;
        }
        if (selectedTeamId == -1) return;
        if (!selectedPosition.isEmpty()) addSummaryChip(selectedPosition, v -> resetToInitialState()); else if (selectedPlayer == null) return;
        if (selectedPlayer != null) { addArrowSpacer(); addSummaryChip(selectedPlayer.getName(), v -> { selectedActionType = ""; subActionContext = ""; renderPlayersState(); }); } else if (selectedActionType.isEmpty()) return;
        if (!selectedActionType.isEmpty()) { addArrowSpacer(); addSummaryChip(selectedActionType, v -> renderActionsState()); }
        if (!subActionContext.isEmpty()) { addArrowSpacer(); addSummaryChip(subActionContext, v -> { subActionContext = ""; renderResultsState(); }); }
    }

    private void addSummaryChip(String text, View.OnClickListener listener) {
        Button chip = new Button(this); chip.setText(text); chip.setTransformationMethod(null); chip.setTextSize(12); chip.setPadding(20, 8, 20, 8);
        chip.setTextColor(Color.WHITE); chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E53935")));
        if (listener != null) chip.setOnClickListener(listener); else chip.setEnabled(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(6, 0, 6, 0); chip.setLayoutParams(params); layoutSummaryChips.addView(chip);
    }

    private void addArrowSpacer() { TextView tv = new TextView(this); tv.setText("➔"); tv.setTextColor(Color.parseColor("#85A2B6")); tv.setTextSize(14); layoutSummaryChips.addView(tv); }

    private boolean belongsToPosition(Player p, String position) {
        String dbPos = p.getPosition() != null ? p.getPosition().toLowerCase().trim() : "";
        dbPos = dbPos.replace("έ", "ε").replace("ό", "ο").replace("ί", "ι").replace("ά", "α").replace("ή", "η").replace("ύ", "υ").replace("ώ", "ω");
        switch (position) {
            case "ΤΕΡΜΑΤΟΦΥΛΑΚΕΣ": return dbPos.contains("goalkeeper") || dbPos.contains("τερμα") || dbPos.contains("gk") || dbPos.contains("τερματοφυλακας");
            case "ΑΜΥΝΤΙΚΟΙ": return dbPos.contains("defender") || dbPos.contains("αμυν") || dbPos.contains("df") || dbPos.contains("αμυντικος");
            case "ΜΕΣΟΙ": return dbPos.contains("midfielder") || dbPos.contains("μεσ") || dbPos.contains("mf") || dbPos.contains("cm") || dbPos.contains("dm") || dbPos.contains("am");
            case "ΕΠΙΘΕΤΙΚΟΙ": return dbPos.contains("forward") || dbPos.contains("επιθ") || dbPos.contains("fw") || dbPos.contains("striker") || dbPos.contains("επιθετικος");
            default: return false;
        }
    }

    private Button createStandardButton(String text, String hexColor) {
        Button btn = new Button(this); btn.setText(text); btn.setTransformationMethod(null); btn.setTextColor(Color.WHITE); btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(hexColor)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); params.setMargins(0, 0, 0, 12); btn.setLayoutParams(params); btn.setPadding(16, 16, 16, 16); return btn;
    }

    @Override protected void onDestroy() { super.onDestroy(); timerHandler.removeCallbacks(updateTimerThread); }
}