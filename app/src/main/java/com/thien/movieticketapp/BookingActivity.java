package com.thien.movieticketapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.thien.movieticketapp.adapters.SeatAdapter;
import com.thien.movieticketapp.adapters.ShowtimeAdapter;
import com.thien.movieticketapp.models.Movie;
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
    private List<Seat> seatList;
    private List<Showtime> showtimeList;
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

        if (movieId == null) {
            Toast.makeText(this, "Không tìm thấy ID phim!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvMovieTitle = findViewById(R.id.tvBookingMovieTitle);
        tvSelectedSeats = findViewById(R.id.tvSelectedSeats);
        btnConfirm = findViewById(R.id.btnConfirmBooking);
        rvSeats = findViewById(R.id.rvSeats);
        rvShowtimes = findViewById(R.id.rvShowtimes);

        // Setup Movie Title
        db.collection("movies").document(movieId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) tvMovieTitle.setText(doc.getString("title"));
        });

        // Setup Showtimes
        showtimeList = new ArrayList<>();
        showtimeAdapter = new ShowtimeAdapter(showtimeList, showtime -> {
            selectedShowtime = showtime;
            selectedSeatLabels.clear();
            updateSelectedSeatsUI();
            initSeats(); // Reset lại ghế khi đổi suất chiếu
            fetchOccupiedSeats();
        });
        
        // Quan trọng: Đảm bảo LayoutManager được set đúng
        rvShowtimes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvShowtimes.setAdapter(showtimeAdapter);

        fetchShowtimes();

        // Setup Seats
        initSeats();
        seatAdapter = new SeatAdapter(seatList, seat -> {
            if (selectedShowtime == null) {
                Toast.makeText(this, "Vui lòng chọn suất chiếu trước", Toast.LENGTH_SHORT).show();
                return;
            }
            seat.setSelected(!seat.isSelected());
            updateSelectedSeatsUI();
            seatAdapter.notifyDataSetChanged();
        });
        rvSeats.setLayoutManager(new GridLayoutManager(this, 6));
        rvSeats.setAdapter(seatAdapter);

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

    private void fetchShowtimes() {
        // Tạm thời bỏ .orderBy("dateTime") để không bị lỗi thiếu Index
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
                if (showtimeList.isEmpty()) {
                    Toast.makeText(this, "Phim này hiện chưa có suất chiếu", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("BookingActivity", "Lỗi tải suất chiếu: " + e.getMessage());
                Toast.makeText(this, "Lỗi kết nối dữ liệu!", Toast.LENGTH_SHORT).show();
            });
    }

    private void initSeats() {
        seatList = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E", "F", "G"};
        for (String row : rows) {
            for (int i = 1; i <= 6; i++) {
                seatList.add(new Seat(row + i));
            }
        }
        if (seatAdapter != null) seatAdapter.notifyDataSetChanged();
    }

    private void fetchOccupiedSeats() {
        if (selectedShowtime == null) return;

        db.collection("tickets")
            .whereEqualTo("showtimeId", selectedShowtime.getId())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Set<String> occupiedLabels = new HashSet<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Ticket ticket = doc.toObject(Ticket.class);
                    occupiedLabels.add(ticket.getSeatNumber());
                }

                for (Seat seat : seatList) {
                    seat.setOccupied(occupiedLabels.contains(seat.getLabel()));
                }
                seatAdapter.notifyDataSetChanged();
            });
    }

    private void updateSelectedSeatsUI() {
        selectedSeatLabels.clear();
        for (Seat seat : seatList) {
            if (seat.isSelected()) selectedSeatLabels.add(seat.getLabel());
        }
        
        StringBuilder sb = new StringBuilder("Ghế chọn: ");
        for (int i = 0; i < selectedSeatLabels.size(); i++) {
            sb.append(selectedSeatLabels.get(i));
            if (i < selectedSeatLabels.size() - 1) sb.append(", ");
        }
        tvSelectedSeats.setText(sb.toString());
    }

    private void performBooking() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        for (String seatLabel : selectedSeatLabels) {
            String ticketId = db.collection("tickets").document().getId();
            Ticket ticket = new Ticket(ticketId, userId, selectedShowtime.getId(), seatLabel, new Date());
            db.collection("tickets").document(ticketId).set(ticket);
        }
        Toast.makeText(this, "Đặt vé thành công!", Toast.LENGTH_LONG).show();
        finish();
    }
}
