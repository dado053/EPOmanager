package com.example.epostats.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;

import com.example.epostats.R;
import com.example.epostats.adapters.EventAdapter;
import com.example.epostats.models.MatchEvent;
import com.example.epostats.models.Player;
import com.example.epostats.network.ApiService;
import com.example.epostats.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsActivity extends AppCompatActivity {
    private int matchId, homeTeamId, awayTeamId;
    private String homeName, awayName;
    private boolean isAdmin;

    private RecyclerView rvEvents, rvStats, rvLineups;
    private TabLayout tabLayout;
    private TextView tvMainHomeName, tvMainAwayName, tvMainScore, tvMatchClock;
    private ApiService apiService;

    private List<Player> homePlayers = new ArrayList<>();
    private List<Player> awayPlayers = new ArrayList<>();
    private int callsCompleted = 0;

    private final List<EventListItem> globalEventsList = new ArrayList<>();
    private EventAdapter eventAdapter;

    private final List<StatItem> globalStatsList = new ArrayList<>();
    private StatBarAdapter statBarAdapter;

    private final List<LineupRow> globalLineupList = new ArrayList<>();
    private LineupAdapter lineupAdapter;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 5000;

    private Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;
    private long matchStartTimeMillis = 0L;
    private boolean isClockRunning = false;

    // Καθυστερήσεις που έρχονται live από τον Admin
    private int injury1stHalf = 0;
    private int injury2ndHalf = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        matchId = getIntent().getIntExtra("MATCH_ID", -1);
        homeTeamId = getIntent().getIntExtra("HOME_TEAM_ID", -1);
        awayTeamId = getIntent().getIntExtra("AWAY_TEAM_ID", -1);
        homeName = getIntent().getStringExtra("HOME_TEAM_NAME");
        awayName = getIntent().getStringExtra("AWAY_TEAM_NAME");
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        tvMainHomeName = findViewById(R.id.tvMainHomeName);
        tvMainAwayName = findViewById(R.id.tvMainAwayName);
        tvMainScore = findViewById(R.id.tvMainScore);
        tvMatchClock = findViewById(R.id.tvMatchClock);

        tvMainHomeName.setText(homeName);
        tvMainAwayName.setText(awayName);

        tabLayout = findViewById(R.id.tabLayout);
        rvEvents = findViewById(R.id.rvEvents);
        rvStats = findViewById(R.id.rvStats);
        rvLineups = findViewById(R.id.rvLineups);

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvStats.setLayoutManager(new LinearLayoutManager(this));
        rvLineups.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(globalEventsList, homeTeamId);
        rvEvents.setAdapter(eventAdapter);

        statBarAdapter = new StatBarAdapter(globalStatsList);
        rvStats.setAdapter(statBarAdapter);

        lineupAdapter = new LineupAdapter(globalLineupList);
        rvLineups.setAdapter(lineupAdapter);

        tabLayout.addTab(tabLayout.newTab().setText("ΣΥΝΟΨΗ"));
        tabLayout.addTab(tabLayout.newTab().setText("ΣΤΑΤΙΣΤΙΚΑ"));
        tabLayout.addTab(tabLayout.newTab().setText("ΠΑΙΚΤΕΣ"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) { rvEvents.setVisibility(View.VISIBLE); rvStats.setVisibility(View.GONE); rvLineups.setVisibility(View.GONE); }
                else if (tab.getPosition() == 1) { rvEvents.setVisibility(View.GONE); rvStats.setVisibility(View.VISIBLE); rvLineups.setVisibility(View.GONE); }
                else if (tab.getPosition() == 2) { rvEvents.setVisibility(View.GONE); rvStats.setVisibility(View.GONE); rvLineups.setVisibility(View.VISIBLE); }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        apiService = RetrofitClient.getClient().create(ApiService.class);

        loadMatchStats();
        fetchTeam(homeTeamId, true);
        fetchTeam(awayTeamId, false);

        refreshRunnable = new Runnable() {
            @Override
            public void run() { loadMatchStats(); handler.postDelayed(this, REFRESH_INTERVAL); }
        };
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL);

        // ΣΥΡΧΡΟΝΙΣΜΕΝΟ LIVE ΡΟΛΟΪ ΦΙΛΑΘΛΟΥ ΜΕ ΒΑΣΗ ΤΑ ΛΕΠΤΑ Injuries ΤΟΥ ADMIN
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                if (matchStartTimeMillis > 0) {
                    long elapsedMillis = System.currentTimeMillis() - matchStartTimeMillis;
                    int totalSecs = (int) (elapsedMillis / 1000);
                    int totalMins = totalSecs / 60;
                    int secs = totalSecs % 60;

                    if (totalMins < 45) {
                        tvMatchClock.setText(String.format("%02d:%02d (LIVE)", totalMins, secs));
                    }
                    else if (totalMins >= 45 && totalMins < 45 + injury1stHalf) {
                        int extraSecs = totalSecs - (45 * 60);
                        tvMatchClock.setText(String.format("45:00 +%02d:%02d (LIVE)", extraSecs / 60, extraSecs % 60));
                    }
                    else if (totalMins >= 45 + injury1stHalf && totalMins < 90 + injury1stHalf) {
                        int adjustedMins = totalMins - injury1stHalf;
                        tvMatchClock.setText(String.format("%02d:%02d (LIVE)", adjustedMins, secs));
                    }
                    else {
                        int extraSecs = totalSecs - ((90 + injury1stHalf) * 60);
                        tvMatchClock.setText(String.format("90:00 +%02d:%02d (LIVE)", extraSecs / 60, extraSecs % 60));
                    }
                }
                clockHandler.postDelayed(this, 1000);
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
        clockHandler.removeCallbacks(clockRunnable);
    }

    private void loadMatchStats() {
        apiService.getMatchEvents(matchId).enqueue(new Callback<List<MatchEvent>>() {
            @Override
            public void onResponse(Call<List<MatchEvent>> call, Response<List<MatchEvent>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MatchEvent> rawEvents = response.body();
                    List<EventListItem> processedList = new ArrayList<>();

                    int homeGoals = 0, awayGoals = 0;
                    EventListItem header1st = null;
                    EventListItem header2nd = null;

                    if (!rawEvents.isEmpty() && matchStartTimeMillis == 0) {
                        for (MatchEvent event : rawEvents) {
                            if (event.getStartTime() > 1000000000000L) {
                                matchStartTimeMillis = event.getStartTime();
                                if (!isClockRunning) { clockHandler.postDelayed(clockRunnable, 0); isClockRunning = true; }
                                break;
                            }
                        }
                    }

                    // Διαβάζουμε αν υπάρχουν δυναμικές καθυστερήσεις αποθηκευμένες στα events
                    for (MatchEvent e : rawEvents) {
                        String rawMinStr = e.getEventMinute() != null ? e.getEventMinute().trim() : "1";
                        if (rawMinStr.contains("45+")) {
                            try { injury1stHalf = Math.max(injury1stHalf, Integer.parseInt(rawMinStr.split("\\+")[1])); } catch (Exception ignored) {}
                        }
                        if (rawMinStr.contains("90+")) {
                            try { injury2ndHalf = Math.max(injury2ndHalf, Integer.parseInt(rawMinStr.split("\\+")[1])); } catch (Exception ignored) {}
                        }

                        if ("Γκολ".equalsIgnoreCase(e.getActionResult())) {
                            if (e.getTeamId() == homeTeamId) homeGoals++; else awayGoals++;
                        }

                        String type = e.getActionType() != null ? e.getActionType() : "";
                        String res = e.getActionResult() != null ? e.getActionResult() : "";
                        boolean isGoal = "Γκολ".equalsIgnoreCase(res);
                        boolean isCard = "Κίτρινη κάρτα".equalsIgnoreCase(res) || "Κόκκινη κάρτα".equalsIgnoreCase(res);
                        boolean isSub = "Αλλαγή".equalsIgnoreCase(type);

                        if (isGoal || isCard || isSub) {
                            int baseMin = 1;
                            try { baseMin = Integer.parseInt(rawMinStr.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}

                            if ((baseMin <= 45 || rawMinStr.contains("45+")) && !rawMinStr.startsWith("46") && !rawMinStr.startsWith("47")) {
                                if (header1st == null) {
                                    header1st = new EventListItem("1ο ΗΜΙΧΡΟΝΟ");
                                    processedList.add(header1st);
                                }
                                header1st.headerScore = homeGoals + " - " + awayGoals;
                            } else {
                                if (header2nd == null) {
                                    header2nd = new EventListItem("2ο ΗΜΙΧΡΟΝΟ");
                                    processedList.add(header2nd);
                                }
                                header2nd.headerScore = homeGoals + " - " + awayGoals;
                            }

                            EventListItem item = new EventListItem(e);
                            item.currentScoreStr = homeGoals + " - " + awayGoals;
                            processedList.add(item);
                        }
                    }

                    tvMainScore.setText(homeGoals + " - " + awayGoals);
                    if (matchStartTimeMillis == 0) tvMatchClock.setText("ΔΕΝ ΞΕΚΙΝΗΣΕ");

                    globalEventsList.clear();
                    globalEventsList.addAll(processedList);
                    eventAdapter.notifyDataSetChanged();

                    updateDynamicStatsUI(rawEvents);
                }
            }
            @Override public void onFailure(Call<List<MatchEvent>> call, Throwable t) {}
        });
    }

    private void updateDynamicStatsUI(List<MatchEvent> events) {
        int hShots = 0, aShots = 0, hOnTarget = 0, aOnTarget = 0, hOffTarget = 0, aOffTarget = 0;
        int hPasses = 0, aPasses = 0, hPassesEff = 0, aPassesEff = 0;
        int hCrosses = 0, aCrosses = 0, hCrossesEff = 0, aCrossesEff = 0;
        int hTackles = 0, aTackles = 0, hFouls = 0, aFouls = 0, hCorners = 0, aCorners = 0;
        int hYellow = 0, aYellow = 0, hRed = 0, aRed = 0, hHeaderGoals = 0, aHeaderGoals = 0;

        for (MatchEvent e : events) {
            boolean isHome = e.getTeamId() == homeTeamId;
            String type = e.getActionType();
            String res = e.getActionResult();

            // 1. ΣΤΑΤΙΣΤΙΚΑ ΣΟΥΤ & ΚΕΦΑΛΙΩΝ
            if ("Σουτ".equalsIgnoreCase(type) || (type != null && type.contains("Κεφαλιά"))) {
                // Συνολικά Σουτ = Μόνο τα Γκολ
                if ("Γκολ".equalsIgnoreCase(res)) {
                    if (isHome) hShots++; else aShots++;
                    // Αν είναι κεφαλιά, προσμετράται και στα Γκολ με κεφαλιά
                    if (type != null && type.contains("Κεφαλιά")) {
                        if (isHome) hHeaderGoals++; else aHeaderGoals++;
                    }
                }

                // Σουτ στο στόχο = Γκολ + Δοκάρι + Απόκρουση
                if ("Γκολ".equalsIgnoreCase(res) || "Δοκάρι".equalsIgnoreCase(res) || "Απόκρουση".equalsIgnoreCase(res)) {
                    if (isHome) hOnTarget++; else aOnTarget++;
                }
                // Άστοχα Σουτ = Άουτ
                else if ("Άουτ".equalsIgnoreCase(res)) {
                    if (isHome) hOffTarget++; else aOffTarget++;
                }
            }
            // 2. ΠΑΣΕΣ & ΣΕΝΤΡΕΣ
            else if ("Πάσα".equalsIgnoreCase(type)) {
                if (isHome) hPasses++; else aPasses++;
                if ("Επιτυχής".equalsIgnoreCase(res)) { if (isHome) hPassesEff++; else aPassesEff++; }
            } else if ("Σέντρα".equalsIgnoreCase(type)) {
                if (isHome) hCrosses++; else aCrosses++;
                if ("Επιτυχής".equalsIgnoreCase(res)) { if (isHome) hCrossesEff++; else aCrossesEff++; }
            }
            // ΥΠΟΛΟΙΠΑ
            else if ("Τάκλιν".equalsIgnoreCase(type)) { if (isHome) hTackles++; else aTackles++; }
            else if ("Φάουλ".equalsIgnoreCase(type)) { if (isHome) hFouls++; else aFouls++; }
            else if ("Κόρνερ".equalsIgnoreCase(type)) { if (isHome) hCorners++; else aCorners++; }

            if ("Κίτρινη κάρτα".equalsIgnoreCase(res)) { if (isHome) hYellow++; else aYellow++; }
            else if ("Κόκκινη κάρτα".equalsIgnoreCase(res)) { if (isHome) hRed++; else aRed++; }
        }

        // Υπολογισμός Ακρίβειας (Πάσες + Σέντρες)
        int totalH = hPasses + hCrosses;
        int succH = hPassesEff + hCrossesEff;
        int hAcc = totalH > 0 ? (succH * 100) / totalH : 0;

        int totalA = aPasses + aCrosses;
        int succA = aPassesEff + aCrossesEff;
        int aAcc = totalA > 0 ? (succA * 100) / totalA : 0;

        // Ενημέρωση λίστας για το adapter
        List<StatItem> stats = new ArrayList<>();
        stats.add(new StatItem("ΕΠΙΘΕΣΗ", "", 0, 0, false, true));
        stats.add(new StatItem("", "Συνολικά Σουτ (Γκολ)", hShots, aShots, false, false));
        stats.add(new StatItem("", "Σουτ στο στόχο", hOnTarget, aOnTarget, false, false));
        stats.add(new StatItem("", "Άστοχα σουτ (Άουτ)", hOffTarget, aOffTarget, false, false));
        stats.add(new StatItem("", "Κόρνερ", hCorners, aCorners, false, false));
        stats.add(new StatItem("", "Γκολ με κεφαλιά", hHeaderGoals, aHeaderGoals, false, false));

        stats.add(new StatItem("ΠΑΣΕΣ", "", 0, 0, false, true));
        stats.add(new StatItem("", "Ακρίβεια πασών/σεντρών (%)", hAcc, aAcc, true, false));
        stats.add(new StatItem("", "Συνολικές Πάσες", hPasses, aPasses, false, false));
        stats.add(new StatItem("", "Επιτυχημένες Πάσες", hPassesEff, aPassesEff, false, false));
        stats.add(new StatItem("", "Σέντρες", hCrosses, aCrosses, false, false));
        stats.add(new StatItem("", "Επιτυχημένες Σέντρες", hCrossesEff, aCrossesEff, false, false));

        stats.add(new StatItem("ΑΜΥΝΑ", "", 0, 0, false, true));
        stats.add(new StatItem("", "Τάκλιν", hTackles, aTackles, false, false));
        stats.add(new StatItem("", "Φάουλ", hFouls, aFouls, false, false));
        stats.add(new StatItem("", "Κίτρινες Κάρτες", hYellow, aYellow, false, false));
        stats.add(new StatItem("", "Κόκκινες Κάρτες", hRed, aRed, false, false));

        globalStatsList.clear();
        globalStatsList.addAll(stats);
        statBarAdapter.notifyDataSetChanged();
    }

    private void fetchTeam(int teamId, boolean isHome) {
        apiService.getTeamPlayers(teamId).enqueue(new Callback<List<Player>>() {
            @Override public void onResponse(Call<List<Player>> call, Response<List<Player>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Player> team = response.body(); for (int i = 0; i < team.size(); i++) team.get(i).setStartingEleven(i < 11);
                    if (isHome) homePlayers.addAll(team); else awayPlayers.addAll(team);
                }
                checkAndBuildLineup();
            }
            @Override public void onFailure(Call<List<Player>> call, Throwable t) { checkAndBuildLineup(); }
        });
    }

    private synchronized void checkAndBuildLineup() { callsCompleted++; if (callsCompleted == 2) buildSideBySideList(); }
    private void buildSideBySideList() {
        List<Player> homeStarters = new ArrayList<>(), homeBench = new ArrayList<>(); for (Player p : homePlayers) { if (p.isStartingEleven()) homeStarters.add(p); else homeBench.add(p); }
        List<Player> awayStarters = new ArrayList<>(), awayBench = new ArrayList<>(); for (Player p : awayPlayers) { if (p.isStartingEleven()) awayStarters.add(p); else awayBench.add(p); }
        List<LineupRow> rows = new ArrayList<>(); LineupRow starterHeader = new LineupRow(); starterHeader.isHeader = true; starterHeader.headerText = "ΒΑΣΙΚΟΙ"; rows.add(starterHeader);
        int maxStarters = Math.max(homeStarters.size(), awayStarters.size());
        for (int i = 0; i < maxStarters; i++) { LineupRow row = new LineupRow(); if (i < homeStarters.size()) row.homePlayer = homeStarters.get(i); if (i < awayStarters.size()) row.awayPlayer = awayStarters.get(i); rows.add(row); }

        LineupRow benchHeader = new LineupRow(); benchHeader.isHeader = true; benchHeader.headerText = "ΑΝΑΠΛΗΡΩΜΑΤΙΚΟΙ"; rows.add(benchHeader);
        int maxBench = Math.max(homeBench.size(), awayBench.size());
        for (int i = 0; i < maxBench; i++) { LineupRow row = new LineupRow(); if (i < homeBench.size()) row.homePlayer = homeBench.get(i); if (i < awayBench.size()) row.awayPlayer = awayBench.get(i); rows.add(row); }
        globalLineupList.clear(); globalLineupList.addAll(rows); lineupAdapter.notifyDataSetChanged();
    }

    static class LineupRow { boolean isHeader = false; String headerText; Player homePlayer; Player awayPlayer; }
    static class StatItem { String headerTitle; String title; double homeValue; double awayValue; boolean isPercentage; boolean isHeader; StatItem(String ht, String t, double hv, double av, boolean ip, boolean ih) { headerTitle=ht; title=t; homeValue=hv; awayValue=av; isPercentage=ip; isHeader=ih; } }
    public static class EventListItem { public boolean isHeader; public String headerText; public String headerScore; public MatchEvent event; public String currentScoreStr; public EventListItem(String headerText) { this.isHeader = true; this.headerText = headerText; this.headerScore = "0 - 0"; } public EventListItem(MatchEvent event) { this.isHeader = false; this.event = event; } }

    static class LineupAdapter extends RecyclerView.Adapter<LineupAdapter.ViewHolder> {
        private final List<LineupRow> rows; LineupAdapter(List<LineupRow> rows) { this.rows = rows; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int v) { return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_lineup_row, p, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            LineupRow r = rows.get(pos);
            if (r.isHeader) { h.tvSectionHeader.setVisibility(View.VISIBLE); h.tvSectionHeader.setText(r.headerText); h.layoutPlayersRow.setVisibility(View.GONE); }
            else { h.tvSectionHeader.setVisibility(View.GONE); h.layoutPlayersRow.setVisibility(View.VISIBLE); h.tvHomePlayer.setText(r.homePlayer != null ? r.homePlayer.getName() : ""); h.tvAwayPlayer.setText(r.awayPlayer != null ? r.awayPlayer.getName() : ""); }
        }
        @Override public int getItemCount() { return rows.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { TextView tvSectionHeader, tvHomePlayer, tvAwayPlayer; View layoutPlayersRow; ViewHolder(View v) { super(v); tvSectionHeader=v.findViewById(R.id.tvSectionHeader); tvHomePlayer=v.findViewById(R.id.tvHomePlayer); tvAwayPlayer=v.findViewById(R.id.tvAwayPlayer); layoutPlayersRow=v.findViewById(R.id.layoutPlayersRow); } }
    }

    static class StatBarAdapter extends RecyclerView.Adapter<StatBarAdapter.ViewHolder> {
        private final List<StatItem> statList; StatBarAdapter(List<StatItem> statList) { this.statList = statList; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stat_bar, parent, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StatItem stat = statList.get(position);
            if (stat.isHeader) { holder.tvCategoryHeader.setVisibility(View.VISIBLE); holder.tvCategoryHeader.setText(stat.headerTitle); holder.layoutStatBody.setVisibility(View.GONE); }
            else {
                holder.tvCategoryHeader.setVisibility(View.GONE); holder.layoutStatBody.setVisibility(View.VISIBLE); holder.tvTitle.setText(stat.title);
                if (stat.isPercentage) { holder.tvHomeVal.setText((int)stat.homeValue + "%"); holder.tvAwayVal.setText((int)stat.awayValue + "%"); }
                else { holder.tvHomeVal.setText(String.valueOf(stat.homeValue).replace(".0", "")); holder.tvAwayVal.setText(String.valueOf(stat.awayValue).replace(".0", "")); }

                // ΕΠΑΝΑΦΟΡΑ ΤΟΥ UI WEIGHTS ΓΙΑ ΤΙΣ ΜΠΑΡΕΣ ΠΡΟΟΔΟΥ (image_4cf0c6.png)
                float hW = 0, aW = 0;
                if (stat.homeValue + stat.awayValue > 0) { hW = (float)((stat.homeValue/(stat.homeValue+stat.awayValue))*100); aW = (float)((stat.awayValue/(stat.homeValue+stat.awayValue))*100); }
                setWeight(holder.barHome, hW); setWeight(holder.spacerHome, 100-hW); setWeight(holder.barAway, aW); setWeight(holder.spacerAway, 100-aW);
            }
        }
        private void setWeight(View v, float w) { LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) v.getLayoutParams(); p.weight = w; v.setLayoutParams(p); }
        @Override public int getItemCount() { return statList.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { TextView tvCategoryHeader, tvTitle, tvHomeVal, tvAwayVal; View barHome, spacerHome, barAway, spacerAway, layoutStatBody; ViewHolder(View v) { super(v); tvCategoryHeader=v.findViewById(R.id.tvCategoryHeader); layoutStatBody=v.findViewById(R.id.layoutStatBody); tvTitle=v.findViewById(R.id.tvStatTitle); tvHomeVal=v.findViewById(R.id.tvHomeValue); tvAwayVal=v.findViewById(R.id.tvAwayValue); barHome=v.findViewById(R.id.barHome); spacerHome=v.findViewById(R.id.spacerHome); barAway=v.findViewById(R.id.barAway); spacerAway=v.findViewById(R.id.spacerAway); } }
    }
}