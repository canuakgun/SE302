package com.examscheduler.model;

import java.io.Serializable;
import java.util.List;

public class Exam implements Serializable {
    private Course course;
    private TimeSlot timeSlot;
    private Classroom classroom;

    public Exam(Course course) {
        this.course = course;
    }

    public Course getCourse() {
        return course;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public void setClassroom(Classroom classroom) {
        this.classroom = classroom;
    }

    public List<Student> getEnrolledStudents() {
        return course.getEnrolledStudents();
    }

    public int getStudentCount() {
        return course.getStudentCount();
    }

    public boolean isScheduled() {
        return timeSlot != null && classroom != null;
    }

    @Override
    public String toString() {
        return "Exam{" +
                "course=" + course.getCourseCode() +
                ", timeSlot=" + (timeSlot != null ? timeSlot : "Unassigned") +
                ", classroom=" + (classroom != null ? classroom.getClassroomID() : "Unassigned") +
                '}';
    }
}
