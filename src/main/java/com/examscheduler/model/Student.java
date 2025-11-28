package com.examscheduler.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Student {

    private final String studentID;
    private final List<Course> courses;

    public Student(String studentID) {
        this.studentID = studentID;
        this.courses = new ArrayList<>();
    }

    public String getStudentID() {
        return studentID;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void addCourse(Course course) {
        if (course != null) {
            this.courses.add(course);
        }
    }

    // If two student objects have the same ID, these students are considered the "same person".
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Student student = (Student) obj;
        return Objects.equals(studentID, student.studentID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentID);
    }

    @Override
    public String toString() {
        return "Student{" +
                "studentID='" + studentID + '\'' +
                ", coursesCount=" + courses.size() +
                '}';
    }
}