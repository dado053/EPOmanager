package com.example.epostats.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.epostats.R;
import com.example.epostats.adapters.MatchAdapter;
import com.example.epostats.models.Match;
import com.example.epostats.network.ApiService;
import com.example.epostats.network.RetrofitClient;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MatchAdapter adapter;
    private ApiService apiService;
    private boolean isAdmin;
    private int championshipId = 1;

    private TextView tvChampionshipTitle;
    private String championshipName = "Πρόγραμμα Αγώνων";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvChampionshipTitle = findViewById(R.id.tvChampionshipTitle);

        if (getIntent() != null) {
            isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
            championshipId = getIntent().getIntExtra("CHAMPIONSHIP_ID", 1);
            championshipName = getIntent().getStringExtra("CHAMPIONSHIP_NAME");
        }

        if (tvChampionshipTitle != null && championshipName != null) {
            tvChampionshipTitle.setText(championshipName);
        }

        // ΔΙΑΓΝΩΣΤΙΚΟ TOAST: Θα σου λέει ποιο ID ζητάει από τον server!
        //Toast.makeText(this, "Ζητείται το Championship ID: " + championshipId, Toast.LENGTH_SHORT).show();

        recyclerView = findViewById(R.id.recyclerViewMatches);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        Button btnOpenStandings = findViewById(R.id.btnOpenStandings);
        if (btnOpenStandings != null) {
            btnOpenStandings.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, StandingsActivity.class);
                intent.putExtra("CHAMPIONSHIP_ID", championshipId); // ΠΕΡΝΑΜΕ ΤΟ ID ΣΤΗ ΒΑΘΜΟΛΟΓΙΑ!
                startActivity(intent);
            });
        }

        apiService = RetrofitClient.getClient().create(ApiService.class);
        loadMatches();
    }

    private void loadMatches() {
        if (apiService == null) return;
        apiService.getMatches(championshipId).enqueue(new Callback<List<Match>>() {
            @Override
            public void onResponse(Call<List<Match>> call, Response<List<Match>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter = new MatchAdapter(MainActivity.this, response.body());
                    if (recyclerView != null) recyclerView.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<List<Match>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Σφάλμα δικτύου: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}