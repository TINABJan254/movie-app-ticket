package com.thien.movieticketapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;
import com.thien.movieticketapp.adapters.MovieAdapter;
import com.thien.movieticketapp.models.Movie;
import com.thien.movieticketapp.models.Showtime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvMovies;
    private MovieAdapter adapter;
    private List<Movie> movieList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private ImageButton btnLogout;
    private Button btnInitData;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getFCMToken();
                } else {
                    Toast.makeText(this, "Bạn cần cấp quyền thông báo để nhận nhắc lịch chiếu", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Xin quyền thông báo
        askNotificationPermission();

        db = FirebaseFirestore.getInstance();
        rvMovies = findViewById(R.id.rvMovies);
        progressBar = findViewById(R.id.progressBar);
        btnLogout = findViewById(R.id.btnLogout);
        btnInitData = findViewById(R.id.btnInitData);

        adapter = new MovieAdapter(movieList, movie -> {
            Intent intent = new Intent(MainActivity.this, MovieDetailActivity.class);
            intent.putExtra("movieId", movie.getId());
            startActivity(intent);
        });

        rvMovies.setLayoutManager(new GridLayoutManager(this, 2));
        rvMovies.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        btnInitData.setOnClickListener(v -> forceInitializeData());

        fetchMovies();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                getFCMToken();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            getFCMToken();
        }
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                    return;
                }
                String token = task.getResult();
                Log.d("FCM_TOKEN", "Token của máy bạn: " + token);
                // Bạn có thể lưu token này vào Firestore để gửi thông báo riêng cho User này
            });
    }

    private void fetchMovies() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("movies").get()
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    movieList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Movie movie = document.toObject(Movie.class);
                        movie.setId(document.getId());
                        movieList.add(movie);
                    }
                    adapter.notifyDataSetChanged();
                }
            });
    }

    private void forceInitializeData() {
        progressBar.setVisibility(View.VISIBLE);
        Movie m1 = new Movie(null, "Avengers: Endgame", "Hành động, Viễn tưởng", "https://image.tmdb.org/t/p/w500/or06vSqzWBFscbePxbsGv7nh9pl.jpg", "Action", 181);
        db.collection("movies").add(m1).addOnSuccessListener(doc1 -> {
            createShowtimesForMovie(doc1.getId());
            progressBar.setVisibility(View.GONE);
            fetchMovies();
        });
    }

    private void createShowtimesForMovie(String movieId) {
        WriteBatch batch = db.batch();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 3; i++) {
            String showtimeId = db.collection("showtimes").document().getId();
            Showtime st = new Showtime(showtimeId, movieId, "Rạp 01", cal.getTime(), 80000);
            batch.set(db.collection("showtimes").document(showtimeId), st);
            cal.add(Calendar.HOUR_OF_DAY, 2);
        }
        batch.commit();
    }
}
