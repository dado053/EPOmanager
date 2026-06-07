package com.example.epostats.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.epostats.R;
import com.example.epostats.models.Player;
import com.example.epostats.network.ApiService;
import com.example.epostats.network.ApiResponse;
import com.example.epostats.network.RetrofitClient;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.ViewHolder> {
    private final List<Player> players;
    private final int matchId;
    private final int teamId;
    private final String currentMinuteStr;

    public PlayerAdapter(List<Player> players, int matchId, int teamId, String currentMinuteStr) {
        this.players = players;
        this.matchId = matchId;
        this.teamId = teamId;
        this.currentMinuteStr = currentMinuteStr != null ? currentMinuteStr : "1";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Player player = players.get(position);
        holder.tvPlayerName.setText(player.getName());

        holder.itemView.setOnClickListener(v -> {
            ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
            apiService.addMatchEvent(
                    matchId,
                    teamId,
                    currentMinuteStr,
                    "Ενέργεια",
                    "PDA Touch",
                    "Επιτυχής",
                    player.getId()
            ).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {}
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {}
            });
        });
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlayerName;
        ViewHolder(View itemView) {
            super(itemView);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
        }
    }
}