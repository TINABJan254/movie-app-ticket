package com.thien.movieticketapp.models;

import java.util.Date;

public class Ticket {
    private String id;
    private String userId;
    private String showtimeId;
    private String seatNumber;
    private Date purchaseTime;

    public Ticket() {}

    public Ticket(String id, String userId, String showtimeId, String seatNumber, Date purchaseTime) {
        this.id = id;
        this.userId = userId;
        this.showtimeId = showtimeId;
        this.seatNumber = seatNumber;
        this.purchaseTime = purchaseTime;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getShowtimeId() { return showtimeId; }
    public void setShowtimeId(String showtimeId) { this.showtimeId = showtimeId; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public Date getPurchaseTime() { return purchaseTime; }
    public void setPurchaseTime(Date purchaseTime) { this.purchaseTime = purchaseTime; }
}
