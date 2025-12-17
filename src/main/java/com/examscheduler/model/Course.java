package com.examscheduler.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    private String courseCode;
    private String courseName;
    private int examDurationSlots;
    private List<Student> enrolledStudents;

    public Course(String courseCode) {
        this(courseCode, "", 1);
    }

    public Course(String courseCode, String courseName, int examDurationSlots) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty");
        }
        this.courseCode = courseCode;
        this.courseName = courseName != null ? courseName : "";
        this.examDurationSlots = Math.max(1, examDurationSlots);
        this.enrolledStudents = new ArrayList<>();
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public List<Student> getEnrolledStudents() {
        return new ArrayList<>(enrolledStudents); // Defensive copy
    }

    public int getStudentCount() {
        return enrolledStudents.size();
    }

    public boolean addStudent(Student student) {
        if (student != null && !enrolledStudents.contains(student)) {
            enrolledStudents.add(student); // Doğrudan gerçek listeye ekler
            return true; // Eklendi
        }
        return false; // Zaten vardı veya null
    }

    public boolean removeStudent(Student student) {
        if (student == null)
            return false;

        // Doğrudan ID eşleşmesi yaparak sil
        return enrolledStudents.removeIf(s -> s.getStudentID().equals(student.getStudentID()));
    }

    public boolean hasStudent(Student student) {
        return enrolledStudents.contains(student);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Course course = (Course) o;
        return Objects.equals(courseCode, course.courseCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseCode);
    }

    @Override
    public String toString() {
        return "Course{" +
                "courseCode='" + courseCode + '\'' +
                ", studentCount=" + getStudentCount() +
                '}';
    }
}