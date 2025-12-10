package com.examscheduler.model;

import java.io.Serializable;
import java.util.Objects;


public class TimeSlot implements Comparable<TimeSlot>, Serializable {
    private static final long serialVersionUID = 1L;
    
    private int day;
    private int slotNumber;

    public TimeSlot(int day, int slotNumber) {
        if (day <= 0) {
            throw new IllegalArgumentException("Day must be positive");
        }
        if (slotNumber <= 0) {
            throw new IllegalArgumentException("Slot number must be positive");
        }
        this.day = day;
        this.slotNumber = slotNumber;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        if (day > 0) {
            this.day = day;
        }
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
        if (slotNumber > 0) {
            this.slotNumber = slotNumber;
        }
    }

    /**
     * User Requirement 5: Check if two time slots are consecutive
     * @param other Another time slot
     * @return true if slots are back-to-back on the same day
     */
    public boolean isConsecutive(TimeSlot other) {
        if (other == null) return false;
        if (!isSameDay(other)) return false;
        return Math.abs(this.slotNumber - other.slotNumber) == 1;
    }

    public boolean isSameDay(TimeSlot other) {
        if (other == null) return false;
        return this.day == other.day;
    }

    @Override
    public int compareTo(TimeSlot other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare to null TimeSlot");
        }
        if (this.day != other.day) {
            return Integer.compare(this.day, other.day);
        }
        return Integer.compare(this.slotNumber, other.slotNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return day == timeSlot.day && slotNumber == timeSlot.slotNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, slotNumber);
    }

    @Override
    public String toString() {
        return "TimeSlot{" +
                "day=" + day +
                ", slot=" + slotNumber +
                '}';
    }

    public String toDisplayString() {
        return "Day " + day + ", Slot " + slotNumber;
    }
}
