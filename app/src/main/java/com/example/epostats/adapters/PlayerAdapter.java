package com.example.epostats.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.epostats.network.ApiResponse;
import com.example.epostats.network.ApiService;
import com.example.epostats.models.Player;
import com.example.epostats.R;
import com.example.epostats.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class  PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder> {
    private Context context;
    private List<Player> playerList;
    private int matchId;
    private int teamId;

    public PlayerAdapter(Context context, List<Player> playerList, int matchId, int teamId) {
        this.context = context;
        this.playerList = playerList;
        this.matchId = matchId;
        this.teamId = teamId;
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_player, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        Player player = playerList.get(position);
        holder.playerName.setText(player.getName());

        if (player.isStartingEleven()) {
            holder.playerPosition.setText(player.getPosition() + " (Βασικός)");
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            holder.itemView.setOnClickListener(v -> showSubstitutionDialog(player, position));
        } else {
            holder.playerPosition.setText(player.getPosition() + " (Πάγκος)");
            holder.itemView.setBackgroundColor(Color.parseColor("#F5F5F5"));
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() { return playerList.size(); }

    private void showSubstitutionDialog(Player outgoingPlayer, int outgoingPosition) {
        List<Player> benchPlayers = new ArrayList<>();
        List<String> benchNames = new ArrayList<>();
        for (Player p : playerList) {
            if (!p.isStartingEleven()) {
                benchPlayers.add(p);
                benchNames.add(p.getName() + " (" + p.getPosition() + ")");
            }
        }
        if (benchPlayers.isEmpty()) {
            Toast.makeText(context, "Δεν υπάρχουν παίκτες στον πάγκο", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Αλλαγή: Εκτός " + outgoingPlayer.getName());
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, benchNames);
        builder.setAdapter(arrayAdapter, (dialog, which) -> {
            Player incomingPlayer = benchPlayers.get(which);
            int incomingPosition = playerList.indexOf(incomingPlayer);
            outgoingPlayer.setStartingEleven(false);
            incomingPlayer.setStartingEleven(true);
            notifyItemChanged(outgoingPosition);
            notifyItemChanged(incomingPosition);
            logSubstitution(outgoingPlayer, incomingPlayer);
        });
        builder.show();
    }

    private void logSubstitution(Player out, Player in) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.addMatchEvent(matchId, teamId, 60, "ΑΛΛΑΓΗ", "ΜΠΗΚΕ", "ΕΠΙΤΥΧΗΣ", in.getId())
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if(response.isSuccessful()) {
                            Toast.makeText(context, "Η αλλαγή καταγράφηκε!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable t) {
                        Log.e("API_ERR", "Σφάλμα: " + t.getMessage());
                    }
                });
    }

    public static class PlayerViewHolder extends RecyclerView.ViewHolder {
        TextView playerName, playerPosition;
        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            playerName = itemView.findViewById(R.id.textPlayerName);
            playerPosition = itemView.findViewById(R.id.textPlayerPosition);
        }
    }
}