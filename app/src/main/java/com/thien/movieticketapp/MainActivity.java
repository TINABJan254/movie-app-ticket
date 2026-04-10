package com.thien.movieticketapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
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

        // Nút nhấn để tạo sạch dữ liệu mới
        btnInitData.setOnClickListener(v -> forceInitializeData());

        fetchMovies();
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
                    Log.d("MainActivity", "Đã load " + movieList.size() + " phim");
                }
            });
    }

    private void forceInitializeData() {
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Đang khởi tạo dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();

        // 1. Tạo 2 phim mẫu
        Movie m1 = new Movie(null, "Avengers: Endgame", "Hành động, Viễn tưởng", "https://image.tmdb.org/t/p/w500/or06vSqzWBFscbePxbsGv7nh9pl.jpg", "Action", 181);
        Movie m2 = new Movie(null, "Spiderman: No Way Home", "Hành động, Phiêu lưu", "https://image.tmdb.org/t/p/w500/1g0vDwsas66W69bccXCYMdzUDOE.jpg", "Action", 148);

        // Add phim thứ nhất
        db.collection("movies").add(m1).addOnSuccessListener(doc1 -> {
            createShowtimesForMovie(doc1.getId());
            
            // Add phim thứ hai
            db.collection("movies").add(m2).addOnSuccessListener(doc2 -> {
                createShowtimesForMovie(doc2.getId());
                
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "KHỞI TẠO THÀNH CÔNG! Collection 'showtimes' đã được tạo.", Toast.LENGTH_LONG).show();
                fetchMovies(); // Reload UI
            }).addOnFailureListener(e -> Log.e("FirebaseError", e.getMessage()));
            
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Log.e("FirebaseError", e.getMessage());
            Toast.makeText(this, "Lỗi: " + e.getMessage() + ". Kiểm tra Rules của Firestore!", Toast.LENGTH_LONG).show();
        });
    }

    private void createShowtimesForMovie(String movieId) {
        WriteBatch batch = db.batch();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 14); // Bắt đầu từ 14:00
        cal.set(Calendar.MINUTE, 0);

        for (int i = 0; i < 4; i++) {
            String showtimeId = db.collection("showtimes").document().getId();
            Showtime st = new Showtime(showtimeId, movieId, "Rạp 01", cal.getTime(), 80000 + (i * 5000));
            batch.set(db.collection("showtimes").document(showtimeId), st);
            cal.add(Calendar.HOUR_OF_DAY, 3); // Mỗi suất cách nhau 3 tiếng
        }
        
        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("MainActivity", "Đã tạo suất chiếu cho movieId: " + movieId);
            }
        });
    }
}
