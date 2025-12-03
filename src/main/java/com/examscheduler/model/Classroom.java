package com.examscheduler.model;

import java.io.Serializable;
import java.util.Objects;

public class Classroom implements Serializable {
    private String classroomID;
    private int capacity;

    public Classroom(String classroomID, int capacity) {
        this.classroomID = classroomID;
        this.capacity = capacity;
    }

    public String getClassroomID() {
        return classroomID;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean canAccommodate(int studentCount) {
        return capacity >= studentCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Classroom classroom = (Classroom) o;
        return Objects.equals(classroomID, classroom.classroomID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classroomID);
    }

    @Override
    public String toString() {
        return "Classroom{" +
                "classroomID='" + classroomID + '\'' +
                ", capacity=" + capacity +
                '}';
    }
}
