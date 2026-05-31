package com.example.epostats;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<MatchEvent> eventList;

    public EventAdapter(List<MatchEvent> eventList) {
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        MatchEvent event = eventList.get(position);

        String title = event.getEventMinute() + "' - " + event.getActionType() + " (" + event.getActionResult() + ")";
        String details = "Παίκτης: " + event.getPlayerName();
        if (!"-".equals(event.getAssistPlayerName()) && !event.getAssistPlayerName().contains("Επιλέξτε")) {
            details += " | Assist: " + event.getAssistPlayerName();
        }

        holder.text1.setText(title);
        holder.text2.setText(details);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}