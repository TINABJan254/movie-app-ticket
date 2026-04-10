package com.thien.movieticketapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.thien.movieticketapp.adapters.SeatAdapter;
import com.thien.movieticketapp.adapters.ShowtimeAdapter;
import com.thien.movieticketapp.models.Seat;
import com.thien.movieticketapp.models.Showtime;
import com.thien.movieticketapp.models.Ticket;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookingActivity extends AppCompatActivity {
    private RecyclerView rvSeats, rvShowtimes;
    private SeatAdapter seatAdapter;
    private ShowtimeAdapter showtimeAdapter;
    private List<Seat> seatList = new ArrayList<>();
    private List<Showtime> showtimeList = new ArrayList<>();
    private List<String> selectedSeatLabels = new ArrayList<>();
    
    private TextView tvMovieTitle, tvSelectedSeats;
    private Button btnConfirm;
    
    private FirebaseFirestore db;
    private String movieId;
    private Showtime selectedShowtime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        db = FirebaseFirestore.getInstance();
        movieId = getIntent().getStringExtra("movieId");

        tvMovieTitle = findViewById(R.id.tvBookingMovieTitle);
        tvSelectedSeats = findViewById(R.id.tvSelectedSeats);
        btnConfirm = findViewById(R.id.btnConfirmBooking);
        rvSeats = findViewById(R.id.rvSeats);
        rvShowtimes = findViewById(R.id.rvShowtimes);

        showtimeAdapter = new ShowtimeAdapter(showtimeList, showtime -> {
            selectedShowtime = showtime;
            resetSeats();
            fetchOccupiedSeats();
            updateSelectedSeatsUI();
        });
        rvShowtimes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvShowtimes.setAdapter(showtimeAdapter);

        initSeatData(); 
        seatAdapter = new SeatAdapter(seatList, seat -> {
            if (selectedShowtime == null) {
                Toast.makeText(this, "Vui lòng chọn suất chiếu trước", Toast.LENGTH_SHORT).show();
                return;
            }
            seat.setSelected(!seat.isSelected());
            seatAdapter.notifyDataSetChanged();
            updateSelectedSeatsUI();
        });
        rvSeats.setLayoutManager(new GridLayoutManager(this, 6));
        rvSeats.setAdapter(seatAdapter);

        fetchMovieTitle();
        fetchShowtimes();

        btnConfirm.setOnClickListener(v -> {
            if (selectedShowtime == null) {
                Toast.makeText(this, "Chưa chọn suất chiếu", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedSeatLabels.isEmpty()) {
                Toast.makeText(this, "Chưa chọn ghế", Toast.LENGTH_SHORT).show();
                return;
            }
            performBooking();
        });
    }

    private void fetchMovieTitle() {
        db.collection("movies").document(movieId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) tvMovieTitle.setText(doc.getString("title"));
        });
    }

    private void fetchShowtimes() {
        db.collection("showtimes")
            .whereEqualTo("movieId", movieId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                showtimeList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Showtime st = doc.toObject(Showtime.class);
                    st.setId(doc.getId());
                    showtimeList.add(st);
                }
                showtimeAdapter.notifyDataSetChanged();
            });
    }

    private void initSeatData() {
        seatList.clear();
        String[] rows = {"A", "B", "C", "D", "E"};
        for (String row : rows) {
            for (int i = 1; i <= 6; i++) {
                seatList.add(new Seat(row + i));
            }
        }
    }

    private void resetSeats() {
        for (Seat seat : seatList) {
            seat.setSelected(false);
            seat.setOccupied(false);
        }
        if (seatAdapter != null) seatAdapter.notifyDataSetChanged();
    }

    private void fetchOccupiedSeats() {
        if (selectedShowtime == null) return;
        db.collection("tickets")
            .whereEqualTo("showtimeId", selectedShowtime.getId())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Set<String> occupied = new HashSet<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    occupied.add(doc.getString("seatNumber"));
                }
                for (Seat seat : seatList) {
                    seat.setOccupied(occupied.contains(seat.getLabel()));
                }
                seatAdapter.notifyDataSetChanged();
            });
    }

    private void updateSelectedSeatsUI() {
        selectedSeatLabels.clear();
        for (Seat seat : seatList) {
            if (seat.isSelected()) selectedSeatLabels.add(seat.getLabel());
        }
        tvSelectedSeats.setText("Ghế chọn: " + (selectedSeatLabels.isEmpty() ? "Chưa chọn" : String.join(", ", selectedSeatLabels)));
    }

    private void performBooking() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String movieTitle = tvMovieTitle.getText().toString();
        
        for (String seatLabel : selectedSeatLabels) {
            String ticketId = db.collection("tickets").document().getId();
            Ticket ticket = new Ticket(ticketId, userId, selectedShowtime.getId(), seatLabel, new Date());
            db.collection("tickets").document(ticketId).set(ticket);
        }

        // Gửi thông báo ngay lập tức
        sendBookingNotification(movieTitle, String.join(", ", selectedSeatLabels));

        Toast.makeText(this, "Đặt vé thành công!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void sendBookingNotification(String movieName, String seats) {
        String channelId = "movie_reminder_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Thông báo đặt vé", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Đặt vé thành công!")
                .setContentText("Bạn đã đặt vé phim " + movieName + " tại ghế: " + seats)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
