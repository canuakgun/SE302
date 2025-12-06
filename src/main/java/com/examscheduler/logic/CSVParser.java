package com.examscheduler.logic;

import com.examscheduler.model.Student;
import com.examscheduler.model.Course;
import com.examscheduler.model.Classroom;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVParser {

    // Students CSV Parser
    public static List<Student> parseStudents(String filePath) throws CSVParseException {
        List<Student> students = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Header atla

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty())
                    continue;

                try {
                    students.add(new Student(line));
                } catch (Exception e) {
                    throw new CSVParseException(
                            "Error parsing student at line " + lineNumber + ": " + line, e);
                }
            }
        } catch (IOException e) {
            throw new CSVParseException("Error reading file: " + filePath, e);
        }

        return students;
    }

    // Courses CSV Parser
    public static List<Course> parseCourses(String filePath) throws CSVParseException {
        List<Course> courses = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Header atla

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty())
                    continue;

                try {
                    courses.add(new Course(line));
                } catch (Exception e) {
                    throw new CSVParseException(
                            "Error parsing course at line " + lineNumber + ": " + line, e);
                }
            }
        } catch (IOException e) {
            throw new CSVParseException("Error reading file: " + filePath, e);
        }

        return courses;
    }

    // Classrooms CSV Parser (Format: ClassroomID;Capacity)
    public static List<Classroom> parseClassrooms(String filePath) throws CSVParseException {
        List<Classroom> classrooms = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Header atla

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty())
                    continue;

                try {
                    String[] parts = line.split(";");

                    if (parts.length != 2) {
                        throw new CSVParseException(
                                "Invalid format at line " + lineNumber +
                                        ". Expected format: ClassroomID;Capacity");
                    }

                    String classroomID = parts[0].trim();
                    int capacity = Integer.parseInt(parts[1].trim());

                    classrooms.add(new Classroom(classroomID, capacity));

                } catch (NumberFormatException e) {
                    throw new CSVParseException(
                            "Invalid capacity number at line " + lineNumber + ": " + line, e);
                } catch (CSVParseException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CSVParseException(
                            "Error parsing classroom at line " + lineNumber + ": " + line, e);
                }
            }
        } catch (IOException e) {
            throw new CSVParseException("Error reading file: " + filePath, e);
        }

        return classrooms;
    }

    // Attendance Lists Parser
    public static void parseAttendanceLists(String filePath,
            List<Student> students,
            List<Course> courses) throws CSVParseException {

        if (students == null || students.isEmpty()) {
            throw new CSVParseException("Students list is empty. Import students first!");
        }

        if (courses == null || courses.isEmpty()) {
            throw new CSVParseException("Courses list is empty. Import courses first!");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Header atla

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty())
                    continue;

                // Course kodu satırı
                String courseCode = line;
                Course course = findCourseByCode(courses, courseCode);

                if (course == null) {
                    throw new CSVParseException(
                            "Course not found at line " + lineNumber + ": " + courseCode);
                }

                // Student listesi satırı
                line = reader.readLine();
                lineNumber++;

                if (line == null) {
                    throw new CSVParseException(
                            "Expected student list after course code at line " + lineNumber);
                }

                List<String> studentIDs = parseStudentList(line);

                for (String studentID : studentIDs) {
                    Student student = findStudentByID(students, studentID);
                    if (student == null) {
                        System.err.println("Warning: Student not found: " + studentID);
                        continue;
                    }

                    course.addStudent(student);
                    student.addCourse(course);
                }
            }
        } catch (IOException e) {
            throw new CSVParseException("Error reading attendance file: " + filePath, e);
        }
    }

    // Helper Method
    private static List<String> parseStudentList(String studentListString) {
        List<String> studentIDs = new ArrayList<>();

        // Köşeli parantezleri ve tırnakları temizle
        studentListString = studentListString.trim()
                .replace("[", "")
                .replace("]", "")
                .replace("'", "")
                .replace("\"", "");

        // Virgülle ayır
        String[] ids = studentListString.split(",");

        for (String id : ids) {
            String trimmedID = id.trim();
            if (!trimmedID.isEmpty()) {
                studentIDs.add(trimmedID);
            }
        }

        return studentIDs;
    }

    // Helper Method - Student bulma
    private static Student findStudentByID(List<Student> students, String studentID) {
        for (Student student : students) {
            if (student.getStudentID().equals(studentID)) {
                return student;
            }
        }
        return null;
    }

    // Helper Method - Course bulma
    private static Course findCourseByCode(List<Course> courses, String courseCode) {
        for (Course course : courses) {
            if (course.getCourseCode().equals(courseCode)) {
                return course;
            }
        }
        return null;
    }

    // CSV Format Validation
    public static boolean validateCSVFormat(String filePath, CSVType type) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Header atla

            switch (type) {
                case STUDENTS:
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty())
                            continue;
                        if (!line.matches("Std_ID_\\d+")) {
                            return false;
                        }
                    }
                    break;

                case COURSES:
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty())
                            continue;
                        if (!line.matches("CourseCode_\\d+")) {
                            return false;
                        }
                    }
                    break;

                case CLASSROOMS:
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty())
                            continue;
                        String[] parts = line.split(";");
                        if (parts.length != 2)
                            return false;
                        try {
                            Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                    break;

                case ATTENDANCE:
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty())
                            continue;
                        lineCount++;
                        if (lineCount % 2 == 1) {
                            // Course code satırı
                            if (!line.matches("CourseCode_\\d+")) {
                                return false;
                            }
                        } else {
                            // Student list satırı
                            if (!line.startsWith("[") || !line.endsWith("]")) {
                                return false;
                            }
                        }
                    }
                    break;
            }
            return true;
        }
    }

    // Enum for CSV types
    public enum CSVType {
        STUDENTS, COURSES, CLASSROOMS, ATTENDANCE
    }

    // Custom Exception
    public static class CSVParseException extends Exception {
        public CSVParseException(String message) {
            super(message);
        }

        public CSVParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}