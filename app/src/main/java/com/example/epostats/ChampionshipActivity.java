package com.example.epostats;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;

public class ChampionshipActivity extends AppCompatActivity {

    private RecyclerView recyclerChampionships;
    private ChampionshipAdapter adapter;

    // Ενσωματώνουμε το API Call απευθείας εδώ (όπως είχες κάνει στο R5StatsActivity)
    interface ChampionshipApi {
        // ΣΗΜΕΙΩΣΗ: Αν το αρχείο PHP είναι μέσα σε φάκελο (π.χ. epo_project), άλλαξέ το εδώ
        @GET("api_get_championships.php")
        Call<List<ChampionshipModel>> getChampionships();
    }

    // Το Model των δεδομένων (ίδιο με τα πεδία της βάσης σου)
    static class ChampionshipModel {
        int id;
        String name;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_championship);

        recyclerChampionships = findViewById(R.id.recycler_championships);
        recyclerChampionships.setLayoutManager(new LinearLayoutManager(this));

        fetchChampionships();
    }

    private void fetchChampionships() {
        ChampionshipApi api = RetrofitClient.getClient().create(ChampionshipApi.class);
        api.getChampionships().enqueue(new Callback<List<ChampionshipModel>>() {
            @Override
            public void onResponse(Call<List<ChampionshipModel>> call, Response<List<ChampionshipModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ChampionshipModel> list = response.body();

                    if (list.isEmpty()) {
                        Toast.makeText(ChampionshipActivity.this, "Δεν υπάρχουν πρωταθλήματα στη βάση.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Τροφοδοτούμε τη λίστα στον Adapter
                    adapter = new ChampionshipAdapter(list);
                    recyclerChampionships.setAdapter(adapter);
                } else {
                    Toast.makeText(ChampionshipActivity.this, "Σφάλμα ανάγνωσης δεδομένων (Code: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ChampionshipModel>> call, Throwable t) {
                Toast.makeText(ChampionshipActivity.this, "Σφάλμα Δικτύου: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Εσωτερικός Adapter για να διαχειρίζεται το UI της κάθε κάρτας
    private static class ChampionshipAdapter extends RecyclerView.Adapter<ChampionshipAdapter.ViewHolder> {
        private final List<ChampionshipModel> list;

        ChampionshipAdapter(List<ChampionshipModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_championship, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChampionshipModel champ = list.get(position);
            holder.tvName.setText(champ.name);

            // Τι συμβαίνει όταν πατάς πάνω σε ένα πρωτάθλημα
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Άνοιγμα: " + champ.name, Toast.LENGTH_SHORT).show();

                // ΠΑΡΑΔΕΙΓΜΑ:
                 Intent intent = new Intent(v.getContext(), MainActivity.class);
                 intent.putExtra("CHAMPIONSHIP_ID", champ.id);
                 v.getContext().startActivity(intent);

            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;

            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_champ_name);
            }
        }
    }
}