package com.example.capstone2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        // Delay for 2 seconds before opening MainActivity
        progressBar.postDelayed(this::startMainActivity, 2000);
    }

    private void startMainActivity() {
        progressBar.setVisibility(View.GONE);
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }
}
