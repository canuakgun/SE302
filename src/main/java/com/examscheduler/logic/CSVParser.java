package com.examscheduler.logic;

import com.examscheduler.model.Student;
import com.examscheduler.model.Course;
import com.examscheduler.model.Classroom;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CSVParser - CSV dosyalarını okur VE günceller.
 * Düzeltme: Attendance.csv dosyasının çok satırlı yapısını destekler.
 */
public class CSVParser {

    public static class CSVParseException extends Exception {
        public CSVParseException(String message) { super(message); }
        public CSVParseException(String message, Throwable cause) { super(message, cause); }
    }

    // ==================== OKUMA METOTLARI ====================

    public static List<Student> parseStudents(String filePath) throws CSVParseException {
        List<Student> students = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Header atla
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) students.add(new Student(line.trim()));
            }
        } catch (IOException e) {
            throw new CSVParseException("Error reading students file", e);
        }
        return students;
    }

    public static List<Course> parseCourses(String filePath) throws CSVParseException {
        List<Course> courses = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Header atla
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    courses.add(new Course(parts[0].trim(), parts[1].trim(), parts[2].trim(), 1));
                } else if (parts.length >= 1) {
                    courses.add(new Course(parts[0].trim()));
                }
            }
        } catch (IOException e) {
            throw new CSVParseException("Error reading courses file", e);
        }
        return courses;
    }

    public static List<Classroom> parseClassrooms(String filePath) throws CSVParseException {
        List<Classroom> classrooms = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Header atla
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(";");
                if (parts.length == 2) {
                    try {
                        classrooms.add(new Classroom(parts[0].trim(), Integer.parseInt(parts[1].trim())));
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            throw new CSVParseException("Error reading classrooms file", e);
        }
        return classrooms;
    }
    
    /**
     * DÜZELTİLMİŞ METOT: Çok satırlı formatı destekler.
     * Satır 1: CourseCode
     * Satır 2: [Std1, Std2, ...]
     */
    public static void parseAttendanceLists(String filePath, List<Student> students, List<Course> courses) throws CSVParseException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Header varsa atla, yoksa ilk satırı oku
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 1. Satır: Ders Kodu
                String courseCode = line;
                Course targetCourse = null;
                for (Course c : courses) {
                    if (c.getCourseCode().equalsIgnoreCase(courseCode)) {
                        targetCourse = c; break;
                    }
                }

                // 2. Satır: Öğrenci Listesi
                line = reader.readLine();
                if (line == null) break; // Dosya sonu hatası önlemi
                
                if (targetCourse != null) {
                    // Köşeli parantezleri temizle: [Std1, Std2] -> Std1, Std2
                    String cleanLine = line.trim().replace("[", "").replace("]", "").replace("'", "");
                    if (!cleanLine.isEmpty()) {
                        String[] studentIDs = cleanLine.split(",");
                        
                        for (String stdId : studentIDs) {
                            String id = stdId.trim();
                            if (id.isEmpty()) continue;

                            // Öğrenciyi bul ve derse ekle
                            for (Student s : students) {
                                if (s.getStudentID().equalsIgnoreCase(id)) {
                                    if (!targetCourse.getEnrolledStudents().contains(s)) {
                                        targetCourse.addStudent(s);
                                        s.addCourse(targetCourse);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new CSVParseException("Error parsing attendance", e);
        }
    }

    // ==================== YAZMA / GÜNCELLEME METOTLARI ====================

    public static void updateStudentFile(File file, List<Student> students) {
        if (file == null) return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("StudentID");
            writer.newLine();
            for (Student s : students) {
                writer.write(s.getStudentID());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error updating student file: " + e.getMessage());
        }
    }

    public static void updateCourseFile(File file, List<Course> courses) {
        if (file == null) return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("CourseCode,CourseName,Instructor");
            writer.newLine();
            for (Course c : courses) {
                String line = String.format("%s,%s,%s", c.getCourseCode(), c.getCourseName(), c.getInstructor());
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error updating course file: " + e.getMessage());
        }
    }

    public static void updateClassroomFile(File file, List<Classroom> classrooms) {
        if (file == null) return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("ClassroomID;Capacity");
            writer.newLine();
            for (Classroom c : classrooms) {
                writer.write(c.getClassroomID() + ";" + c.getCapacity());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error updating classroom file: " + e.getMessage());
        }
    }
}
