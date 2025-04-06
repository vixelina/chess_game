package com.chess;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chess.GameActivityManagers.GameStateManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button startButton = findViewById(R.id.button_start);
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        });

        Button resumeButton = findViewById(R.id.button_resume);
        resumeButton.setOnClickListener(v -> {
            if (GameStateManager.hasSavedGame(MainActivity.this)) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("resume", true);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "No saved game found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // allow resume button if game exists
        Button resumeButton = findViewById(R.id.button_resume);
        resumeButton.setEnabled(GameStateManager.hasSavedGame(this));
    }
}