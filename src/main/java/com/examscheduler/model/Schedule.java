package com.examscheduler.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Schedule implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int examPeriodDays;
    private final int slotsPerDay;
    
    
    private List<Exam> exams;
    
    
    private Map<TimeSlot, List<Exam>> timeSlotMap;

    public Schedule(int examPeriodDays, int slotsPerDay) {
        if (examPeriodDays <= 0) throw new IllegalArgumentException("Days must be positive");
        if (slotsPerDay <= 0) throw new IllegalArgumentException("Slots must be positive");
        
        this.examPeriodDays = examPeriodDays;
        this.slotsPerDay = slotsPerDay;
        this.exams = new ArrayList<>();
        this.timeSlotMap = new HashMap<>();
    }

    public int getExamPeriodDays() {
        return examPeriodDays;
    }

    public int getSlotsPerDay() {
        return slotsPerDay;
    }

    public List<Exam> getExams() {
        return new ArrayList<>(exams); 
    }

    
    public void addExam(Exam exam) {
        if (exam != null) {
            this.exams.add(exam);
            if (exam.isScheduled()) {
                timeSlotMap.computeIfAbsent(exam.getTimeSlot(), k -> new ArrayList<>()).add(exam);
            }
        }
    }
    
    public boolean removeExam(Exam exam) {
        if (exam != null && exams.remove(exam)) {
            if (exam.isScheduled() && timeSlotMap.containsKey(exam.getTimeSlot())) {
                timeSlotMap.get(exam.getTimeSlot()).remove(exam);
            }
            return true;
        }
        return false;
    }

   
    public List<Exam> getScheduledExams() {
        return exams.stream()
                .filter(Exam::isScheduled)
                .collect(Collectors.toList());
    }

    
    public List<Exam> getExamsAtTimeSlot(TimeSlot timeSlot) {
        return timeSlotMap.getOrDefault(timeSlot, new ArrayList<>());
    }

    
    public void rebuildTimeSlotMap() {
        timeSlotMap.clear();
        for (Exam exam : exams) {
            if (exam.isScheduled()) {
                timeSlotMap.computeIfAbsent(exam.getTimeSlot(), k -> new ArrayList<>()).add(exam);
            }
        }
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "days=" + examPeriodDays +
                ", slots=" + slotsPerDay +
                ", totalExams=" + exams.size() +
                ", scheduled=" + getScheduledExams().size() +
                '}';
    }
}
