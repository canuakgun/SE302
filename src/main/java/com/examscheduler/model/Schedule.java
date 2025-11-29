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

    @Override
    public String toString() {
        return "Schedule{" +
                "days=" + examPeriodDays +
                ", slotsPerDay=" + slotsPerDay +
                ", totalExams=" + exams.size() +
                '}';
    }

    
}
