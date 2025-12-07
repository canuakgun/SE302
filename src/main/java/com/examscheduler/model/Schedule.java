package com.examscheduler.model;

import java.util.ArrayList;
import java.util.List;

public class Schedule {
    private final int examPeriodDays;
    private final int slotsPerDay;
    private List<Exam> exams;

    public Schedule(int examPeriodDays, int slotsPerDay) {
        this.examPeriodDays = examPeriodDays;
        this.slotsPerDay = slotsPerDay;
        this.exams = new ArrayList<>();
    }

    public List<Exam> getExams() {
        return exams;
    }

    public int getExamPeriodDays() {
        return examPeriodDays;
    }

    public int getSlotsPerDay() {
        return slotsPerDay;
    }

    public void addExam(Exam exam) {
        if (exam != null) {
            this.exams.add(exam);
        }
    }

    public boolean removeExam(Exam exam) {
        return this.exams.remove(exam);
    }

    public Exam getExamForCourse(Course course) {
        if (course == null) return null;
        
        for (Exam exam : exams) {
            if (exam.getCourse() != null && exam.getCourse().equals(course)) {
                return exam;
            }
        }
        return null;
    }

    public List<Exam> getExamsForDay(int day) {
        List<Exam> result = new ArrayList<>();
        for (Exam exam : exams) {
            // Sınavın bir saati atanmış mı? Ve günü tutuyor mu?
            if (exam.getTimeSlot() != null && exam.getTimeSlot().getDay() == day) {
                result.add(exam);
            }
        }
        return result;
    }

    public List<Exam> getExamsForTimeSlot(TimeSlot timeSlot) {
        List<Exam> result = new ArrayList<>();
        if (timeSlot == null) return result;

        for (Exam exam : exams) {
            // Slotlar eşit mi?
            if (timeSlot.equals(exam.getTimeSlot())) {
                result.add(exam);
            }
        }
        return result;
    }

    public List<Exam> getExamsForClassroom(Classroom classroom) {
        List<Exam> result = new ArrayList<>();
        if (classroom == null) return result;

        for (Exam exam : exams) {
            // Sınıflar eşit mi?
            if (classroom.equals(exam.getClassroom())) {
                result.add(exam);
            }
        }
        return result;
    }

    public List<String> validateConstraints() {
        // TODO: Logic Layer - This will be detailed with the ConstraintValidator.
        // For now, we return an empty list as if there was no error.
        return new ArrayList<>(); 
    }
    @Override
    public String toString() {
        return "Schedule{" +
                "days=" + examPeriodDays +
                ", slotsPerDay=" + slotsPerDay +
                ", totalExams=" + exams.size() +
                '}';
    }

    
}
