package com.example.epostats.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
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

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder> {
    private Context context;
    private List<Player> playerList;
    private int matchId;
    private int teamId;

    // Η ΔΙΟΡΘΩΣΗ: Το Interface μεταφέρθηκε εδώ έξω για να είναι συμβατό με Java 8
    public interface OnBenchPlayerClickListener {
        void onClick(Player incomingPlayer);
    }

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
            holder.playerPosition.setText(player.getPosition() + "\n(Βασικός)");
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            holder.itemView.setOnClickListener(v -> showSubstitutionDialog(player, position));
        } else {
            holder.playerPosition.setText(player.getPosition() + "\n(Πάγκος)");
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0"));
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() { return playerList.size(); }

    private void showSubstitutionDialog(Player outgoingPlayer, int outgoingPosition) {
        List<Player> eligibleBenchPlayers = new ArrayList<>();

        for (Player p : playerList) {
            if (!p.isStartingEleven() && p.getPosition() != null && p.getPosition().equals(outgoingPlayer.getPosition())) {
                eligibleBenchPlayers.add(p);
            }
        }

        if (eligibleBenchPlayers.isEmpty()) {
            Toast.makeText(context, "Δεν υπάρχουν παίκτες στον πάγκο για τη θέση: " + outgoingPlayer.getPosition(), Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_bench_players, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        tvTitle.setText("Αλλαγή (" + outgoingPlayer.getPosition() + ")\nΕκτός: " + outgoingPlayer.getName());

        RecyclerView recyclerBench = dialogView.findViewById(R.id.recyclerBenchPlayers);
        recyclerBench.setLayoutManager(new GridLayoutManager(context, 2));

        AlertDialog dialog = builder.create();

        BenchAdapter benchAdapter = new BenchAdapter(eligibleBenchPlayers, incomingPlayer -> {
            int incomingPosition = playerList.indexOf(incomingPlayer);

            outgoingPlayer.setStartingEleven(false);
            incomingPlayer.setStartingEleven(true);

            notifyItemChanged(outgoingPosition);
            notifyItemChanged(incomingPosition);

            logSubstitution(outgoingPlayer, incomingPlayer);

            dialog.dismiss();
        });

        recyclerBench.setAdapter(benchAdapter);
        dialog.show();
    }

    private void logSubstitution(Player out, Player in) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.addMatchEvent(matchId, teamId, 60, "ΑΛΛΑΓΗ", "ΜΠΗΚΕ", "ΕΠΙΤΥΧΗΣ", in.getId())
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if(response.isSuccessful()) {
                            Toast.makeText(context, "Η αλλαγή καταγράφηκε: Μπήκε ο " + in.getName(), Toast.LENGTH_SHORT).show();
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
        CardView cardView;

        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            playerName = itemView.findViewById(R.id.textPlayerName);
            playerPosition = itemView.findViewById(R.id.textPlayerPosition);
            cardView = itemView.findViewById(R.id.cardViewPlayer);
        }
    }

    // --- ΕΣΩΤΕΡΙΚΗ ΚΛΑΣΗ ΓΙΑ ΤΟ ΠΛΕΓΜΑ ΤΟΥ ΠΑΓΚΟΥ ---
    private class BenchAdapter extends RecyclerView.Adapter<PlayerViewHolder> {
        private List<Player> benchList;
        private OnBenchPlayerClickListener listener;

        BenchAdapter(List<Player> benchList, OnBenchPlayerClickListener listener) {
            this.benchList = benchList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_player, parent, false);
            return new PlayerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
            Player p = benchList.get(position);
            holder.playerName.setText(p.getName());
            holder.playerPosition.setText(p.getPosition());

            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFFFFF"));

            holder.itemView.setOnClickListener(v -> listener.onClick(p));
        }

        @Override
        public int getItemCount() {
            return benchList.size();
        }
    }
}