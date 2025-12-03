package com.examscheduler.model;

import java.io.Serializable;
import java.util.Objects;

public class TimeSlot implements Comparable<TimeSlot>, Serializable {
    private int day;
    private int slotNumber;

    public TimeSlot(int day, int slotNumber) {
        this.day = day;
        this.slotNumber = slotNumber;
    }

    public int getDay() {
        return day;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

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
                ", slotNumber=" + slotNumber +
                '}';
    }
}
