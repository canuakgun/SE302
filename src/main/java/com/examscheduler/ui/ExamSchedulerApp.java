package com.examscheduler.ui;

import com.examscheduler.logic.CSVParser;
import com.examscheduler.logic.DataManager;
import com.examscheduler.model.Classroom;
import com.examscheduler.model.Course;
import com.examscheduler.model.Exam;
import com.examscheduler.model.Schedule;
import com.examscheduler.model.Student;
import com.examscheduler.model.TimeSlot;

import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ExamSchedulerApp extends Application {

    private final DataManager dataManager = DataManager.getInstance();
    private final ObservableList<ExamEntry> exams = FXCollections.observableArrayList();
    private final ObservableList<String> messages = FXCollections.observableArrayList();

    private TableView<ExamEntry> table;
    private TextArea statsArea;
    private final Spinner<Integer> daysSpinner = new Spinner<>(1, 30, 5);
    private ListView<String> crView = new ListView<>();
    private ListView<String> tsView = new ListView<>();
    private Supplier<List<String>> getTimeSlotsFromUI;

    private DatePicker examStartDatePicker;
    private List<String> unplacedCourses = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("📅 Exam Scheduler - Enhanced with Student Portal");

        String primaryColor = "#0078D4";
        String secondaryColor = "#106EBE";
        String buttonStyle = "-fx-background-color: " + secondaryColor
                + "; -fx-text-fill: white; -fx-background-radius: 3; -fx-padding: 8px 16px; -fx-font-weight: bold; -fx-cursor: hand;";
        String toolBarStyle = "-fx-background-color: " + primaryColor
                + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);";
        String titleStyle = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + primaryColor + ";";

        MenuBar menuBar = createMenuBar(stage);
        ToolBar toolBar = new ToolBar();

        Button loadBtn = new Button("📁 Load Data");
        Button generateBtn = new Button("⚡ Generate Schedule");
        Button saveBtn = new Button("💾 Save Schedule");
        Button validateBtn = new Button("✓ Validate");
        Button exportBtn = new Button("📤 Export");
        Button studentPortalBtn = new Button("👤 Student Portal");
        loadBtn.setStyle(buttonStyle);
        generateBtn.setStyle(buttonStyle);
        saveBtn.setStyle(buttonStyle);
        validateBtn.setStyle(buttonStyle);
        exportBtn.setStyle(buttonStyle);
        studentPortalBtn.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8px 16px; -fx-font-weight: bold; -fx-cursor: hand;");
        toolBar.setStyle(toolBarStyle);

        toolBar.getItems().addAll(loadBtn, new Separator(), generateBtn, validateBtn, new Separator(),
                saveBtn, exportBtn, new Separator(), studentPortalBtn);

        loadBtn.setOnAction(e -> handleLoad(stage));
        generateBtn.setOnAction(e -> handleGenerateSchedule());
        saveBtn.setOnAction(e -> handleSave(stage));
        validateBtn.setOnAction(e -> handleValidate());
        exportBtn.setOnAction(e -> handleExport(stage));
        studentPortalBtn.setOnAction(e -> showStudentPortal(stage));

        VBox leftPane = createConfigurationPanel(buttonStyle);
        table = createExamTable(stage);

        Label scheduleTitle = new Label("📋 Exam Schedule");
        scheduleTitle.setStyle(titleStyle);

        VBox centerPane = new VBox(10, scheduleTitle, table);
        centerPane.setPadding(new Insets(15));
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox rightPane = createMessagesPanel(titleStyle);

        VBox topContainer = new VBox(menuBar, toolBar);
        BorderPane root = new BorderPane();
        root.setTop(topContainer);
        root.setLeft(leftPane);
        root.setCenter(centerPane);
        root.setRight(rightPane);

        messages.add("✓ System ready. Click 'Load Data' to import CSV files.");

        Scene sc = new Scene(root, 1400, 800);
        stage.setScene(sc);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.show();
    }

    private void showStudentPortal(Stage owner) {
        if (!dataManager.isDataLoaded() || dataManager.getSchedule() == null) {
            showError("No Schedule", "Please load data and generate schedule first.");
            return;
        }

        Stage loginStage = new Stage();
        loginStage.initOwner(owner);
        loginStage.initModality(Modality.APPLICATION_MODAL);
        loginStage.setTitle("🎓 Student Portal Login");

        ComboBox<String> studentCombo = new ComboBox<>();
        dataManager.getStudents().forEach(s -> studentCombo.getItems().add(s.getStudentID()));
        studentCombo.setPromptText("Select your Student ID");
        studentCombo.setPrefWidth(250);

        Button loginBtn = new Button("View My Schedule");
        loginBtn.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-font-size: 14px;");

        loginBtn.setOnAction(e -> {
            String studentId = studentCombo.getValue();
            if (studentId != null) {
                Student student = dataManager.getStudentByID(studentId);
                if (student != null) {
                    loginStage.close();
                    showStudentSchedule(owner, student);
                }
            } else {
                showWarning("Selection Required", "Please select a student ID.");
            }
        });

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));

        Label title = new Label("🎓 Student Portal");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0078D4;");

        Label subtitle = new Label("Select your student ID to view your exam schedule");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        layout.getChildren().addAll(title, subtitle, studentCombo, loginBtn);

        Scene scene = new Scene(layout, 450, 300);
        loginStage.setScene(scene);
        loginStage.show();
    }

    private void showStudentSchedule(Stage owner, Student student) {
        Stage scheduleStage = new Stage();
        scheduleStage.initOwner(owner);
        scheduleStage.setTitle("📅 My Exam Schedule - " + student.getStudentID());

        List<Exam> studentExams = getStudentExams(student);

        if (studentExams.isEmpty()) {
            showInfo("No Exams", "You have no scheduled exams.");
            return;
        }

        GridPane calendar = createCalendarView(studentExams);

        ListView<String> examList = createExamListView(studentExams);

        TextArea summary = new TextArea();
        summary.setEditable(false);
        summary.setPrefHeight(100);
        summary.setText(generateStudentSummary(student, studentExams));

        Button exportBtn = new Button("📄 Export My Schedule");
        exportBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 8px 16px;");
        exportBtn.setOnAction(e -> exportStudentSchedule(scheduleStage, student, studentExams));

        VBox calendarBox = new VBox(10);
        Label calTitle = new Label("📅 Calendar View");
        calTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        calendarBox.getChildren().addAll(calTitle, new ScrollPane(calendar));

        VBox listBox = new VBox(10);
        Label listTitle = new Label("📋 Exam List");
        listTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        listBox.getChildren().addAll(listTitle, examList);

        VBox summaryBox = new VBox(10);
        Label summaryTitle = new Label("📊 Summary");
        summaryTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        summaryBox.getChildren().addAll(summaryTitle, summary, exportBtn);

        SplitPane mainSplit = new SplitPane(calendarBox, listBox);
        mainSplit.setDividerPositions(0.6);

        BorderPane root = new BorderPane();
        root.setCenter(mainSplit);
        root.setBottom(summaryBox);
        BorderPane.setMargin(summaryBox, new Insets(10));

        Scene scene = new Scene(root, 1000, 700);
        scheduleStage.setScene(scene);
        scheduleStage.show();
    }

    private GridPane createCalendarView(List<Exam> studentExams) {
        GridPane calendar = new GridPane();
        calendar.setHgap(5);
        calendar.setVgap(5);
        calendar.setPadding(new Insets(10));
        calendar.setStyle("-fx-background-color: white;");

        int maxDay = studentExams.stream().mapToInt(e -> e.getTimeSlot().getDay()).max().orElse(5);
        List<String> timeSlots = getTimeSlotsFromUI.get();

        Label cornerLabel = new Label("Day / Time");
        cornerLabel.setStyle(
                "-fx-font-weight: bold; -fx-padding: 10px; -fx-background-color: #0078D4; -fx-text-fill: white;");
        calendar.add(cornerLabel, 0, 0);

        for (int slot = 0; slot < timeSlots.size(); slot++) {
            Label slotLabel = new Label(timeSlots.get(slot));
            slotLabel.setStyle(
                    "-fx-font-weight: bold; -fx-padding: 10px; -fx-background-color: #0078D4; -fx-text-fill: white;");
            slotLabel.setMinWidth(150);
            calendar.add(slotLabel, slot + 1, 0);
        }

        Map<String, Exam> examMap = new HashMap<>();
        for (Exam exam : studentExams) {
            String key = exam.getTimeSlot().getDay() + "-" + exam.getTimeSlot().getSlotNumber();
            examMap.put(key, exam);
        }

        LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                ? examStartDatePicker.getValue()
                : LocalDate.now();

        for (int day = 1; day <= maxDay; day++) {
            LocalDate currentDate = startDate.plusDays(day - 1);
            String dayLabel = "Day " + day + "\n" + currentDate.toString();

            Label dayLabelNode = new Label(dayLabel);
            dayLabelNode.setStyle("-fx-font-weight: bold; -fx-padding: 10px; -fx-background-color: #E3F2FD;");
            dayLabelNode.setMinHeight(80);
            calendar.add(dayLabelNode, 0, day);

            for (int slot = 1; slot <= timeSlots.size(); slot++) {
                String key = day + "-" + slot;
                VBox cell = new VBox(5);
                cell.setMinWidth(150);
                cell.setMinHeight(80);
                cell.setPadding(new Insets(5));
                cell.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-background-color: white;");

                if (examMap.containsKey(key)) {
                    Exam exam = examMap.get(key);
                    cell.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-background-color: #FFF9C4;");

                    Label courseLabel = new Label(exam.getCourse().getCourseCode());
                    courseLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #D84315;");

                    Label roomLabel = new Label("📍 " + exam.getClassroom().getClassroomID());
                    roomLabel.setStyle("-fx-font-size: 11px;");

                    Label courseNameLabel = new Label(exam.getCourse().getCourseName());
                    courseNameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
                    courseNameLabel.setWrapText(true);

                    cell.getChildren().addAll(courseLabel, roomLabel, courseNameLabel);
                }

                calendar.add(cell, slot, day);
            }
        }

        return calendar;
    }

    private ListView<String> createExamListView(List<Exam> studentExams) {
        ListView<String> listView = new ListView<>();
        List<String> timeSlots = getTimeSlotsFromUI.get();

        LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                ? examStartDatePicker.getValue()
                : LocalDate.now();

        studentExams.stream()
                .sorted(Comparator.comparing((Exam e) -> e.getTimeSlot().getDay())
                        .thenComparing(e -> e.getTimeSlot().getSlotNumber()))
                .forEach(exam -> {
                    LocalDate examDate = startDate.plusDays(exam.getTimeSlot().getDay() - 1);
                    String timeSlot = "";
                    try {
                        timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
                    } catch (Exception e) {
                        timeSlot = "Slot " + exam.getTimeSlot().getSlotNumber();
                    }

                    String entry = String.format("📅 %s | ⏰ %s | 📚 %s (%s) | 📍 Room %s",
                            examDate.toString(),
                            timeSlot,
                            exam.getCourse().getCourseCode(),
                            exam.getCourse().getCourseName(),
                            exam.getClassroom().getClassroomID());
                    listView.getItems().add(entry);
                });

        return listView;
    }

    private String generateStudentSummary(Student student, List<Exam> studentExams) {
        int totalExams = studentExams.size();
        Map<Integer, Long> examsPerDay = studentExams.stream()
                .collect(Collectors.groupingBy(e -> e.getTimeSlot().getDay(), Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("Student ID: ").append(student.getStudentID()).append("\n");
        sb.append("Total Exams: ").append(totalExams).append("\n");
        sb.append("\nExams per Day:\n");
        examsPerDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sb.append("  Day ").append(entry.getKey())
                        .append(": ").append(entry.getValue()).append(" exam(s)\n"));

        return sb.toString();
    }

    private void exportStudentSchedule(Stage owner, Student student, List<Exam> studentExams) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export My Schedule");
        chooser.setInitialFileName("my_exam_schedule_" + student.getStudentID() + ".txt");
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file)) {
                pw.println("==============================================");
                pw.println("        EXAM SCHEDULE - " + student.getStudentID());
                pw.println("==============================================\n");

                List<String> timeSlots = getTimeSlotsFromUI.get();
                LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                        ? examStartDatePicker.getValue()
                        : LocalDate.now();

                studentExams.stream()
                        .sorted(Comparator.comparing((Exam e) -> e.getTimeSlot().getDay())
                                .thenComparing(e -> e.getTimeSlot().getSlotNumber()))
                        .forEach(exam -> {
                            LocalDate examDate = startDate.plusDays(exam.getTimeSlot().getDay() - 1);
                            String timeSlot = "";
                            try {
                                timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
                            } catch (Exception e) {
                                timeSlot = "Slot " + exam.getTimeSlot().getSlotNumber();
                            }

                            pw.println("Date: " + examDate);
                            pw.println("Time: " + timeSlot);
                            pw.println("Course: " + exam.getCourse().getCourseCode() + " - "
                                    + exam.getCourse().getCourseName());
                            pw.println("Room: " + exam.getClassroom().getClassroomID());
                            pw.println("----------------------------------------------\n");
                        });

                pw.println("Total Exams: " + studentExams.size());
                showInfo("Export Success", "Your schedule has been exported to " + file.getName());
            } catch (Exception e) {
                showError("Export Failed", e.getMessage());
            }
        }
    }

    private List<Exam> getStudentExams(Student student) {
        List<Exam> studentExams = new ArrayList<>();

        if (dataManager.getSchedule() != null) {
            for (Exam exam : dataManager.getSchedule().getExams()) {
                if (exam.isScheduled() && exam.getEnrolledStudents().contains(student)) {
                    studentExams.add(exam);
                }
            }
        }

        return studentExams;
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem loadItem = new MenuItem("Load Data...");
        MenuItem saveItem = new MenuItem("Save Schedule");
        MenuItem exportItem = new MenuItem("Export...");
        MenuItem exitItem = new MenuItem("Exit");

        loadItem.setOnAction(e -> handleLoad(stage));
        saveItem.setOnAction(e -> handleSave(stage));
        exportItem.setOnAction(e -> handleExport(stage));
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(loadItem, saveItem, exportItem, new SeparatorMenuItem(), exitItem);

        Menu editMenu = new Menu("Edit");
        MenuItem manageCourses = new MenuItem("Manage Courses...");
        MenuItem manageStudents = new MenuItem("Manage Students...");
        MenuItem manageClassrooms = new MenuItem("Manage Classrooms...");

        manageCourses.setOnAction(e -> showManageCourses(stage));
        manageStudents.setOnAction(e -> showManageStudents(stage));
        manageClassrooms.setOnAction(e -> showManageClassrooms(stage));

        editMenu.getItems().addAll(manageCourses, manageStudents, manageClassrooms);

        Menu scheduleMenu = new Menu("Schedule");
        MenuItem generateItem = new MenuItem("Generate Schedule");
        MenuItem validateItem = new MenuItem("Validate Schedule");
        MenuItem conflictReport = new MenuItem("Conflict Report");

        generateItem.setOnAction(e -> handleGenerateSchedule());
        validateItem.setOnAction(e -> handleValidate());
        conflictReport.setOnAction(e -> showConflictReport());

        scheduleMenu.getItems().addAll(generateItem, validateItem, conflictReport);

        Menu studentMenu = new Menu("Students");
        MenuItem studentPortalItem = new MenuItem("Student Portal...");
        studentPortalItem.setOnAction(e -> showStudentPortal(stage));
        studentMenu.getItems().add(studentPortalItem);

        Menu helpMenu = new Menu("Help");
        MenuItem userManual = new MenuItem("📚 User Manual");
        MenuItem quickStart = new MenuItem("🚀 Quick Start Guide");
        MenuItem faq = new MenuItem("❓ FAQ");
        MenuItem about = new MenuItem("ℹ About");

        userManual.setOnAction(e -> showHelpDialog("📚 User Manual", getUserManualText()));
        quickStart.setOnAction(e -> showHelpDialog("🚀 Quick Start Guide", getQuickStartText()));
        faq.setOnAction(e -> showHelpDialog("❓ Frequently Asked Questions", getFAQText()));
        about.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().addAll(userManual, quickStart, faq, new SeparatorMenuItem(), about);

        menuBar.getMenus().addAll(fileMenu, editMenu, scheduleMenu, studentMenu, helpMenu);
        return menuBar;
    }

    private VBox createConfigurationPanel(String buttonStyle) {
        VBox leftPane = new VBox(15);
        leftPane.setPadding(new Insets(15));
        leftPane.setPrefWidth(320);
        leftPane.setStyle("-fx-background-color: #F3F3F3; -fx-border-color: #D0D0D0; -fx-border-width: 0 1px 0 0;");

        Label configTitle = new Label("⚙ Configuration");
        configTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0078D4;");

        Label dateLabel = new Label("📅 Exam Start Date:");
        dateLabel.setStyle("-fx-font-weight: bold;");
        examStartDatePicker = new DatePicker(LocalDate.now());
        examStartDatePicker.setPrefWidth(200);

        HBox daysRow = new HBox(10, new Label("Exam Period (Days):"), daysSpinner);
        daysRow.setAlignment(Pos.CENTER_LEFT);
        daysSpinner.setPrefWidth(80);
        daysSpinner.setEditable(true);

        Label tsLabel = new Label("⏰ Time Slots (Per Day):");
        tsLabel.setStyle("-fx-font-weight: bold;");
        tsView.setEditable(true);
        tsView.setCellFactory(TextFieldListCell.forListView());
        tsView.getItems().addAll("09:00-11:00", "12:00-14:00", "15:00-17:00");
        tsView.setPrefHeight(120);

        Button addTs = new Button("+");
        Button remTs = new Button("-");
        HBox tsButtons = new HBox(5, addTs, remTs);
        tsButtons.setAlignment(Pos.CENTER_RIGHT);

        String smallBtnStyle = buttonStyle + "-fx-padding: 5px 10px; -fx-min-width: 35px;";
        addTs.setStyle(smallBtnStyle);
        remTs.setStyle(smallBtnStyle);

        addTs.setOnAction(e -> tsView.getItems().add("HH:MM-HH:MM"));
        remTs.setOnAction(e -> {
            int i = tsView.getSelectionModel().getSelectedIndex();
            if (i >= 0)
                tsView.getItems().remove(i);
        });

        Label crLabel = new Label("🏫 Classrooms (ID - Capacity):");
        crLabel.setStyle("-fx-font-weight: bold;");
        crView.setPrefHeight(140);

        Button manageClassroomsBtn = new Button("Manage Classrooms");
        manageClassroomsBtn.setStyle(buttonStyle + "-fx-font-size: 11px;");
        manageClassroomsBtn.setOnAction(e -> showManageClassrooms((Stage) crView.getScene().getWindow()));

        getTimeSlotsFromUI = () -> new ArrayList<>(tsView.getItems());

        leftPane.getChildren().addAll(
                configTitle,
                new Separator(),
                dateLabel, examStartDatePicker,
                new Separator(),
                daysRow,
                new Separator(),
                tsLabel, tsView, tsButtons,
                new Separator(),
                crLabel, crView, manageClassroomsBtn);

        return leftPane;
    }

    private VBox createMessagesPanel(String titleStyle) {
        ListView<String> msgView = new ListView<>(messages);
        msgView.setPrefHeight(300);

        Label msgTitle = new Label("📢 Messages & Logs");
        msgTitle.setStyle(titleStyle);

        Label statsTitle = new Label("📊 Statistics");
        statsTitle.setStyle(titleStyle);

        statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setPrefHeight(150);
        statsArea.setText("Total Exams: 0\nPlaced Exams: 0\nUnplaced Exams: 0\nConflicts: 0");

        VBox rightPane = new VBox(10, msgTitle, msgView, statsTitle, statsArea);
        rightPane.setPadding(new Insets(15));
        rightPane.setPrefWidth(380);
        rightPane.setStyle("-fx-background-color: #F3F3F3; -fx-border-color: #D0D0D0; -fx-border-width: 0 0 0 1px;");

        return rightPane;
    }

    private TableView<ExamEntry> createExamTable(Stage stage) {
        TableView<ExamEntry> tableView = new TableView<>();
        tableView.setItems(exams);
        tableView.setPlaceholder(new Label("No exam schedule generated yet. Click 'Generate Schedule'."));

        TableColumn<ExamEntry, String> colId = new TableColumn<>("Exam ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(80);

        TableColumn<ExamEntry, String> colCourse = new TableColumn<>("Course Code");
        colCourse.setCellValueFactory(new PropertyValueFactory<>("courseId"));
        colCourse.setPrefWidth(120);

        TableColumn<ExamEntry, Integer> colDay = new TableColumn<>("Day");
        colDay.setCellValueFactory(new PropertyValueFactory<>("day"));
        colDay.setPrefWidth(60);

        TableColumn<ExamEntry, String> colTime = new TableColumn<>("Time Slot");
        colTime.setCellValueFactory(new PropertyValueFactory<>("timeSlot"));
        colTime.setPrefWidth(140);

        TableColumn<ExamEntry, String> colRoom = new TableColumn<>("Classroom");
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        colRoom.setPrefWidth(100);

        TableColumn<ExamEntry, Integer> colEnrolled = new TableColumn<>("Students");
        colEnrolled.setCellValueFactory(new PropertyValueFactory<>("enrolled"));
        colEnrolled.setPrefWidth(80);

        TableColumn<ExamEntry, Void> colAct = new TableColumn<>("Actions");
        colAct.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏ Edit");
            private final Button deleteBtn = new Button("🗑");

            {
                String editStyle = "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 3px 8px; -fx-font-size: 10px; -fx-background-radius: 3; -fx-cursor: hand;";
                String deleteStyle = "-fx-background-color: #F44336; -fx-text-fill: white; -fx-padding: 3px 8px; -fx-font-size: 10px; -fx-background-radius: 3; -fx-cursor: hand;";

                editBtn.setStyle(editStyle);
                deleteBtn.setStyle(deleteStyle);

                editBtn.setOnAction(ev -> {
                    ExamEntry e = getTableView().getItems().get(getIndex());
                    editExam(stage, e);
                });

                deleteBtn.setOnAction(ev -> {
                    ExamEntry e = getTableView().getItems().get(getIndex());
                    deleteExam(e);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editBtn, deleteBtn);
                    setGraphic(buttons);
                }
            }
        });
        colAct.setPrefWidth(120);

        tableView.getColumns().addAll(colId, colCourse, colDay, colTime, colRoom, colEnrolled, colAct);
        return tableView;
    }

    private void handleLoad(Stage owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder with CSV files (students.csv, courses.csv, classrooms.csv, attendance.csv)");
        File dir = chooser.showDialog(owner);
        if (dir == null) {
            messages.add("⚠ Load cancelled.");
            return;
        }

        messages.add("📂 Loading data from: " + dir.getName() + "...");
        dataManager.clearAllData();

        try {
            List<File> files = Files.list(dir.toPath()).map(p -> p.toFile()).toList();

            String studentsPath = findFile(files, "student");
            String coursesPath = findFile(files, "course");
            String classroomsPath = findFile(files, "classroom");
            String attendancePath = findFile(files, "attendance");

            if (studentsPath == null || coursesPath == null || classroomsPath == null) {
                messages.add("❌ ERROR: Missing required CSV files (students, courses, classrooms)");
                return;
            }

            dataManager.setSourceFiles(new File(studentsPath), new File(coursesPath), new File(classroomsPath));

            List<Student> loadedStudents = CSVParser.parseStudents(studentsPath);
            List<Course> loadedCourses = CSVParser.parseCourses(coursesPath);
            List<Classroom> loadedClassrooms = CSVParser.parseClassrooms(classroomsPath);

            dataManager.setStudents(loadedStudents);
            dataManager.setCourses(loadedCourses);
            dataManager.setClassrooms(loadedClassrooms);

            if (attendancePath != null) {
                CSVParser.parseAttendanceLists(attendancePath, dataManager.getStudents(), dataManager.getCourses());
                messages.add("✓ Attendance lists loaded successfully.");
            } else {
                messages.add("⚠ Warning: No attendance file found.");
            }

        } catch (CSVParser.CSVParseException e) {
            messages.add("❌ CSV Parse Error: " + e.getMessage());
            dataManager.clearAllData();
            return;
        } catch (IOException e) {
            messages.add("❌ File I/O Error: " + e.getMessage());
            return;
        }

        updateClassroomsView();

        messages.add("✓ Data loaded successfully:");
        messages.add("  • Students: " + dataManager.getStudents().size());
        messages.add("  • Courses: " + dataManager.getCourses().size());
        messages.add("  • Classrooms: " + dataManager.getClassrooms().size());
        messages.add("🚀 Ready to generate schedule.");
    }

    private String findFile(List<File> files, String keyword) {
        return files.stream()
                .filter(f -> f.getName().toLowerCase().contains(keyword))
                .map(File::getAbsolutePath)
                .findFirst()
                .orElse(null);
    }

    private void updateClassroomsView() {
        crView.getItems().clear();
        if (dataManager.getClassrooms() != null) {
            dataManager.getClassrooms()
                    .forEach(c -> crView.getItems().add(c.getClassroomID() + " - Cap: " + c.getCapacity()));
        }
    }

    private void handleGenerateSchedule() {

        if (!dataManager.isDataLoaded()) {
            showError("Data not loaded", "Please load data from CSV files first.");
            messages.add("❌ ERROR: No data loaded. Click 'Load Data' first.");
            return;
        }

        List<Course> coursesToSchedule = dataManager.getCourses();
        List<Classroom> availableClassrooms = new ArrayList<>(dataManager.getClassrooms());

        List<String> timeSlotsRaw = getTimeSlotsFromUI.get();
        int days = daysSpinner.getValue();

        if (coursesToSchedule.isEmpty() || availableClassrooms.isEmpty() || timeSlotsRaw.isEmpty()) {
            showError("Configuration Error", "Please check configuration (Days, Slots, Courses).");
            return;
        }

        messages.add("⚡ Starting schedule generation...");
        messages.add("📊 Parameters: " + days + " days, " + timeSlotsRaw.size() + " slots/day");

        List<Exam> examsToPlace = coursesToSchedule.stream()
                .filter(c -> c.getStudentCount() > 0)
                .map(Exam::new)
                .sorted((a, b) -> Integer.compare(b.getStudentCount(), a.getStudentCount()))
                .collect(Collectors.toList());

        dataManager.setSchedule(new Schedule(days, timeSlotsRaw.size()));

        Map<Student, Set<TimeSlot>> studentScheduledSlots = new HashMap<>();
        Map<TimeSlot, Set<String>> roomOccupancy = new HashMap<>();
        Map<TimeSlot, Set<String>> instructorOccupancy = new HashMap<>();

        int placedCount = 0;
        unplacedCourses.clear();

        // Greedy Algorithm - Randomized Room Selection with Additional Constraints
        for (Exam exam : examsToPlace) {
            boolean placed = false;
            List<Student> studentsOfCourse = exam.getEnrolledStudents();
            int enrolledCount = exam.getStudentCount();
            String instructor = exam.getCourse().getInstructor();

            // Randomize classroom search order for each new exam
            Collections.shuffle(availableClassrooms, new Random());

            outerLoop: for (int day = 1; day <= days; day++) {
                for (int slotNum = 1; slotNum <= timeSlotsRaw.size(); slotNum++) {
                    TimeSlot currentSlot = new TimeSlot(day, slotNum);

                    // 1. CONSTRAINT: STUDENT CONFLICT
                    boolean studentConflict = false;
                    // A. Does the student have another exam at the same time?
                    for (Student student : studentsOfCourse) {
                        Set<TimeSlot> busySlots = studentScheduledSlots.getOrDefault(student, Collections.emptySet());
                        if (busySlots.contains(currentSlot)) {
                            studentConflict = true;
                            break;
                        }
                    }
                    if (studentConflict)
                        continue;

                    // B. Consecutive Exam Constraint
                    if (slotNum > 1) {
                        TimeSlot previousSlot = new TimeSlot(day, slotNum - 1);
                        for (Student student : studentsOfCourse) {
                            Set<TimeSlot> busySlots = studentScheduledSlots.getOrDefault(student,
                                    Collections.emptySet());
                            if (busySlots.contains(previousSlot)) {
                                studentConflict = true;
                                messages.add("  ⚠ WARNING: " + exam.getCourse().getCourseCode() +
                                        " conflict (Consecutive) for student");
                                break;
                            }
                        }
                    }

                    if (studentConflict)
                        continue;

                    // 2. CONSTRAINT: INSTRUCTOR CONFLICT
                    if (instructor != null && !instructor.isEmpty()) {
                        instructorOccupancy.putIfAbsent(currentSlot, new HashSet<>());
                        if (instructorOccupancy.get(currentSlot).contains(instructor)) {
                            messages.add("  ⚠ WARNING: " + exam.getCourse().getCourseCode() +
                                    " conflict (Instructor) for " + instructor);
                            continue;
                        }
                    }

                    // 3. CONSTRAINT: CLASSROOM ASSIGNMENT AND ROOM CONFLICT
                    for (Classroom room : availableClassrooms) {

                        // Capacity check
                        if (!room.canAccommodate(enrolledCount))
                            continue;

                        // Room occupancy check
                        roomOccupancy.putIfAbsent(currentSlot, new HashSet<>());
                        if (roomOccupancy.get(currentSlot).contains(room.getClassroomID()))
                            continue;

                        // --- ASSIGNMENT ---
                        exam.setTimeSlot(currentSlot);
                        exam.setClassroom(room);
                        dataManager.getSchedule().addExam(exam);

                        // Record Room and Instructor occupancy
                        roomOccupancy.get(currentSlot).add(room.getClassroomID());
                        if (instructor != null && !instructor.isEmpty()) {
                            instructorOccupancy.get(currentSlot).add(instructor);
                        }

                        // Update student schedule
                        for (Student student : studentsOfCourse) {
                            studentScheduledSlots.computeIfAbsent(student, k -> new HashSet<>()).add(currentSlot);
                        }

                        placed = true;
                        placedCount++;

                        messages.add("  ✓ " + exam.getCourse().getCourseCode() +
                                " → Day " + day + ", Slot " + slotNum +
                                ", Room " + room.getClassroomID() +
                                " (" + enrolledCount + " students)");

                        break outerLoop; // Successful assignment made, move to the next exam
                    }
                }
            }

            if (!placed) {
                unplacedCourses.add(exam.getCourse().getCourseCode());
                messages.add("❌ FAILED: " + exam.getCourse().getCourseCode()
                        + " could not be placed (All constraints failed).");
            }
        }

        updateExamTableView(timeSlotsRaw);

        int total = examsToPlace.size();
        int unplacedCount = unplacedCourses.size();

        String statsText = String.format(
                "Total Exams: %d\nPlaced Exams: %d\nUnplaced Exams: %d\nConflicts: %d",
                total,
                placedCount,
                unplacedCount,
                unplacedCount);

        if (statsArea != null) {
            statsArea.setText(statsText);
        } else {
            System.err.println("Error: statsArea object is null!");
        }

        messages.add("✓ Schedule generation completed!");
        if (unplacedCount > 0) {
            messages.add("❌ Unplaced Courses: " + String.join(", ", unplacedCourses));
        } else {
            messages.add("🎉 Perfect Schedule! All exams placed.");
        }
    }

    private void updateExamTableView(List<String> timeSlotLabels) {
        exams.clear();
        int idCounter = 1;

        if (dataManager.getSchedule() != null) {
            for (Exam exam : dataManager.getSchedule().getExams()) {
                if (exam.isScheduled()) {
                    String slotLabel = "N/A";
                    try {
                        slotLabel = timeSlotLabels.get(exam.getTimeSlot().getSlotNumber() - 1);
                    } catch (Exception ignored) {
                        slotLabel = "Slot " + exam.getTimeSlot().getSlotNumber();
                    }

                    String eid = String.format("EX%03d", idCounter++);

                    exams.add(new ExamEntry(
                            eid,
                            exam.getCourse().getCourseCode(),
                            exam.getTimeSlot().getDay(),
                            slotLabel,
                            exam.getClassroom().getClassroomID(),
                            exam.getStudentCount()));
                }
            }
        }
        table.refresh();
    }

    private void showManageStudents(Stage owner) {
        if (!dataManager.isDataLoaded()) {
            showError("No Data", "Please load data first.");
            return;
        }

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Manage Students");

        ListView<String> listView = new ListView<>();
        dataManager.getStudents().forEach(s -> listView.getItems().add(s.getStudentID()));

        TextField idField = new TextField();
        idField.setPromptText("New Student ID");
        Button addBtn = new Button("Add");
        Button remBtn = new Button("Remove");

        addBtn.setOnAction(e -> {
            String id = idField.getText().trim();
            if (!id.isEmpty()) {
                Student s = new Student(id);
                dataManager.addStudent(s);
                listView.getItems().add(id);
                messages.add("Student added: " + id);
                idField.clear();
            }
        });

        remBtn.setOnAction(e -> {
            String selectedId = listView.getSelectionModel().getSelectedItem();
            if (selectedId != null) {
                Student s = dataManager.getStudentByID(selectedId);
                dataManager.removeStudent(s);
                listView.getItems().remove(selectedId);
                messages.add("Student removed: " + selectedId);
            }
        });

        VBox root = new VBox(10, new Label("Students"), listView, new HBox(5, idField, addBtn, remBtn));
        root.setPadding(new Insets(10));
        dialog.setScene(new Scene(root, 300, 400));
        dialog.show();
    }

    private void showManageCourses(Stage owner) {
        if (!dataManager.isDataLoaded()) {
            showError("No Data", "Please load data first.");
            return;
        }

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Manage Courses");

        ListView<String> listView = new ListView<>();
        dataManager.getCourses().forEach(c -> listView.getItems().add(c.getCourseCode()));

        TextField codeField = new TextField();
        codeField.setPromptText("Course Code");
        TextField nameField = new TextField();
        nameField.setPromptText("Course Name");

        Button addBtn = new Button("Add");
        Button remBtn = new Button("Remove");

        addBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (!code.isEmpty()) {
                Course c = new Course(code, nameField.getText(), "", 1);
                dataManager.addCourse(c);
                listView.getItems().add(code);
                messages.add("Course added: " + code);
                codeField.clear();
                nameField.clear();
            }
        });

        remBtn.setOnAction(e -> {
            int idx = listView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                Course c = dataManager.getCourses().get(idx);
                dataManager.removeCourse(c);
                listView.getItems().remove(idx);
                messages.add("Course removed.");
            }
        });

        VBox inputs = new VBox(5, codeField, nameField);
        VBox root = new VBox(10, new Label("Courses"), listView, inputs, new HBox(5, addBtn, remBtn));
        root.setPadding(new Insets(10));
        dialog.setScene(new Scene(root, 300, 450));
        dialog.show();
    }

    private void showManageClassrooms(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Manage Classrooms");

        ListView<String> classroomList = new ListView<>();
        if (dataManager.getClassrooms() != null) {
            dataManager.getClassrooms()
                    .forEach(c -> classroomList.getItems().add(c.getClassroomID() + " - Cap: " + c.getCapacity()));
        }

        TextField idField = new TextField();
        idField.setPromptText("Room ID");
        Spinner<Integer> capacitySpinner = new Spinner<>(1, 500, 40);
        capacitySpinner.setEditable(true);

        Button addBtn = new Button("Add");
        Button remBtn = new Button("Remove");
        Button closeBtn = new Button("Close");

        addBtn.setOnAction(e -> {
            String id = idField.getText().trim();
            if (!id.isEmpty()) {
                Classroom c = new Classroom(id, capacitySpinner.getValue());
                dataManager.addClassroom(c);
                classroomList.getItems().add(id + " - Cap: " + c.getCapacity());
                updateClassroomsView();
                idField.clear();
                messages.add("Classroom added: " + id);
            }
        });

        remBtn.setOnAction(e -> {
            int idx = classroomList.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                dataManager.getClassrooms().remove(idx);
                classroomList.getItems().remove(idx);
                updateClassroomsView();
                messages.add("Classroom removed");
            }
        });

        closeBtn.setOnAction(e -> dialog.close());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(15));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Classrooms:"), 0, 0);
        grid.add(classroomList, 0, 1, 2, 1);
        grid.add(new Label("ID:"), 0, 2);
        grid.add(idField, 1, 2);
        grid.add(new Label("Capacity:"), 0, 3);
        grid.add(capacitySpinner, 1, 3);
        grid.add(new HBox(10, addBtn, remBtn, closeBtn), 0, 4, 2, 1);

        dialog.setScene(new Scene(grid, 400, 400));
        dialog.show();
    }

    private void editExam(Stage owner, ExamEntry e) {
        Stage d = new Stage();
        d.initOwner(owner);
        d.initModality(Modality.APPLICATION_MODAL);
        d.setTitle("Edit Exam: " + e.getId());

        TextField course = new TextField(e.getCourseId());
        Spinner<Integer> day = new Spinner<>(1, 30, e.getDay());
        TextField slot = new TextField(e.getTimeSlot());
        TextField room = new TextField(e.getRoomId());
        Spinner<Integer> enroll = new Spinner<>(0, 1000, e.getEnrolled());
        day.setEditable(true);
        enroll.setEditable(true);

        Button save = new Button("💾 Save");
        save.setOnAction(ev -> {
            e.setCourseId(course.getText());
            e.setDay(day.getValue());
            e.setTimeSlot(slot.getText());
            e.setRoomId(room.getText());
            e.setEnrolled(enroll.getValue());

            if (dataManager.getSchedule() != null) {
                dataManager.getSchedule().rebuildTimeSlotMap();
            }

            table.refresh();
            messages.add("✓ Exam " + e.getId() + " updated");
            d.close();
        });

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(12);
        grid.add(new Label("Course Code:"), 0, 0);
        grid.add(course, 1, 0);
        grid.add(new Label("Day:"), 0, 1);
        grid.add(day, 1, 1);
        grid.add(new Label("Time Slot:"), 0, 2);
        grid.add(slot, 1, 2);
        grid.add(new Label("Room ID:"), 0, 3);
        grid.add(room, 1, 3);
        grid.add(new Label("Enrolled:"), 0, 4);
        grid.add(enroll, 1, 4);
        grid.add(save, 1, 5);

        d.setScene(new Scene(grid, 350, 350));
        d.showAndWait();
    }

    private void deleteExam(ExamEntry e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Exam");
        confirm.setContentText("Delete " + e.getId() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            exams.remove(e);
            table.refresh();
            messages.add("🗑 Deleted: " + e.getId());
        }
    }

    private void handleSave(Stage owner) {
        if (dataManager.getSchedule() == null) {
            showWarning("No Schedule", "Nothing to save.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Schedule");
        chooser.setInitialFileName("schedule.csv");
        File f = chooser.showSaveDialog(owner);
        if (f != null) {
            try (PrintWriter pw = new PrintWriter(f)) {
                pw.println("ExamID,Course,Day,Slot,Room,Students");
                for (ExamEntry e : exams)
                    pw.printf("%s,%s,%d,%s,%s,%d%n", e.getId(), e.getCourseId(), e.getDay(), e.getTimeSlot(),
                            e.getRoomId(), e.getEnrolled());
                messages.add("✓ Saved to " + f.getName());
            } catch (Exception e) {
                showError("Save Failed", e.getMessage());
            }
        }
    }

    private void handleExport(Stage owner) {
        if (dataManager.getSchedule() == null || exams.isEmpty()) {
            showWarning("Export Failed", "Please generate a schedule first.");
            return;
        }

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Export Schedule");

        Label formatLabel = new Label("Export format:");
        ComboBox<String> formatCombo = new ComboBox<>(FXCollections.observableArrayList("CSV", "JSON", "Print Report"));
        formatCombo.setValue("CSV");

        Button exportButton = new Button("Export");
        exportButton.setStyle("-fx-background-color: #0078D4; -fx-text-fill: white; -fx-padding: 8px 16px;");

        exportButton.setOnAction(e -> {
            String format = formatCombo.getValue();
            dialog.close();

            switch (format) {
                case "CSV":
                    handleSave(owner);
                    break;
                case "JSON":
                    showInfo("Export", "JSON export is not implemented yet.");
                    break;
                case "Print Report":
                    showInfo("Export", "Print report is not implemented yet.");
                    break;
                default:
                    handleSave(owner);
            }
        });

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));

        Label title = new Label("Export Schedule");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0078D4;");

        HBox formatRow = new HBox(10, formatLabel, formatCombo);
        formatRow.setAlignment(Pos.CENTER_LEFT);

        VBox contentBox = new VBox(10, formatRow, exportButton);
        contentBox.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(title, contentBox);

        Scene scene = new Scene(layout, 350, 200);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // --- VALIDATION AND CONFLICT REPORT METHODS ---

    private void handleValidate() {
        messages.add("🔎 Starting schedule validation...");
        List<String> conflicts = validateScheduleLogic();

        // Add unplaced courses as critical conflicts
        if (!unplacedCourses.isEmpty()) {
            for (String course : unplacedCourses) {
                conflicts.add("❌ UNPLACED: Course " + course + " could not be scheduled");
            }
        }

        long criticalCount = conflicts.stream().filter(s -> s.startsWith("❌")).count();
        long warningCount = conflicts.stream().filter(s -> s.startsWith("⚠")).count();

        if (criticalCount == 0 && unplacedCourses.isEmpty()) {
            if (warningCount == 0) {
                showInfo("Validation Success", "The schedule passed all validation checks with no issues.");
                messages.add("✅ Validation completed: No conflicts or warnings found.");
            } else {
                showInfo("Validation Success with Warnings",
                        String.format("The schedule passed all critical validation checks, but has %d warning(s).",
                                warningCount));
                messages.add("⚠ Validation completed: " + warningCount + " warnings found (no critical conflicts).");
            }
        } else {
            StringBuilder sb = new StringBuilder();
            if (!unplacedCourses.isEmpty()) {
                sb.append(String.format("❌ %d unplaced course(s): %s\n\n",
                        unplacedCourses.size(), String.join(", ", unplacedCourses)));
            }
            sb.append(
                    String.format("Found %d critical conflict(s) and %d warning(s).\n\n", criticalCount, warningCount));
            sb.append("First 5 critical issues:\n");
            conflicts.stream().filter(s -> s.startsWith("❌") && !s.contains("UNPLACED")).limit(5)
                    .forEach(c -> sb.append("- ").append(c).append("\n"));

            if (warningCount > 0) {
                sb.append("\nFirst 3 warnings:\n");
                conflicts.stream().filter(s -> s.startsWith("⚠")).limit(3)
                        .forEach(c -> sb.append("- ").append(c).append("\n"));
            }

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Validation Issues Found");
            alert.setHeaderText("Schedule Validation Failed");
            alert.setContentText(sb.toString());
            alert.showAndWait();

            messages.add("❌ Validation completed: " + criticalCount + " critical issues, " + warningCount
                    + " warnings found.");
        }
    }

    private void showConflictReport() {
        messages.add("📄 Generating conflict report...");
        List<String> conflicts = validateScheduleLogic();

        // Add unplaced courses
        List<String> allConflicts = new ArrayList<>();
        if (!unplacedCourses.isEmpty()) {
            allConflicts.add("--- UNPLACED COURSES ---");
            for (String course : unplacedCourses) {
                allConflicts.add("❌ " + course + " - Could not be assigned a time slot/classroom");
            }
            allConflicts.add("");
        }
        allConflicts.addAll(conflicts);

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Conflict Report");

        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setPrefSize(600, 400);

        if (allConflicts.isEmpty() && unplacedCourses.isEmpty()) {
            reportArea.setText(
                    "🎉 PERFECT SCHEDULE\n\nNo critical conflicts or warnings found.\nAll exams have been successfully placed.");
        } else {
            StringBuilder sb = new StringBuilder("📋 CONFLICT REPORT\n");
            sb.append("=".repeat(50)).append("\n\n");

            long criticalCount = allConflicts.stream().filter(s -> s.startsWith("❌")).count();
            long warningCount = allConflicts.stream().filter(s -> s.startsWith("⚠")).count();

            sb.append("SUMMARY:\n");
            sb.append("-".repeat(20)).append("\n");
            sb.append(String.format("• Total Exams: %d\n", dataManager.getCourses().size()));
            sb.append(String.format("• Placed Exams: %d\n", exams.size()));
            sb.append(String.format("• Unplaced Exams: %d\n", unplacedCourses.size()));
            sb.append(String.format("• Critical Conflicts: %d\n", criticalCount));
            sb.append(String.format("• Warnings: %d\n\n", warningCount));

            if (!unplacedCourses.isEmpty()) {
                sb.append("UNPLACED COURSES (CRITICAL):\n");
                sb.append("-".repeat(30)).append("\n");
                for (String course : unplacedCourses) {
                    sb.append("❌ ").append(course).append("\n");
                }
                sb.append("\n");
            }

            // Filter out unplaced courses from critical conflicts list
            List<String> criticalConflicts = allConflicts.stream()
                    .filter(s -> s.startsWith("❌") && !s.contains("UNPLACED"))
                    .collect(Collectors.toList());

            if (!criticalConflicts.isEmpty()) {
                sb.append("CRITICAL CONFLICTS:\n");
                sb.append("-".repeat(30)).append("\n");
                criticalConflicts.forEach(c -> sb.append(c).append("\n"));
                sb.append("\n");
            }

            if (warningCount > 0) {
                sb.append("WARNINGS:\n");
                sb.append("-".repeat(30)).append("\n");
                allConflicts.stream().filter(s -> s.startsWith("⚠"))
                        .forEach(c -> sb.append(c).append("\n"));
            }

            if (criticalCount == 0 && warningCount == 0 && !unplacedCourses.isEmpty()) {
                sb.append("\n⚠ NOTE: The only issues are unplaced courses. Consider:\n");
                sb.append("   • Increasing exam days\n");
                sb.append("   • Adding more classrooms\n");
                sb.append("   • Adding more time slots\n");
            }

            reportArea.setText(sb.toString());
        }

        Button exportBtn = new Button("📤 Export Report");
        exportBtn.setOnAction(e -> exportConflictReport(reportArea.getText()));

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> dialogStage.close());

        HBox buttonBox = new HBox(10, exportBtn, closeBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, new Label("Detailed Conflict Analysis:"), reportArea, buttonBox);
        root.setPadding(new Insets(10));
        dialogStage.setScene(new Scene(root));
        dialogStage.show();
        messages.add("✓ Conflict report generated.");
    }

    private void exportConflictReport(String reportText) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Conflict Report");
        chooser.setInitialFileName("conflict_report_" + LocalDate.now() + ".txt");
        File file = chooser.showSaveDialog(null);

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file)) {
                pw.println(reportText);
                showInfo("Export Successful", "Conflict report exported to:\n" + file.getAbsolutePath());
                messages.add("✓ Conflict report exported to: " + file.getName());
            } catch (Exception e) {
                showError("Export Failed", e.getMessage());
            }
        }
    }

    private List<String> validateScheduleLogic() {
        List<String> validationMessages = new ArrayList<>();
        if (dataManager.getSchedule() == null || dataManager.getSchedule().getExams().isEmpty()) {
            validationMessages.add("❌ Validation Failed: No schedule generated.");
            return validationMessages;
        }

        // Get all PLACED exams
        List<Exam> placedExams = dataManager.getSchedule().getExams().stream()
                .filter(Exam::isScheduled)
                .collect(Collectors.toList());

        if (placedExams.isEmpty()) {
            validationMessages.add("❌ No exams have been placed in the schedule.");
            return validationMessages;
        }

        Map<Student, Set<TimeSlot>> studentScheduledSlots = new HashMap<>();
        Map<TimeSlot, Set<String>> roomOccupancy = new HashMap<>();
        Map<TimeSlot, Set<String>> instructorOccupancy = new HashMap<>();

        for (Exam exam : placedExams) {
            TimeSlot currentSlot = exam.getTimeSlot();
            String roomID = exam.getClassroom().getClassroomID();
            String instructor = exam.getCourse().getInstructor();
            String courseCode = exam.getCourse().getCourseCode();
            List<Student> students = exam.getEnrolledStudents();

            // 1. ROOM CONFLICT (Double booking)
            if (roomOccupancy.getOrDefault(currentSlot, new HashSet<>()).contains(roomID)) {
                validationMessages.add("❌ Room Conflict: " + courseCode + " and another exam at Day " +
                        currentSlot.getDay() + ", Slot " + currentSlot.getSlotNumber() + " in " + roomID);
            }
            roomOccupancy.computeIfAbsent(currentSlot, k -> new HashSet<>()).add(roomID);

            // 2. INSTRUCTOR CONFLICT
            if (instructor != null && !instructor.isEmpty()) {
                if (instructorOccupancy.getOrDefault(currentSlot, new HashSet<>()).contains(instructor)) {
                    validationMessages.add("❌ Instructor Conflict: " + courseCode + " and another exam for " +
                            instructor + " at Day " + currentSlot.getDay() + ", Slot " + currentSlot.getSlotNumber());
                }
                instructorOccupancy.computeIfAbsent(currentSlot, k -> new HashSet<>()).add(instructor);
            }

            // 3. STUDENT CONFLICT AND CONSECUTIVE EXAMS
            for (Student student : students) {
                Set<TimeSlot> busySlots = studentScheduledSlots.computeIfAbsent(student, k -> new HashSet<>());

                // Student Conflict (Same time - CRITICAL)
                if (busySlots.contains(currentSlot)) {
                    validationMessages.add("❌ Student Conflict: " + student.getStudentID() +
                            " has two exams at Day " + currentSlot.getDay() + ", Slot " + currentSlot.getSlotNumber() +
                            " (Course: " + courseCode + ")");
                }

                // Consecutive Exam Constraint (Warning)
                if (currentSlot.getSlotNumber() > 1) {
                    TimeSlot previousSlot = new TimeSlot(currentSlot.getDay(), currentSlot.getSlotNumber() - 1);
                    if (busySlots.contains(previousSlot)) {
                        validationMessages.add("⚠ Consecutive Exam: " + student.getStudentID() +
                                " has " + courseCode + " at Slot " + currentSlot.getSlotNumber() +
                                " right after an exam at Slot " + previousSlot.getSlotNumber());
                    }
                }

                busySlots.add(currentSlot);
            }
        }

        return validationMessages;
    }

    private List<String> getUnplacedCourses() {
        return new ArrayList<>(unplacedCourses);
    }

    // --- UPDATED HELP TEXTS WITH BETTER FORMATTING ---

    private void showHelpDialog(String title, String content) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 12px;");
        textArea.setPrefSize(700, 500);

        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> dialog.close());
        closeButton.setStyle("-fx-background-color: #0078D4; -fx-text-fill: white; -fx-padding: 8px 16px;");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().addAll(scrollPane, closeButton);

        Scene scene = new Scene(layout, 750, 550);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showAboutDialog() {
        String aboutText = """
                ╔══════════════════════════════════════════╗
                ║           EXAM SCHEDULER v2.0            ║
                ║        with Student Portal Feature       ║
                ╚══════════════════════════════════════════╝

                📅 APPLICATION OVERVIEW
                • Intelligent Exam Scheduling System
                • Student Portal for Individual Schedules
                • Conflict Detection & Resolution
                • Multi-format Export Capabilities

                👥 DEVELOPMENT TEAM 11
                • Project Lead: [Your Name]
                • Backend Developer: [Name]
                • Frontend Developer: [Name]
                • QA Tester: [Name]

                🔧 TECHNOLOGIES USED
                • Java 17
                • JavaFX for GUI
                • Greedy Algorithm for Scheduling
                • CSV Data Management

                📞 CONTACT INFORMATION
                • Email: support@examscheduler.edu
                • Website: www.examscheduler.edu
                • Documentation: docs.examscheduler.edu

                📄 LICENSE
                • Educational Use License
                • Version: 2.0.0
                • Release Date: """ + LocalDate.now().getYear() + """

                ⭐ FEATURES
                ✓ Load CSV Data Files
                ✓ Generate Optimal Schedules
                ✓ Student Conflict Prevention
                ✓ Room Capacity Management
                ✓ Instructor Scheduling
                ✓ Student Portal Access
                ✓ Export Multiple Formats
                ✓ Detailed Validation Reports

                🚀 Thank you for using Exam Scheduler!
                """;

        showHelpDialog("ℹ About Exam Scheduler", aboutText);
    }

    private String getUserManualText() {
        return """
                📚 EXAM SCHEDULER - USER MANUAL v2.0
                =====================================

                TABLE OF CONTENTS
                1. Getting Started
                2. Data Management
                3. Schedule Configuration
                4. Generating Schedules
                5. Student Portal
                6. Validation & Reports
                7. Export Options
                8. Advanced Features

                --------------------------
                1. GETTING STARTED
                --------------------------

                SYSTEM REQUIREMENTS:
                • Java 11 or higher
                • Minimum 2GB RAM
                • CSV files in proper format

                INITIAL SETUP:
                1. Launch the application
                2. Prepare your CSV files:
                   - students.csv: StudentID,Name
                   - courses.csv: CourseCode,CourseName,Instructor,MaxCapacity
                   - classrooms.csv: ClassroomID,Capacity
                   - attendance.csv: CourseCode,StudentID (optional)
                3. Click 'Load Data' and select folder containing CSV files

                INTERFACE OVERVIEW:
                ┌─────────────────────────────────────────────┐
                │ Left Panel: Configuration Settings          │
                │ Center Panel: Exam Schedule Table           │
                │ Right Panel: Messages & Statistics          │
                │ Top: Menu Bar & Toolbar                     │
                └─────────────────────────────────────────────┘

                --------------------------
                2. DATA MANAGEMENT
                --------------------------

                LOADING DATA:
                • Use 'File → Load Data...' or 📁 button
                • Select folder containing all CSV files
                • System validates file structure automatically

                REQUIRED FILES:
                • students.csv: Must contain StudentID column
                • courses.csv: Must contain CourseCode and CourseName
                • classrooms.csv: Must contain ClassroomID and Capacity

                OPTIONAL FILES:
                • attendance.csv: Links students to courses
                • If missing, you can assign manually later

                MANAGING DATA:
                • Edit → Manage Students: Add/remove students
                • Edit → Manage Courses: Add/remove courses
                • Edit → Manage Classrooms: Add/remove classrooms

                DATA VALIDATION:
                ✓ File format checking
                ✓ Duplicate detection
                ✓ Capacity validation

                --------------------------
                3. SCHEDULE CONFIGURATION
                --------------------------

                EXAM PERIOD SETTINGS:
                • Exam Start Date: Select calendar date
                • Exam Period (Days): 1-30 days
                • Time Slots Per Day: Default 3 slots

                TIME SLOT MANAGEMENT:
                • Default: 09:00-11:00, 12:00-14:00, 15:00-17:00
                • Add new slots: Click '+' button
                • Remove slots: Select and click '-' button
                • Edit slots: Double-click to modify

                CLASSROOM SETTINGS:
                • View all available classrooms
                • See capacity for each room
                • Manage via 'Manage Classrooms' button

                --------------------------
                4. GENERATING SCHEDULES
                --------------------------

                GENERATION PROCESS:
                1. Ensure data is loaded (✓ check messages)
                2. Configure exam period and slots
                3. Click '⚡ Generate Schedule' button

                ALGORITHM FEATURES:
                • Greedy algorithm with randomization
                • Student conflict prevention
                • Room capacity optimization
                • Instructor scheduling
                • Consecutive exam avoidance

                GENERATION STEPS:
                1. Sort courses by enrollment (largest first)
                2. For each course, find suitable time slot
                3. Check all constraints:
                   - Student availability
                   - Room capacity
                   - Instructor schedule
                   - No consecutive exams
                4. Assign or mark as unplaced

                POST-GENERATION:
                • View schedule in central table
                • Check statistics in right panel
                • Review messages for warnings

                --------------------------
                5. STUDENT PORTAL
                --------------------------

                ACCESSING PORTAL:
                • Click '👤 Student Portal' button
                • Or use 'Students → Student Portal...'

                FEATURES:
                • Calendar View: Visual schedule by day/slot
                • List View: Detailed exam list
                • Summary: Total exams and distribution
                • Export: Save personal schedule

                CALENDAR FEATURES:
                • Color-coded exam blocks
                • Course codes and room numbers
                • Date-based navigation

                EXPORT OPTIONS:
                • Text file with all exam details
                • Includes dates, times, rooms
                • Suitable for printing

                --------------------------
                6. VALIDATION & REPORTS
                --------------------------

                VALIDATION TYPES:
                • Quick Validation: Click '✓ Validate' button
                • Detailed Report: 'Schedule → Conflict Report'

                CHECKED CONSTRAINTS:
                1. Student Conflicts (Critical)
                2. Room Double-booking (Critical)
                3. Instructor Conflicts (Critical)
                4. Consecutive Exams (Warning)
                5. Unplaced Courses (Critical)

                CONFLICT REPORT CONTENTS:
                • Summary statistics
                • List of unplaced courses
                • Detailed conflict descriptions
                • Recommendations for fixes

                RESOLVING CONFLICTS:
                • Increase exam days
                • Add more classrooms
                • Adjust time slots
                • Reduce course enrollments

                --------------------------
                7. EXPORT OPTIONS
                --------------------------

                AVAILABLE FORMATS:
                • CSV: Comma-separated values
                • JSON: JavaScript Object Notation
                • Print Report: Formatted document

                WHAT'S EXPORTED:
                • Full schedule with all details
                • Student-specific schedules
                • Conflict reports
                • Statistical summaries

                EXPORT LOCATIONS:
                • Choose folder on your computer
                • Default naming with timestamps
                • Overwrite protection

                --------------------------
                8. ADVANCED FEATURES
                --------------------------

                MANUAL EDITING:
                • Edit individual exams: Click ✏ button
                • Delete exams: Click 🗑 button
                • Real-time validation updates

                CUSTOM CONSTRAINTS:
                • Adjust consecutive exam policy
                • Set instructor preferences
                • Define room preferences

                STATISTICAL ANALYSIS:
                • Placement rate calculation
                • Resource utilization metrics
                • Conflict frequency tracking

                TROUBLESHOOTING:

                COMMON ISSUES:
                Issue: "No data loaded"
                Fix: Check CSV file formats and reload

                Issue: "All exams unplaced"
                Fix: Increase days, add rooms, or reduce slots

                Issue: "Student conflicts"
                Fix: Increase exam period length

                SUPPORT:
                • Check FAQ section
                • Use Quick Start Guide
                • Contact: support@examscheduler.edu

                VERSION: 2.0
                LAST UPDATED: """ + LocalDate.now().getYear() + "-" +
                String.format("%02d", LocalDate.now().getMonthValue()) + "-" +
                String.format("%02d", LocalDate.now().getDayOfMonth());
    }

    private String getFAQText() {
        return """
                ❓ FREQUENTLY ASKED QUESTIONS (FAQ)
                ===================================

                📊 GENERAL QUESTIONS
                --------------------

                Q1: What is the purpose of this application?
                A1: Exam Scheduler is designed to automatically generate optimal
                    exam schedules for educational institutions, considering
                    multiple constraints like room capacity, student availability,
                    and instructor schedules.

                Q2: Is there a limit to the number of students or courses?
                A2: Theoretically no, but performance is optimized for:
                    • Up to 10,000 students
                    • Up to 500 courses
                    • Up to 100 classrooms

                Q3: Can I use this for different types of scheduling?
                A3: Yes! While designed for exams, it can be adapted for:
                    • Class scheduling
                    • Meeting room booking
                    • Event planning

                📁 DATA MANAGEMENT
                ------------------

                Q4: What CSV format should I use?
                A4: Required CSV formats:

                students.csv:
                StudentID,Name,Email
                S001,John Doe,john@edu.edu
                S002,Jane Smith,jane@edu.edu

                courses.csv:
                CourseCode,CourseName,Instructor,MaxCapacity
                CS101,Intro to CS,Dr. Smith,100
                MATH201,Calculus I,Dr. Johnson,80

                classrooms.csv:
                ClassroomID,Capacity
                A101,50
                B202,100

                Q5: What if I don't have an attendance.csv file?
                A5: You can:
                    1. Create one manually
                    2. Use 'Manage Courses' to assign students later
                    3. The system will work but scheduling may be less optimal

                Q6: Can I import data from Excel?
                A6: Yes! Save your Excel files as:
                    • File → Save As
                    • Choose "CSV (Comma delimited) (*.csv)"
                    • Use UTF-8 encoding for best results

                ⚙ CONFIGURATION
                ---------------

                Q7: How many time slots can I add per day?
                A7: Technically unlimited, but practical limits:
                    • Recommended: 3-6 slots
                    • Maximum tested: 10 slots
                    • Each slot should be at least 2 hours

                Q8: What's the best exam period length?
                A8: Depends on your constraints:
                    • Small institutions: 3-5 days
                    • Medium: 5-10 days
                    • Large: 10-20 days
                    • Rule: More days = fewer conflicts

                Q9: Can I schedule exams on weekends?
                A9: Yes! The system treats all days equally. Just count
                    weekend days in your total exam period.

                ⚡ SCHEDULE GENERATION
                ----------------------

                Q10: Why are some courses not placed?
                A10: Common reasons:
                     1. Not enough exam days
                     2. Classroom capacity too small
                     3. Time slot conflicts with student schedules
                     4. Instructor unavailable

                     Solutions:
                     • Increase exam period
                     • Add larger classrooms
                     • Adjust time slots
                     • Review student enrollment

                Q11: How does the algorithm prioritize courses?
                A11: By default:
                     1. Courses with most students first
                     2. Courses with specialized room requirements
                     3. Instructor availability
                     4. Time preferences

                Q12: Can I manually override the schedule?
                A12: Yes! After generation:
                     • Click ✏ to edit any exam
                     • Change day, time, or room
                     • System will warn about conflicts

                👤 STUDENT PORTAL
                -----------------

                Q13: How do students access their schedules?
                A13: Two methods:
                     1. Through administrator (your) interface
                     2. Export schedules and distribute
                     3. Future: Web portal integration

                Q14: Can students see only their own exams?
                A14: Yes! The student portal filters by:
                     • Selected student ID
                     • Only shows their enrolled courses
                     • Private and secure

                Q15: What if a student has consecutive exams?
                A15: The system shows warnings for:
                     • Same-day consecutive exams
                     • Recommendations for rescheduling
                     • Manual adjustment options

                ✓ VALIDATION & CONFLICTS
                -------------------------

                Q16: What's considered a "critical" conflict?
                A16: Critical conflicts prevent scheduling:
                     • Student double-booked in same slot
                     • Room double-booked
                     • Instructor teaching two courses simultaneously
                     • Course cannot be placed at all

                Q17: What's considered a "warning"?
                A17: Warnings don't prevent scheduling but are suboptimal:
                     • Students with consecutive exams
                     • Underutilized classrooms
                     • Long gaps between student exams

                Q18: How do I fix validation errors?
                A18: Step-by-step approach:
                     1. Check Conflict Report for details
                     2. Increase exam days if many conflicts
                     3. Add classrooms if capacity issues
                     4. Adjust time slots if consecutive exam warnings

                📤 EXPORT & SHARING
                --------------------

                Q19: What export formats are available?
                A19: Currently:
                     • CSV: For spreadsheet programs
                     • JSON: For web applications
                     • TXT: For printing and sharing
                     Future: PDF, Excel, iCalendar

                Q20: Can I export for specific students only?
                A20: Yes! Two methods:
                     1. Use Student Portal → Export My Schedule
                     2. Filter main schedule and export

                Q21: Is there batch export capability?
                A21: Not yet, but you can:
                     • Export full schedule
                     • Export individual student schedules
                     • Combine using external tools

                🔧 TROUBLESHOOTING
                -------------------

                Q22: Application crashes on startup
                A22: Try:
                     1. Update Java to latest version
                     2. Check system memory (min 2GB)
                     3. Run as administrator
                     4. Reinstall application

                Q23: CSV files not loading properly
                A23: Common issues:
                     • Wrong file encoding (use UTF-8)
                     • Missing required columns
                     • Special characters in headers
                     • Empty rows at end of file

                Q24: Schedule generation takes too long
                A24: Optimization tips:
                     • Reduce number of courses
                     • Limit exam period to necessary days
                     • Close other applications
                     • Upgrade computer RAM

                Q25: Can't see all columns in table
                A25: Solutions:
                     • Scroll horizontally
                     • Maximize window
                     • Hide unnecessary columns
                     • Export to see all data

                📞 SUPPORT & RESOURCES
                -----------------------

                Q26: Where can I get more help?
                A26: Available resources:
                     • This FAQ section
                     • User Manual (Help menu)
                     • Quick Start Guide
                     • Email: support@examscheduler.edu

                Q27: Are there video tutorials?
                A27: Yes! Check our YouTube channel:
                     • Basic setup: youtu.be/exam-scheduler-setup
                     • Advanced features: youtu.be/exam-scheduler-advanced

                Q28: Can I request new features?
                A28: Absolutely! Send feature requests to:
                     • Email: features@examscheduler.edu
                     • Include: Use case, benefits, priority

                Q29: Is there a mobile app?
                A29: Currently desktop only, but:
                     • Student schedules can be exported to mobile
                     • Web version planned for next release
                     • Mobile app in development

                Q30: How do I report bugs?
                A30: Please include:
                     1. Application version
                     2. Steps to reproduce
                     3. Error message screenshot
                     4. System information
                     Send to: bugs@examscheduler.edu

                LAST UPDATED: """ + LocalDate.now().toString() + """

                Need more help? Contact: help@examscheduler.edu
                """;
    }

    private String getQuickStartText() {
        return """
                🚀 QUICK START GUIDE - EXAM SCHEDULER
                =====================================

                ⏱️ 5-MINUTE SETUP GUIDE

                Follow these steps to create your first exam schedule in minutes!

                ┌─────────────────────────────────────────┐
                │          GETTING STARTED                │
                └─────────────────────────────────────────┘

                STEP 1: PREPARE YOUR DATA
                --------------------------

                Create these CSV files in a folder:

                1. students.csv
                   -------------
                   StudentID,Name
                   S001,John Doe
                   S002,Jane Smith
                   S003,Bob Johnson

                2. courses.csv
                   -------------
                   CourseCode,CourseName,Instructor,Capacity
                   CS101,Computer Science,Dr. Adams,50
                   MATH201,Calculus,Dr. Brown,40

                3. classrooms.csv
                   ----------------
                   ClassroomID,Capacity
                   A101,60
                   B202,50

                4. attendance.csv (optional)
                   --------------------------
                   CourseCode,StudentID
                   CS101,S001
                   CS101,S002
                   MATH201,S003

                ┌─────────────────────────────────────────┐
                │         LOAD YOUR DATA                  │
                └─────────────────────────────────────────┘

                STEP 2: IMPORT FILES
                ---------------------

                1. Launch Exam Scheduler

                2. Click the 📁 LOAD DATA button

                3. Select the folder containing your CSV files

                4. Wait for confirmation messages:
                   ✓ Data loaded successfully
                   ✓ Students: [number]
                   ✓ Courses: [number]
                   ✓ Classrooms: [number]

                ┌─────────────────────────────────────────┐
                │        CONFIGURE SETTINGS               │
                └─────────────────────────────────────────┘

                STEP 3: SET UP EXAM PERIOD
                ---------------------------

                In the LEFT PANEL (Configuration):

                1. 📅 Exam Start Date
                   • Click calendar icon
                   • Select first exam day

                2. Exam Period (Days)
                   • Use spinner or type: 5 (recommended)

                3. ⏰ Time Slots
                   • Keep defaults or modify:
                     09:00-11:00
                     12:00-14:00
                     15:00-17:00

                ┌─────────────────────────────────────────┐
                │      GENERATE SCHEDULE                  │
                └─────────────────────────────────────────┘

                STEP 4: CREATE SCHEDULE
                ------------------------

                1. Click ⚡ GENERATE SCHEDULE button

                2. Watch progress in Messages panel:
                   • Starting schedule generation...
                   • Parameters: 5 days, 3 slots/day
                   • Course assignments appear

                3. Check Statistics:
                   • Total Exams: [number]
                   • Placed Exams: [number]
                   • Unplaced Exams: [number]

                ┌─────────────────────────────────────────┐
                │       VALIDATE & REVIEW                 │
                └─────────────────────────────────────────┘

                STEP 5: CHECK FOR ISSUES
                -------------------------

                1. Click ✓ VALIDATE button

                2. Review results:
                   • Green: No critical issues
                   • Red: Conflicts found

                3. View detailed report:
                   • Schedule → Conflict Report

                ┌─────────────────────────────────────────┐
                │      STUDENT SCHEDULES                  │
                └─────────────────────────────────────────┘

                STEP 6: SHARE WITH STUDENTS
                ----------------------------

                1. Click 👤 STUDENT PORTAL button

                2. Select a student ID from dropdown

                3. View their personal schedule:
                   • Calendar view (visual)
                   • List view (detailed)
                   • Summary statistics

                4. Export their schedule:
                   • Click 📄 Export My Schedule
                   • Save as text file
                   • Share with student

                ┌─────────────────────────────────────────┐
                │        SAVE & EXPORT                    │
                └─────────────────────────────────────────┘

                STEP 7: FINALIZE SCHEDULE
                --------------------------

                1. Save full schedule:
                   • Click 💾 SAVE SCHEDULE
                   • Choose location
                   • Name: spring_exams_2024.csv

                2. Export options:
                   • CSV: For spreadsheets
                   • JSON: For web apps
                   • Print: For distribution

                ⭐ PRO TIPS FOR BEGINNERS
                -------------------------

                TIP 1: START SMALL
                • Test with 50 students, 5 courses first
                • Understand the workflow
                • Then scale up

                TIP 2: CHECK CAPACITIES
                • Ensure classrooms fit course enrollments
                • Add buffer (e.g., 60-capacity room for 50 students)

                TIP 3: USE DEFAULT SLOTS
                • 3 slots/day works for most cases
                • 2-hour slots allow for 30min breaks

                TIP 4: VALIDATE EARLY
                • Check after each major change
                • Fix conflicts as they appear

                TIP 5: EXPORT OFTEN
                • Save versions as you work
                • Name files with dates

                🚨 COMMON PITFALLS TO AVOID
                ----------------------------

                PITFALL 1: Missing data
                • Ensure all CSV files are in same folder
                • Check column headers exactly

                PITFALL 2: Too few exam days
                • Start with more days than you think
                • Reduce after successful generation

                PITFALL 3: Room capacity issues
                • Match largest course to largest room
                • Consider splitting large courses

                PITFALL 4: Ignoring warnings
                • Address consecutive exam warnings
                • Consider student fatigue

                🎯 NEXT STEPS
                -------------

                AFTER MASTERING BASICS:

                1. ADVANCED FEATURES:
                   • Manual schedule editing
                   • Custom constraints
                   • Room preferences

                2. DATA OPTIMIZATION:
                   • Analyze placement rates
                   • Optimize room utilization
                   • Balance student schedules

                3. AUTOMATION:
                   • Batch processing
                   • Regular schedule updates
                   • Integration with school systems

                📞 NEED HELP?
                -------------

                Quick support options:

                • Check FAQ section (Help → FAQ)
                • Read full User Manual (Help → User Manual)
                • Email: quickstart@examscheduler.edu

                Remember: The first schedule might have issues.
                Adjust settings and try again!

                Happy Scheduling! 🎓

                Version: 2.0 | Quick Start Guide
                """;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class ExamEntry {
        private final SimpleStringProperty id, courseId, timeSlot, roomId;
        private final SimpleIntegerProperty day, enrolled;

        public ExamEntry(String id, String courseId, int day, String timeSlot, String roomId, int enrolled) {
            this.id = new SimpleStringProperty(id);
            this.courseId = new SimpleStringProperty(courseId);
            this.day = new SimpleIntegerProperty(day);
            this.timeSlot = new SimpleStringProperty(timeSlot);
            this.roomId = new SimpleStringProperty(roomId);
            this.enrolled = new SimpleIntegerProperty(enrolled);
        }

        public String getId() {
            return id.get();
        }

        public String getCourseId() {
            return courseId.get();
        }

        public int getDay() {
            return day.get();
        }

        public String getTimeSlot() {
            return timeSlot.get();
        }

        public String getRoomId() {
            return roomId.get();
        }

        public int getEnrolled() {
            return enrolled.get();
        }

        public void setCourseId(String value) {
            courseId.set(value);
        }

        public void setDay(int value) {
            day.set(value);
        }

        public void setTimeSlot(String value) {
            timeSlot.set(value);
        }

        public void setRoomId(String value) {
            roomId.set(value);
        }

        public void setEnrolled(int value) {
            enrolled.set(value);
        }
    }
}