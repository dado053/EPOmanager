package com.example.epostats;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.Intent;
import android.widget.Button;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLoginAdmin;
    Button btnLoginGuest;

    private final String ADMIN_PASSWORD = "admin123";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLoginAdmin = findViewById(R.id.btnLoginAdmin);
        btnLoginGuest = findViewById(R.id.btnLoginGuest);

        btnLoginAdmin.setOnClickListener(v -> loginAdmin());
        btnLoginGuest.setOnClickListener(v -> {

            getSharedPreferences("user_session", MODE_PRIVATE)
                    .edit()
                    .putBoolean("IS_ADMIN", false)
                    .apply();

            Intent intent = new Intent(LoginActivity.this, ChampionshipActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loginAdmin() {

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isAdmin = password.equals("admin123");

        if (!isAdmin) {
            Toast.makeText(this, "Invalid Password", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this,
                "Admin: " + isAdmin,
                Toast.LENGTH_LONG).show();
        // Save session
        getSharedPreferences("user_session", MODE_PRIVATE)
                .edit()
                .putBoolean("IS_ADMIN", true)
                .putString("USERNAME", username)
                .apply();

        Intent intent = new Intent(LoginActivity.this, ChampionshipActivity.class);
        startActivity(intent);
        finish();
    }
}