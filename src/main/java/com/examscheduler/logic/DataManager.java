package com.examscheduler.logic;

import com.examscheduler.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.io.Serializable;

/**
 * DataManager - Singleton Pattern
 * Uygulama genelinde merkezi veri erişimi sağlar
 * Tüm Student, Course, Classroom ve Schedule verilerini saklar
 */
public class DataManager implements Serializable {
    private static DataManager instance;

    private List<Student> students;
    private List<Course> courses;
    private List<Classroom> classrooms;
    private Schedule schedule;

    // Private constructor for Singleton pattern
    private DataManager() {
        this.students = new ArrayList<>();
        this.courses = new ArrayList<>();
        this.classrooms = new ArrayList<>();
        this.schedule = null;
    }

    /**
     * Singleton instance getter
     * 
     * @return DataManager'ın tek instance'ı
     */
    public static DataManager getInstance() {
        if (instance == null) {
            synchronized (DataManager.class) {
                if (instance == null) {
                    instance = new DataManager();
                }
            }
        }
        return instance;
    }

    // ==================== SETTERS ====================

    public void setStudents(List<Student> students) {
        this.students = students != null ? students : new ArrayList<>();
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses != null ? courses : new ArrayList<>();
    }

    public void setClassrooms(List<Classroom> classrooms) {
        this.classrooms = classrooms != null ? classrooms : new ArrayList<>();
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public boolean addClassroom(Classroom classroom) {
        if (classroom != null && !this.classrooms.contains(classroom)) {
            return this.classrooms.add(classroom); // true döner
        }
        return false;
    }

    public void removeClassroom(Classroom classroom) {
        if (this.classrooms != null) {
            this.classrooms.remove(classroom);
        }
    }

    public String getDetailedStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("Students: ").append(students != null ? students.size() : 0).append("\n");
        sb.append("Courses: ").append(courses != null ? courses.size() : 0).append("\n");
        sb.append("Classrooms: ").append(classrooms != null ? classrooms.size() : 0).append("\n");
        sb.append("Total Enrollments: ").append(getTotalEnrollments()).append("\n");
        sb.append("Average Class Size: ").append(String.format("%.1f", getAverageClassSize())).append("\n");
        sb.append("Total Classroom Capacity: ").append(getTotalClassroomCapacity()).append("\n");
        sb.append("Schedule: ").append(schedule != null ? "Loaded" : "Not Loaded").append("\n");
        return sb.toString();
    }

    private int getTotalEnrollments() {
        if (courses == null)
            return 0;
        return courses.stream()
                .filter(Objects::nonNull)
                .mapToInt(Course::getStudentCount)
                .sum();
    }

    private double getAverageClassSize() {
        if (courses == null || courses.isEmpty())
            return 0.0;
        return (double) getTotalEnrollments() / courses.size();
    }

    private int getTotalClassroomCapacity() {
        if (classrooms == null)
            return 0;
        return classrooms.stream()
                .filter(Objects::nonNull)
                .mapToInt(Classroom::getCapacity)
                .sum();
    }

    // ==================== GETTERS ====================

    public List<Student> getStudents() {
        return students;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public List<Classroom> getClassrooms() {
        return classrooms;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    /**
     * ID'ye göre öğrenci bulur
     * 
     * @param studentID Öğrenci ID'si
     * @return Student nesnesi veya null
     */
    public Student getStudentByID(String studentID) {
        if (students == null)
            return null;

        for (Student student : students) {
            if (student.getStudentID().equals(studentID)) {
                return student;
            }
        }
        return null;
    }

    /**
     * Koda göre ders bulur
     * 
     * @param courseCode Ders kodu
     * @return Course nesnesi veya null
     */
    public Course getCourseByCode(String courseCode) {
        if (courses == null)
            return null;

        for (Course course : courses) {
            if (course.getCourseCode().equals(courseCode)) {
                return course;
            }
        }
        return null;
    }

    /**
     * ID'ye göre sınıf bulur
     * 
     * @param classroomID Sınıf ID'si
     * @return Classroom nesnesi veya null
     */
    public Classroom getClassroomByID(String classroomID) {
        if (classrooms == null)
            return null;

        for (Classroom classroom : classrooms) {
            if (classroom.getClassroomID().equals(classroomID)) {
                return classroom;
            }
        }
        return null;
    }

    // Tüm gerekli verilerin yüklenip yüklenmediğini kontrol eder
    // @return true ise tüm veriler yüklü

    public boolean isDataLoaded() {
        return students != null && !students.isEmpty() &&
                courses != null && !courses.isEmpty() &&
                classrooms != null && !classrooms.isEmpty();
    }

    // Tüm verileri temizler (yeni proje için)

    public void clearAllData() {
        this.students = new ArrayList<>();
        this.courses = new ArrayList<>();
        this.classrooms = new ArrayList<>();
        this.schedule = null;
    }

    @Override
    public String toString() {
        return "DataManager{" +
                "students=" + (students != null ? students.size() : 0) +
                ", courses=" + (courses != null ? courses.size() : 0) +
                ", classrooms=" + (classrooms != null ? classrooms.size() : 0) +
                ", schedule=" + (schedule != null ? "Loaded" : "Not Loaded") +
                '}';
    }
}