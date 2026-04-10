package com.thien.movieticketapp.models;

public class Seat {
    private String label;
    private boolean isSelected;
    private boolean isOccupied;

    public Seat(String label) {
        this.label = label;
        this.isSelected = false;
        this.isOccupied = false;
    }

    public String getLabel() { return label; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }
}
