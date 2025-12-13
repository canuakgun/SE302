package com.examscheduler.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Exam implements Serializable {
    private Course course;
    private TimeSlot timeSlot;
    private Classroom classroom;
    private Integer customStudentCount = null;
    private List<Student> assignedStudents = null;

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
        // Eğer özel bir alt küme atandıysa onu döndür (Bölünmüş sınav)
        if (assignedStudents != null) {
            return assignedStudents;
        }
        // Yoksa dersin ana listesini döndür (Normal sınav)
        return course.getEnrolledStudents();
    }

    public int getStudentCount() {
        // Özel sayı varsa onu, yoksa liste boyutunu döndür
        if (customStudentCount != null) return customStudentCount;
        if (assignedStudents != null) return assignedStudents.size();
        return course.getStudentCount();
    }

    public void setStudentCount(int count) {
        this.customStudentCount = count;
    }

    public boolean isScheduled() {
        return timeSlot != null && classroom != null;
    }

    public void setAssignedStudents(List<Student> students) {
        this.assignedStudents = new ArrayList<>(students);
        this.customStudentCount = students.size(); // Sayıyı da sabitle
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
