package com.thien.movieticketapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.thien.movieticketapp.models.Movie;

public class MovieDetailActivity extends AppCompatActivity {
    private ImageView ivPoster;
    private TextView tvTitle, tvGenre, tvDescription;
    private Button btnBookNow;
    private FirebaseFirestore db;
    private String movieId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        db = FirebaseFirestore.getInstance();
        movieId = getIntent().getStringExtra("movieId");

        ivPoster = findViewById(R.id.ivDetailPoster);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvGenre = findViewById(R.id.tvDetailGenre);
        tvDescription = findViewById(R.id.tvDetailDescription);
        btnBookNow = findViewById(R.id.btnBookNow);

        if (movieId != null) {
            fetchMovieDetails();
        }

        btnBookNow.setOnClickListener(v -> {
            Intent intent = new Intent(MovieDetailActivity.this, BookingActivity.class);
            intent.putExtra("movieId", movieId);
            startActivity(intent);
        });
    }

    private void fetchMovieDetails() {
        db.collection("movies").document(movieId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Movie movie = documentSnapshot.toObject(Movie.class);
                    if (movie != null) {
                        tvTitle.setText(movie.getTitle());
                        tvGenre.setText(movie.getGenre());
                        tvDescription.setText(movie.getDescription());
                        
                        Glide.with(this)
                                .load(movie.getPosterUrl())
                                .placeholder(android.R.drawable.ic_menu_report_image)
                                .into(ivPoster);
                    }
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải chi tiết phim", Toast.LENGTH_SHORT).show());
    }
}
