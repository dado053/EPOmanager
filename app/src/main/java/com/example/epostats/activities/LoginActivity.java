package com.example.epostats.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.epostats.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressBar loginProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Αρχικοποίηση των Views σύμφωνα με το νέο XML
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Έλεγχος εγκυρότητας πεδίων
            if (username.isEmpty()) {
                etUsername.setError("Συμπληρώστε Username");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Συμπληρώστε Password");
                return;
            }

            // Εμφάνιση του premium loading animation
            loginProgressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);

            // Δημιουργία του Intent για την επόμενη οθόνη (Επιλογή Πρωταθλήματος)
            Intent intent = new Intent(LoginActivity.this, ChampionshipActivity.class);

            // ΕΞΥΠΝΟΣ ΕΛΕΓΧΟΣ ADMIN
            if (username.equalsIgnoreCase("admin") && password.equals("1234")) {
                intent.putExtra("IS_ADMIN", true); // Κλειδώνει το true για τον διαχειριστή!
                Toast.makeText(LoginActivity.this, "Σύνδεση ως Διαχειριστής Επιτυχής!", Toast.LENGTH_SHORT).show();
            } else {
                intent.putExtra("IS_ADMIN", false); // Απλός χρήστης
                Toast.makeText(LoginActivity.this, "Σύνδεση ως Χρήστης Επιτυχής!", Toast.LENGTH_SHORT).show();
            }

            // Άνοιγμα της εφαρμογής
            startActivity(intent);
            finish();
        });
    }
}