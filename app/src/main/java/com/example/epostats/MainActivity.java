package com.example.epostats;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MatchAdapter adapter;
    TextView tvWelcome;
    boolean isAdmin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvWelcome = findViewById(R.id.tvWelcome);
//        Intent intent = getIntent();
//        String username = getIntent().getStringExtra("USERNAME");
//        boolean isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        //tvWelcome.setText("Welcome " + username);
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN",false);

        if (isAdmin) {
            // admin features
        } else {
            // guest features
        }
        recyclerView = findViewById(R.id.recyclerViewMatches);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fetchMatches();
    }

    private void fetchMatches() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getMatches().enqueue(new Callback<List<Match>>() {
            @Override
            public void onResponse(Call<List<Match>> call, Response<List<Match>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    //edo kalo st matchadapter
                    adapter = new MatchAdapter(MainActivity.this, response.body());
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(MainActivity.this, "Λάθος απάντηση εξυπηρετητή", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Match>> call, Throwable t) {
                Log.e("API_ERROR", "Σφάλμα: " + t.getMessage());
            }
        });
    }
}