package com.example.epostats;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {
    private List<Match> matchList;
    private Context context;

    public MatchAdapter(Context context, List<Match> matchList) {
        this.context = context;
        this.matchList = matchList;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match match = matchList.get(position);

        holder.homeTeam.setText(match.getHomeTeamName());
        holder.awayTeam.setText(match.getAwayTeamName());

        // --- ΛΟΓΙΚΗ ΓΙΑ ΤΗΝ ΕΠΙΚΕΦΑΛΙΔΑ ΤΗΣ ΑΓΩΝΙΣΤΙΚΗΣ ---
        boolean showHeader = false;

        if (position == 0) {
            // Στον πρώτο-πρώτο αγώνα της λίστας δείχνουμε πάντα την επικεφαλίδα
            showHeader = true;
        } else {
            // Συγκρίνουμε την αγωνιστική αυτού του αγώνα με του ακριβώς προηγούμενου
            Match previousMatch = matchList.get(position - 1);
            if (match.getMatchday() != previousMatch.getMatchday()) {
                showHeader = true; // Άλλαξε η αγωνιστική, άρα δείξε την επικεφαλίδα!
            }
        }

        if (showHeader) {
            holder.tvMatchdayHeader.setVisibility(View.VISIBLE);
            holder.tvMatchdayHeader.setText("Αγωνιστική " + match.getMatchday());
        } else {
            // Αν είναι ίδια αγωνιστική με τον προηγούμενο, κρύψε την επικεφαλίδα
            holder.tvMatchdayHeader.setVisibility(View.GONE);
        }
        // --------------------------------------------------

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MatchCardActivity.class);
            intent.putExtra("MATCH_ID", match.getMatchId());
            intent.putExtra("HOME_TEAM_ID", match.getHomeTeamId());
            intent.putExtra("AWAY_TEAM_ID", match.getAwayTeamId());
            intent.putExtra("HOME_TEAM_NAME", match.getHomeTeamName());
            intent.putExtra("AWAY_TEAM_NAME", match.getAwayTeamName());
            if (context instanceof MainActivity) {
                intent.putExtra("IS_ADMIN", ((MainActivity) context).getIntent().getBooleanExtra("IS_ADMIN", false));
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return matchList.size(); }

    public static class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView homeTeam, awayTeam, tvMatchdayHeader; // Προσθέσαμε το tvMatchdayHeader

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            homeTeam = itemView.findViewById(R.id.textHomeTeam);
            awayTeam = itemView.findViewById(R.id.textAwayTeam);
            tvMatchdayHeader = itemView.findViewById(R.id.tvMatchdayHeader); // Σύνδεση με το XML
        }
    }
}