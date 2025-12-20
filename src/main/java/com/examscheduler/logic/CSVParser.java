package com.examscheduler.logic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.examscheduler.model.Classroom;
import com.examscheduler.model.Course;
import com.examscheduler.model.Exam;
import com.examscheduler.model.Student;

/**
 * CSVParser - CSV dosyalarını okur VE günceller.
 * Düzeltme: Attendance.csv dosyasının çok satırlı yapısını destekler.
 */
public class CSVParser {

    public static class CSVParseException extends Exception {
        public CSVParseException(String message) {
            super(message);
        }

        public CSVParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // READING METHODS

    public static List<Student> parseStudents(String filePath) throws CSVParseException {
        List<Student> students = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty())
                    students.add(new Student(line.trim()));
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
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    courses.add(new Course(parts[0].trim(), parts[1].trim(), 1));
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
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                String[] parts = line.split(";");
                if (parts.length == 2) {
                    try {
                        classrooms.add(new Classroom(parts[0].trim(), Integer.parseInt(parts[1].trim())));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            throw new CSVParseException("Error reading classrooms file", e);
        }
        return classrooms;
    }

    /**
     * Result object for attendance parsing validation
     */
    public static class AttendanceValidationResult {
        private final List<String> missingStudents = new ArrayList<>();
        private final List<String> missingCourses = new ArrayList<>();
        private int totalEnrollments = 0;

        public List<String> getMissingStudents() {
            return missingStudents;
        }

        public List<String> getMissingCourses() {
            return missingCourses;
        }

        public int getTotalEnrollments() {
            return totalEnrollments;
        }

        public boolean hasWarnings() {
            return !missingStudents.isEmpty() || !missingCourses.isEmpty();
        }
    }

    public static AttendanceValidationResult parseAttendanceLists(String filePath, List<Student> students,
            List<Course> courses)
            throws CSVParseException {

        AttendanceValidationResult validationResult = new AttendanceValidationResult();

        Map<String, Student> studentMap = new HashMap<>();
        for (Student s : students) {
            if (s != null && s.getStudentID() != null) {
                studentMap.put(s.getStudentID().toLowerCase(), s);
            }
        }

        Map<String, Course> courseMap = new HashMap<>();
        for (Course c : courses) {
            if (c != null && c.getCourseCode() != null) {
                courseMap.put(c.getCourseCode().toLowerCase(), c);
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();

            // Check if first line is a valid Course Code or Header
            if (line != null) {
                String trimmed = line.trim().toLowerCase();
                boolean isHeader = !courseMap.containsKey(trimmed);

                if (isHeader) {
                    // It's a header, skip it and read next line
                    line = reader.readLine();
                }
            }

            while (line != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    line = reader.readLine();
                    continue;
                }

                String courseCode = line;
                Course targetCourse = courseMap.get(courseCode.toLowerCase());

                String studentListLine = reader.readLine();
                if (studentListLine == null)
                    break;

                if (targetCourse != null) {
                    String cleanLine = studentListLine.trim().replace("[", "").replace("]", "").replace("'", "");
                    if (!cleanLine.isEmpty()) {
                        String[] studentIDs = cleanLine.split(",");

                        for (String stdId : studentIDs) {
                            String id = stdId.trim();
                            if (id.isEmpty())
                                continue;

                            Student found = studentMap.get(id.toLowerCase());
                            if (found != null) {
                                if (!targetCourse.getEnrolledStudents().contains(found)) {
                                    targetCourse.addStudent(found);
                                    found.addCourse(targetCourse);
                                    validationResult.totalEnrollments++;
                                }
                            } else {
                                if (!validationResult.missingStudents.contains(id)) {
                                    validationResult.missingStudents.add(id);
                                }
                            }
                        }
                    }
                } else {
                    // Course not found in courses.csv
                    if (!validationResult.missingCourses.contains(courseCode)) {
                        validationResult.missingCourses.add(courseCode);
                    }
                }

                // Read next course line for next iteration
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new CSVParseException("Error parsing attendance", e);
        }

        return validationResult;
    }

    // SCHEDULE PARSING (EXPORTED CSV FILE)
    public static List<Exam> parseSchedule(File file, DataManager dm, List<String> timeSlotLabels) throws IOException {
        List<Exam> loadedExams = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            reader.mark(1000);
            String firstLine = reader.readLine();
            if (firstLine != null && !firstLine.toLowerCase().startsWith("examid")) {
                reader.reset();
            }

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                String[] parts = line.split(",");
                if (parts.length < 5)
                    continue;

                try {
                    String courseCode = parts[1].trim();
                    int day = Integer.parseInt(parts[2].trim());
                    String timeSlotStr = parts[3].trim();
                    String roomId = parts[4].trim();

                    int importedStudentCount = -1;
                    if (parts.length >= 6) {
                        try {
                            importedStudentCount = Integer.parseInt(parts[5].trim());
                        } catch (NumberFormatException e) {
                        }
                    }

                    Course course = dm.getCourseByCode(courseCode);
                    Classroom room = dm.getClassroomByID(roomId);

                    if (course != null && room != null) {
                        int slotIndex = 1;
                        for (int i = 0; i < timeSlotLabels.size(); i++) {
                            if (timeSlotLabels.get(i).equalsIgnoreCase(timeSlotStr)) {
                                slotIndex = i + 1;
                                break;
                            }
                        }
                        if (slotIndex == 1 && timeSlotStr.toLowerCase().startsWith("slot ")) {
                            try {
                                slotIndex = Integer.parseInt(timeSlotStr.substring(5).trim());
                            } catch (Exception ignored) {
                            }
                        }
                        Exam exam = new Exam(course);
                        exam.setTimeSlot(new com.examscheduler.model.TimeSlot(day, slotIndex));
                        exam.setClassroom(room);
                        if (importedStudentCount != -1) {
                            exam.setStudentCount(importedStudentCount);
                        }

                        loadedExams.add(exam);
                    }
                } catch (Exception e) {
                    System.err.println("Skipping invalid line: " + line + " (" + e.getMessage() + ")");
                }
            }
        }
        return loadedExams;
    }

    // WRITING / UPDATING METHODS

    public static void updateStudentFile(File file, List<Student> students) {
        if (file == null)
            return;
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
        if (file == null)
            return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("CourseCode,CourseName");
            writer.newLine();
            for (Course c : courses) {
                String line = String.format("%s,%s", c.getCourseCode(), c.getCourseName());
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error updating course file: " + e.getMessage());
        }
    }

    public static void updateClassroomFile(File file, List<Classroom> classrooms) {
        if (file == null)
            return;
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

    public static void updateAttendanceFile(File file, List<Course> courses) {
        if (file == null)
            return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Course course : courses) {
                writer.write(course.getCourseCode());
                writer.newLine();

                List<String> studentIds = new ArrayList<>();
                for (Student s : course.getEnrolledStudents()) {
                    studentIds.add("'" + s.getStudentID() + "'");
                }

                writer.write("[" + String.join(", ", studentIds) + "]");
                writer.newLine();
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error updating attendance file: " + e.getMessage());
        }
    }
}
