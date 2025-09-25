package com.example.capstone2;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.capstone2.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private String deviceQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        deviceQr = getIntent().getStringExtra("DEVICE_QR");
        if (deviceQr != null) {
            Toast.makeText(this, "Device QR: " + deviceQr, Toast.LENGTH_LONG).show();
        }

        // Load default Home fragment
        replaceFragment(new HomeFragment());

        // Handle bottom navigation
        binding.bottomNavigationView.setBackground(null);
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (itemId == R.id.about) {
                replaceFragment(new AboutFragment());
            } else if (itemId == R.id.history) {
                replaceFragment(new HistoryFragment());
            } else if (itemId == R.id.settings) {
                replaceFragment(new SettingsFragment());
            }
            return true;
        });

        FloatingActionButton fab = findViewById(R.id.fab_refresh);
        fab.setOnClickListener(v -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
            if (currentFragment != null && !(currentFragment instanceof SettingsFragment)) {
                getSupportFragmentManager().beginTransaction()
                        .detach(currentFragment)
                        .attach(currentFragment)
                        .commit();
            }
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
    
}
