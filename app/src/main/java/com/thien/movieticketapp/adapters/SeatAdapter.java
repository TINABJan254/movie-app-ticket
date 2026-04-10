package com.thien.movieticketapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.thien.movieticketapp.R;
import com.thien.movieticketapp.models.Seat;

import java.util.List;

public class SeatAdapter extends RecyclerView.Adapter<SeatAdapter.SeatViewHolder> {
    private List<Seat> seatList;
    private OnSeatClickListener listener;

    public interface OnSeatClickListener {
        void onSeatClick(Seat seat);
    }

    public SeatAdapter(List<Seat> seatList, OnSeatClickListener listener) {
        this.seatList = seatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SeatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seat, parent, false);
        return new SeatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeatViewHolder holder, int position) {
        Seat seat = seatList.get(position);
        holder.tvLabel.setText(seat.getLabel());

        if (seat.isOccupied()) {
            holder.ivSeat.setImageResource(R.drawable.ic_seat_occupied);
        } else if (seat.isSelected()) {
            holder.ivSeat.setImageResource(R.drawable.ic_seat_selected);
        } else {
            holder.ivSeat.setImageResource(R.drawable.ic_seat_available);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!seat.isOccupied()) {
                listener.onSeatClick(seat);
            }
        });
    }

    @Override
    public int getItemCount() {
        return seatList.size();
    }

    static class SeatViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSeat;
        TextView tvLabel;

        public SeatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSeat = itemView.findViewById(R.id.ivSeat);
            tvLabel = itemView.findViewById(R.id.tvSeatLabel);
        }
    }
}
