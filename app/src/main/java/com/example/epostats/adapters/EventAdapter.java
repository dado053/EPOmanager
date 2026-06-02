package com.example.epostats.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.epostats.R;
import com.example.epostats.activities.StatsActivity;
import com.example.epostats.activities.StatsActivity.EventListItem;
import com.example.epostats.models.MatchEvent;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<EventListItem> eventList;
    private int homeTeamId;

    public EventAdapter(List<EventListItem> eventList, int homeTeamId) {
        this.eventList = eventList;
        this.homeTeamId = homeTeamId;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_match_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventListItem item = eventList.get(position);

        if (item.isHeader) {
            holder.layoutHalfHeader.setVisibility(View.VISIBLE);
            holder.tvHalfHeaderTitle.setText(item.headerText);
            holder.tvHalfHeaderScore.setText(item.headerScore);
            holder.layoutEventBody.setVisibility(View.GONE);
            return;
        }

        holder.layoutHalfHeader.setVisibility(View.GONE);
        holder.layoutEventBody.setVisibility(View.VISIBLE);
        MatchEvent event = item.event;

        String minuteStr = event.getEventMinute() + "'";
        String primaryText = event.getPlayerName();
        String secondaryText = "";
        String iconStr = "";

        if ("Γκολ".equalsIgnoreCase(event.getActionResult())) {
            iconStr = "⚽ " + item.currentScoreStr;
            if (event.getAssistPlayerName() != null && !event.getAssistPlayerName().equals("-") && !event.getAssistPlayerName().contains("Επιλέξτε")) {
                secondaryText = "(" + event.getAssistPlayerName() + ")";
            }
        }
        else if ("ΑΛΛΑΓΗ".equalsIgnoreCase(event.getActionType())) {
            iconStr = "🔄";
            if (event.getAssistPlayerName() != null && !event.getAssistPlayerName().equals("-")) {
                secondaryText = event.getAssistPlayerName();
            }
        }
        else if ("Κίτρινη κάρτα".equalsIgnoreCase(event.getActionResult())) {
            iconStr = "🟨";
        }
        else if ("Κόκκινη κάρτα".equalsIgnoreCase(event.getActionResult())) {
            iconStr = "🟥";
        }
        else {
            iconStr = "▪";
            if (event.getAssistPlayerName() != null && !event.getAssistPlayerName().equals("-")) {
                secondaryText = event.getAssistPlayerName();
            }
        }

        if (event.getTeamId() == homeTeamId) {
            holder.layoutHomeEvent.setVisibility(View.VISIBLE);
            holder.layoutAwayEvent.setVisibility(View.GONE);

            holder.tvMinHome.setText(minuteStr);
            holder.tvIconHome.setText(iconStr);
            holder.tvPrimaryHome.setText(primaryText);
            holder.tvSecondaryHome.setText(secondaryText);
            holder.tvSecondaryHome.setVisibility(secondaryText.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            holder.layoutHomeEvent.setVisibility(View.GONE);
            holder.layoutAwayEvent.setVisibility(View.VISIBLE);

            holder.tvMinAway.setText(minuteStr);
            holder.tvIconAway.setText(iconStr);
            holder.tvPrimaryAway.setText(primaryText);
            holder.tvSecondaryAway.setText(secondaryText);
            holder.tvSecondaryAway.setVisibility(secondaryText.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() { return eventList.size(); }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        View layoutHalfHeader, layoutEventBody;
        TextView tvHalfHeaderTitle, tvHalfHeaderScore;
        LinearLayout layoutHomeEvent, layoutAwayEvent;

        TextView tvMinHome, tvIconHome, tvPrimaryHome, tvSecondaryHome;
        TextView tvMinAway, tvIconAway, tvPrimaryAway, tvSecondaryAway;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutHalfHeader = itemView.findViewById(R.id.layoutHalfHeader);
            tvHalfHeaderTitle = itemView.findViewById(R.id.tvHalfHeaderTitle);
            tvHalfHeaderScore = itemView.findViewById(R.id.tvHalfHeaderScore);

            layoutEventBody = itemView.findViewById(R.id.layoutEventBody);

            layoutHomeEvent = itemView.findViewById(R.id.layoutHomeEvent);
            tvMinHome = itemView.findViewById(R.id.tvMinHome);
            tvIconHome = itemView.findViewById(R.id.tvIconHome);
            tvPrimaryHome = itemView.findViewById(R.id.tvPrimaryHome);
            tvSecondaryHome = itemView.findViewById(R.id.tvSecondaryHome);

            layoutAwayEvent = itemView.findViewById(R.id.layoutAwayEvent);
            tvMinAway = itemView.findViewById(R.id.tvMinAway);
            tvIconAway = itemView.findViewById(R.id.tvIconAway);
            tvPrimaryAway = itemView.findViewById(R.id.tvPrimaryAway);
            tvSecondaryAway = itemView.findViewById(R.id.tvSecondaryAway);
        }
    }
}