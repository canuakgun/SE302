package com.examscheduler.ui;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
        Button exportBtn = new Button("📤 Export Schedule");
        Button importScheduleBtn = new Button("📥 Import Schedule");
        Button studentPortalBtn = new Button("👤 Student Portal");
        loadBtn.setStyle(buttonStyle);
        generateBtn.setStyle(buttonStyle);
        saveBtn.setStyle(buttonStyle);
        validateBtn.setStyle(buttonStyle);
        exportBtn.setStyle(buttonStyle);
        importScheduleBtn.setStyle(buttonStyle);
        studentPortalBtn.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8px 16px; -fx-font-weight: bold; -fx-cursor: hand;");
        toolBar.setStyle(toolBarStyle);

        toolBar.getItems().addAll(loadBtn, new Separator(), generateBtn, validateBtn, new Separator(),
                saveBtn, exportBtn, importScheduleBtn, new Separator(), studentPortalBtn);

        loadBtn.setOnAction(e -> handleLoad(stage));
        generateBtn.setOnAction(e -> handleGenerateSchedule());
        saveBtn.setOnAction(e -> handleSave(stage));
        validateBtn.setOnAction(e -> handleValidate());
        exportBtn.setOnAction(e -> handleExport(stage));
        importScheduleBtn.setOnAction(e -> handleImportSchedule(stage));
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
        tsView.getItems().addAll("09:00-11:00", "11:00-13:00", "13:00-15:00", "15:00-17:00", "17:00-19:00");
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
            List<File> files = Files.list(dir.toPath()).map(p -> p.toFile()).collect(Collectors.toList());

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

    private void handleImportSchedule(Stage owner) {
        // 1. Temel veriler (Dersler, Sınıflar) yüklü mü kontrol et
        if (!dataManager.isDataLoaded()) {
            showError("Data Required", "Please load base data (Students, Courses, Classrooms) first using 'Load Data'.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Schedule CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(owner);

        if (file == null) return;

        try {
            // 2. CSVParser ile dosyayı okut ve Exam listesini al (TEK SATIR!)
            List<String> currentSlots = getTimeSlotsFromUI.get();
            List<Exam> loadedExams = CSVParser.parseSchedule(file, dataManager, currentSlots);

            if (loadedExams.isEmpty()) {
                showWarning("Import Failed", "No valid exams found in file or matching courses not found.");
                return;
            }

            // 3. Yeni Schedule nesnesini oluştur
            int days = daysSpinner.getValue();
            Schedule newSchedule = new Schedule(days, currentSlots.size());
            
            // Okunan sınavları takvime ekle
            for (Exam exam : loadedExams) {
                newSchedule.addExam(exam);
            }

            // 4. Sistemi güncelle
            dataManager.setSchedule(newSchedule);
            updateExamTableView(currentSlots);
            
            // İstatistikleri yazdır
            if (statsArea != null) {
                statsArea.setText("Imported Exams: " + loadedExams.size() + "\nStatus: Loaded from file");
            }
            messages.add("✅ Imported schedule from " + file.getName());
            messages.add("📊 Successfully loaded " + loadedExams.size() + " exams.");

        } catch (IOException e) {
            showError("Import Failed", "Could not read file: " + e.getMessage());
        }
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

                final int currentDay = day;

                for (int slotNum = 1; slotNum <= timeSlotsRaw.size(); slotNum++) {
                    TimeSlot currentSlot = new TimeSlot(day, slotNum);

                    // 1. CONSTRAINT: STUDENT CONFLICT
                    boolean studentConflict = false;

                    for (Student student : studentsOfCourse) {
                        Set<TimeSlot> busySlots = studentScheduledSlots.getOrDefault(student, Collections.emptySet());

                        // A. Does the student have another exam at the same time? (Kritik Çakışma)
                        if (busySlots.contains(currentSlot)) {
                            studentConflict = true;
                            break;
                        }

                        // --- YENİ KISITLAMA: GÜNLÜK MAKSİMUM 2 SINAV (User Req 6) ---
                        // Öğrencinin o gün (currentDay) kaç tane sınavı olduğunu sayıyoruz.
                        long examsOnDay = busySlots.stream()
                                .filter(ts -> ts.getDay() == currentDay)
                                .count();

                        if (examsOnDay >= 2) {
                            // Eğer öğrencinin o gün zaten 2 sınavı varsa, 3. sınav verilemez.
                            studentConflict = true;
                            break;
                        }
                        // ------------------------------------------------------------
                    }
                    if (studentConflict)
                        continue;

                    // B. Consecutive Exam Constraint (Ardışık Sınav - Kesin Engel olarak kaldı)
                    if (slotNum > 1) {
                        TimeSlot previousSlot = new TimeSlot(day, slotNum - 1);
                        for (Student student : studentsOfCourse) {
                            Set<TimeSlot> busySlots = studentScheduledSlots.getOrDefault(student,
                                    Collections.emptySet());
                            if (busySlots.contains(previousSlot)) {
                                studentConflict = true;
                                // messages.add("  ⚠ WARNING: " + exam.getCourse().getCourseCode() + " conflict (Consecutive)");
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
                            // messages.add("  ⚠ WARNING: " + exam.getCourse().getCourseCode() + " conflict (Instructor)");
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

    // --- UPDATED HELP TEXTS WITHOUT TEXT BLOCKS ---

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
        String aboutText = "╔══════════════════════════════════════════╗\n" +
                "║           EXAM SCHEDULER v2.0            ║\n" +
                "║        with Student Portal Feature       ║\n" +
                "╚══════════════════════════════════════════╝\n\n" +
                "📅 APPLICATION OVERVIEW\n" +
                "• Intelligent Exam Scheduling System\n" +
                "• Student Portal for Individual Schedules\n" +
                "• Conflict Detection & Resolution\n" +
                "• Multi-format Export Capabilities\n\n" +
                "👥 DEVELOPMENT TEAM 11\n" +
                "• Project Lead: [Your Name]\n" +
                "• Backend Developer: [Name]\n" +
                "• Frontend Developer: [Name]\n" +
                "• QA Tester: [Name]\n\n" +
                "🔧 TECHNOLOGIES USED\n" +
                "• Java 17\n" +
                "• JavaFX for GUI\n" +
                "• Greedy Algorithm for Scheduling\n" +
                "• CSV Data Management\n\n" +
                "📞 CONTACT INFORMATION\n" +
                "• Email: support@examscheduler.edu\n" +
                "• Website: www.examscheduler.edu\n" +
                "• Documentation: docs.examscheduler.edu\n\n" +
                "📄 LICENSE\n" +
                "• Educational Use License\n" +
                "• Version: 2.0.0\n" +
                "• Release Date: " + LocalDate.now().getYear() + "\n\n" +
                "⭐ FEATURES\n" +
                "✓ Load CSV Data Files\n" +
                "✓ Generate Optimal Schedules\n" +
                "✓ Student Conflict Prevention\n" +
                "✓ Room Capacity Management\n" +
                "✓ Instructor Scheduling\n" +
                "✓ Student Portal Access\n" +
                "✓ Export Multiple Formats\n" +
                "✓ Detailed Validation Reports\n\n" +
                "🚀 Thank you for using Exam Scheduler!";

        showHelpDialog("ℹ About Exam Scheduler", aboutText);
    }

    private String getUserManualText() {
        return "📚 EXAM SCHEDULER - USER MANUAL v2.0\n" +
                "======================================\n\n" +
                "TABLE OF CONTENTS\n" +
                "1. Getting Started\n" +
                "2. Data Management\n" +
                "3. Schedule Configuration\n" +
                "4. Generating Schedules\n" +
                "5. Student Portal\n" +
                "6. Validation & Reports\n" +
                "7. Export Options\n" +
                "8. Advanced Features\n\n" +
                "--------------------------\n" +
                "1. GETTING STARTED\n" +
                "--------------------------\n\n" +
                "SYSTEM REQUIREMENTS:\n" +
                "• Java 11 or higher\n" +
                "• Minimum 2GB RAM\n" +
                "• CSV files in proper format\n\n" +
                "INITIAL SETUP:\n" +
                "1. Launch the application\n" +
                "2. Prepare your CSV files:\n" +
                "   - students.csv: StudentID,Name\n" +
                "   - courses.csv: CourseCode,CourseName,Instructor,MaxCapacity\n" +
                "   - classrooms.csv: ClassroomID,Capacity\n" +
                "   - attendance.csv: CourseCode,StudentID (optional)\n" +
                "3. Click 'Load Data' and select folder containing CSV files\n\n" +
                "INTERFACE OVERVIEW:\n" +
                "┌─────────────────────────────────────────────┐\n" +
                "│ Left Panel: Configuration Settings          │\n" +
                "│ Center Panel: Exam Schedule Table           │\n" +
                "│ Right Panel: Messages & Statistics          │\n" +
                "│ Top: Menu Bar & Toolbar                     │\n" +
                "└─────────────────────────────────────────────┘\n\n" +
                "--------------------------\n" +
                "2. DATA MANAGEMENT\n" +
                "--------------------------\n\n" +
                "LOADING DATA:\n" +
                "• Use 'File → Load Data...' or 📁 button\n" +
                "• Select folder containing all CSV files\n" +
                "• System validates file structure automatically\n\n" +
                "REQUIRED FILES:\n" +
                "• students.csv: Must contain StudentID column\n" +
                "• courses.csv: Must contain CourseCode and CourseName\n" +
                "• classrooms.csv: Must contain ClassroomID and Capacity\n\n" +
                "OPTIONAL FILES:\n" +
                "• attendance.csv: Links students to courses\n" +
                "• If missing, you can assign manually later\n\n" +
                "MANAGING DATA:\n" +
                "• Edit → Manage Students: Add/remove students\n" +
                "• Edit → Manage Courses: Add/remove courses\n" +
                "• Edit → Manage Classrooms: Add/remove classrooms\n\n" +
                "DATA VALIDATION:\n" +
                "✓ File format checking\n" +
                "✓ Duplicate detection\n" +
                "✓ Capacity validation\n\n" +
                "--------------------------\n" +
                "3. SCHEDULE CONFIGURATION\n" +
                "--------------------------\n\n" +
                "EXAM PERIOD SETTINGS:\n" +
                "• Exam Start Date: Select calendar date\n" +
                "• Exam Period (Days): 1-30 days\n" +
                "• Time Slots Per Day: Default 3 slots\n\n" +
                "TIME SLOT MANAGEMENT:\n" +
                "• Default: 09:00-11:00, 11:00-13:00, 13:00-15:00, 15:00-17:00, 17:00-19:00\n" +
                "• Add new slots: Click '+' button\n" +
                "• Remove slots: Select and click '-' button\n" +
                "• Edit slots: Double-click to modify\n\n" +
                "CLASSROOM SETTINGS:\n" +
                "• View all available classrooms\n" +
                "• See capacity for each room\n" +
                "• Manage via 'Manage Classrooms' button\n\n" +
                "--------------------------\n" +
                "4. GENERATING SCHEDULES\n" +
                "--------------------------\n\n" +
                "GENERATION PROCESS:\n" +
                "1. Ensure data is loaded (✓ check messages)\n" +
                "2. Configure exam period and slots\n" +
                "3. Click '⚡ Generate Schedule' button\n\n" +
                "ALGORITHM FEATURES:\n" +
                "• Greedy algorithm with randomization\n" +
                "• Student conflict prevention\n" +
                "• Room capacity optimization\n" +
                "• Instructor scheduling\n" +
                "• Consecutive exam avoidance\n\n" +
                "GENERATION STEPS:\n" +
                "1. Sort courses by enrollment (largest first)\n" +
                "2. For each course, find suitable time slot\n" +
                "3. Check all constraints:\n" +
                "   - Student availability\n" +
                "   - Room capacity\n" +
                "   - Instructor schedule\n" +
                "   - No consecutive exams\n" +
                "4. Assign or mark as unplaced\n\n" +
                "POST-GENERATION:\n" +
                "• View schedule in central table\n" +
                "• Check statistics in right panel\n" +
                "• Review messages for warnings\n\n" +
                "--------------------------\n" +
                "5. STUDENT PORTAL\n" +
                "--------------------------\n\n" +
                "ACCESSING PORTAL:\n" +
                "• Click '👤 Student Portal' button\n" +
                "• Or use 'Students → Student Portal...'\n\n" +
                "FEATURES:\n" +
                "• Calendar View: Visual schedule by day/slot\n" +
                "• List View: Detailed exam list\n" +
                "• Summary: Total exams and distribution\n" +
                "• Export: Save personal schedule\n\n" +
                "CALENDAR FEATURES:\n" +
                "• Color-coded exam blocks\n" +
                "• Course codes and room numbers\n" +
                "• Date-based navigation\n\n" +
                "EXPORT OPTIONS:\n" +
                "• Text file with all exam details\n" +
                "• Includes dates, times, rooms\n" +
                "• Suitable for printing\n\n" +
                "--------------------------\n" +
                "6. VALIDATION & REPORTS\n" +
                "--------------------------\n\n" +
                "VALIDATION TYPES:\n" +
                "• Quick Validation: Click '✓ Validate' button\n" +
                "• Detailed Report: 'Schedule → Conflict Report'\n\n" +
                "CHECKED CONSTRAINTS:\n" +
                "1. Student Conflicts (Critical)\n" +
                "2. Room Double-booking (Critical)\n" +
                "3. Instructor Conflicts (Critical)\n" +
                "4. Consecutive Exams (Warning)\n" +
                "5. Unplaced Courses (Critical)\n\n" +
                "CONFLICT REPORT CONTENTS:\n" +
                "• Summary statistics\n" +
                "• List of unplaced courses\n" +
                "• Detailed conflict descriptions\n" +
                "• Recommendations for fixes\n\n" +
                "RESOLVING CONFLICTS:\n" +
                "• Increase exam days\n" +
                "• Add more classrooms\n" +
                "• Adjust time slots\n" +
                "• Reduce course enrollments\n\n" +
                "--------------------------\n" +
                "7. EXPORT OPTIONS\n" +
                "--------------------------\n\n" +
                "AVAILABLE FORMATS:\n" +
                "• CSV: Comma-separated values\n" +
                "• JSON: JavaScript Object Notation\n" +
                "• Print Report: Formatted document\n\n" +
                "WHAT'S EXPORTED:\n" +
                "• Full schedule with all details\n" +
                "• Student-specific schedules\n" +
                "• Conflict reports\n" +
                "• Statistical summaries\n\n" +
                "EXPORT LOCATIONS:\n" +
                "• Choose folder on your computer\n" +
                "• Default naming with timestamps\n" +
                "• Overwrite protection\n\n" +
                "--------------------------\n" +
                "8. ADVANCED FEATURES\n" +
                "--------------------------\n\n" +
                "MANUAL EDITING:\n" +
                "• Edit individual exams: Click ✏ button\n" +
                "• Delete exams: Click 🗑 button\n" +
                "• Real-time validation updates\n\n" +
                "CUSTOM CONSTRAINTS:\n" +
                "• Adjust consecutive exam policy\n" +
                "• Set instructor preferences\n" +
                "• Define room preferences\n\n" +
                "STATISTICAL ANALYSIS:\n" +
                "• Placement rate calculation\n" +
                "• Resource utilization metrics\n" +
                "• Conflict frequency tracking\n\n" +
                "TROUBLESHOOTING:\n\n" +
                "COMMON ISSUES:\n" +
                "Issue: \"No data loaded\"\n" +
                "Fix: Check CSV file formats and reload\n\n" +
                "Issue: \"All exams unplaced\"\n" +
                "Fix: Increase days, add rooms, or reduce slots\n\n" +
                "Issue: \"Student conflicts\"\n" +
                "Fix: Increase exam period length\n\n" +
                "SUPPORT:\n" +
                "• Check FAQ section\n" +
                "• Use Quick Start Guide\n" +
                "• Contact: support@examscheduler.edu\n\n" +
                "VERSION: 2.0\n" +
                "LAST UPDATED: " + LocalDate.now().getYear() + "-" +
                String.format("%02d", LocalDate.now().getMonthValue()) + "-" +
                String.format("%02d", LocalDate.now().getDayOfMonth());
    }

    private String getFAQText() {
        return "❓ FREQUENTLY ASKED QUESTIONS (FAQ)\n" +
                "===================================\n\n" +
                "📊 GENERAL QUESTIONS\n" +
                "--------------------\n\n" +
                "Q1: What is the purpose of this application?\n" +
                "A1: Exam Scheduler is designed to automatically generate optimal \n" +
                "    exam schedules for educational institutions, considering \n" +
                "    multiple constraints like room capacity, student availability, \n" +
                "    and instructor schedules.\n\n" +
                "Q2: Is there a limit to the number of students or courses?\n" +
                "A2: Theoretically no, but performance is optimized for:\n" +
                "    • Up to 10,000 students\n" +
                "    • Up to 500 courses\n" +
                "    • Up to 100 classrooms\n\n" +
                "Q3: Can I use this for different types of scheduling?\n" +
                "A3: Yes! While designed for exams, it can be adapted for:\n" +
                "    • Class scheduling\n" +
                "    • Meeting room booking\n" +
                "    • Event planning\n\n" +
                "📁 DATA MANAGEMENT\n" +
                "------------------\n\n" +
                "Q4: What CSV format should I use?\n" +
                "A4: Required CSV formats:\n\n" +
                "students.csv:\n" +
                "StudentID,Name,Email\n" +
                "S001,John Doe,john@edu.edu\n" +
                "S002,Jane Smith,jane@edu.edu\n\n" +
                "courses.csv:\n" +
                "CourseCode,CourseName,Instructor,MaxCapacity\n" +
                "CS101,Intro to CS,Dr. Smith,100\n" +
                "MATH201,Calculus I,Dr. Johnson,80\n\n" +
                "classrooms.csv:\n" +
                "ClassroomID,Capacity\n" +
                "A101,50\n" +
                "B202,100\n\n" +
                "Q5: What if I don't have an attendance.csv file?\n" +
                "A5: You can:\n" +
                "    1. Create one manually\n" +
                "    2. Use 'Manage Courses' to assign students later\n" +
                "    3. The system will work but scheduling may be less optimal\n\n" +
                "Q6: Can I import data from Excel?\n" +
                "A6: Yes! Save your Excel files as:\n" +
                "    • File → Save As\n" +
                "    • Choose \"CSV (Comma delimited) (*.csv)\"\n" +
                "    • Use UTF-8 encoding for best results\n\n" +
                "⚙ CONFIGURATION\n" +
                "---------------\n\n" +
                "Q7: How many time slots can I add per day?\n" +
                "A7: Technically unlimited, but practical limits:\n" +
                "    • Recommended: 3-6 slots\n" +
                "    • Maximum tested: 10 slots\n" +
                "    • Each slot should be at least 2 hours\n\n" +
                "Q8: What's the best exam period length?\n" +
                "A8: Depends on your constraints:\n" +
                "    • Small institutions: 3-5 days\n" +
                "    • Medium: 5-10 days  \n" +
                "    • Large: 10-20 days\n" +
                "    • Rule: More days = fewer conflicts\n\n" +
                "Q9: Can I schedule exams on weekends?\n" +
                "A9: Yes! The system treats all days equally. Just count \n" +
                "    weekend days in your total exam period.\n\n" +
                "⚡ SCHEDULE GENERATION\n" +
                "---------------------\n\n" +
                "Q10: Why are some courses not placed?\n" +
                "A10: Common reasons:\n" +
                "     1. Not enough exam days\n" +
                "     2. Classroom capacity too small\n" +
                "     3. Time slot conflicts with student schedules\n" +
                "     4. Instructor unavailable\n" +
                "     \n" +
                "     Solutions:\n" +
                "     • Increase exam period\n" +
                "     • Add larger classrooms\n" +
                "     • Adjust time slots\n" +
                "     • Review student enrollment\n\n" +
                "Q11: How does the algorithm prioritize courses?\n" +
                "A11: By default:\n" +
                "     1. Courses with most students first\n" +
                "     2. Courses with specialized room requirements\n" +
                "     3. Instructor availability\n" +
                "     4. Time preferences\n\n" +
                "Q12: Can I manually override the schedule?\n" +
                "A12: Yes! After generation:\n" +
                "     • Click ✏ to edit any exam\n" +
                "     • Change day, time, or room\n" +
                "     • System will warn about conflicts\n\n" +
                "👤 STUDENT PORTAL\n" +
                "-----------------\n\n" +
                "Q13: How do students access their schedules?\n" +
                "A13: Two methods:\n" +
                "     1. Through administrator (your) interface\n" +
                "     2. Export schedules and distribute\n" +
                "     3. Future: Web portal integration\n\n" +
                "Q14: Can students see only their own exams?\n" +
                "A14: Yes! The student portal filters by:\n" +
                "     • Selected student ID\n" +
                "     • Only shows their enrolled courses\n" +
                "     • Private and secure\n\n" +
                "Q15: What if a student has consecutive exams?\n" +
                "A15: The system shows warnings for:\n" +
                "     • Same-day consecutive exams\n" +
                "     • Recommendations for rescheduling\n" +
                "     • Manual adjustment options\n\n" +
                "✓ VALIDATION & CONFLICTS\n" +
                "-------------------------\n\n" +
                "Q16: What's considered a \"critical\" conflict?\n" +
                "A16: Critical conflicts prevent scheduling:\n" +
                "     • Student double-booked in same slot\n" +
                "     • Room double-booked\n" +
                "     • Instructor teaching two courses simultaneously\n" +
                "     • Course cannot be placed at all\n\n" +
                "Q17: What's considered a \"warning\"?\n" +
                "A17: Warnings don't prevent scheduling but are suboptimal:\n" +
                "     • Students with consecutive exams\n" +
                "     • Underutilized classrooms\n" +
                "     • Long gaps between student exams\n\n" +
                "Q18: How do I fix validation errors?\n" +
                "A18: Step-by-step approach:\n" +
                "     1. Check Conflict Report for details\n" +
                "     2. Increase exam days if many conflicts\n" +
                "     3. Add classrooms if capacity issues\n" +
                "     4. Adjust time slots if consecutive exam warnings\n\n" +
                "📤 EXPORT & SHARING\n" +
                "-------------------\n\n" +
                "Q19: What export formats are available?\n" +
                "A19: Currently:\n" +
                "     • CSV: For spreadsheet programs\n" +
                "     • JSON: For web applications\n" +
                "     • TXT: For printing and sharing\n" +
                "     Future: PDF, Excel, iCalendar\n\n" +
                "Q20: Can I export for specific students only?\n" +
                "A20: Yes! Two methods:\n" +
                "     1. Use Student Portal → Export My Schedule\n" +
                "     2. Filter main schedule and export\n\n" +
                "Q21: Is there batch export capability?\n" +
                "A21: Not yet, but you can:\n" +
                "     • Export full schedule\n" +
                "     • Export individual student schedules\n" +
                "     • Combine using external tools\n\n" +
                "🔧 TROUBLESHOOTING\n" +
                "-------------------\n\n" +
                "Q22: Application crashes on startup\n" +
                "A22: Try:\n" +
                "     1. Update Java to latest version\n" +
                "     2. Check system memory (min 2GB)\n" +
                "     3. Run as administrator\n" +
                "     4. Reinstall application\n\n" +
                "Q23: CSV files not loading properly\n" +
                "A23: Common issues:\n" +
                "     • Wrong file encoding (use UTF-8)\n" +
                "     • Missing required columns\n" +
                "     • Special characters in headers\n" +
                "     • Empty rows at end of file\n\n" +
                "Q24: Schedule generation takes too long\n" +
                "A24: Optimization tips:\n" +
                "     • Reduce number of courses\n" +
                "     • Limit exam period to necessary days\n" +
                "     • Close other applications\n" +
                "     • Upgrade computer RAM\n\n" +
                "Q25: Can't see all columns in table\n" +
                "A25: Solutions:\n" +
                "     • Scroll horizontally\n" +
                "     • Maximize window\n" +
                "     • Hide unnecessary columns\n" +
                "     • Export to see all data\n\n" +
                "📞 SUPPORT & RESOURCES\n" +
                "----------------------\n\n" +
                "Q26: Where can I get more help?\n" +
                "A26: Available resources:\n" +
                "     • This FAQ section\n" +
                "     • User Manual (Help menu)\n" +
                "     • Quick Start Guide\n" +
                "     • Email: support@examscheduler.edu\n\n" +
                "Q27: Are there video tutorials?\n" +
                "A27: Yes! Check our YouTube channel:\n" +
                "     • Basic setup: youtu.be/exam-scheduler-setup\n" +
                "     • Advanced features: youtu.be/exam-scheduler-advanced\n\n" +
                "Q28: Can I request new features?\n" +
                "A28: Absolutely! Send feature requests to:\n" +
                "     • Email: features@examscheduler.edu\n" +
                "     • Include: Use case, benefits, priority\n\n" +
                "Q29: Is there a mobile app?\n" +
                "A29: Currently desktop only, but:\n" +
                "     • Student schedules can be exported to mobile\n" +
                "     • Web version planned for next release\n" +
                "     • Mobile app in development\n\n" +
                "Q30: How do I report bugs?\n" +
                "A30: Please include:\n" +
                "     1. Application version\n" +
                "     2. Steps to reproduce\n" +
                "     3. Error message screenshot\n" +
                "     4. System information\n" +
                "     Send to: bugs@examscheduler.edu\n\n" +
                "LAST UPDATED: " + LocalDate.now().toString() + "\n\n" +
                "Need more help? Contact: help@examscheduler.edu";
    }

    private String getQuickStartText() {
        return "🚀 QUICK START GUIDE - EXAM SCHEDULER\n" +
                "=====================================\n\n" +
                "⏱️ 5-MINUTE SETUP GUIDE\n\n" +
                "Follow these steps to create your first exam schedule in minutes!\n\n" +
                "┌─────────────────────────────────────────┐\n" +
                "│          GETTING STARTED                │\n" +
                "└─────────────────────────────────────────┘\n\n" +
                "STEP 1: PREPARE YOUR DATA\n" +
                "--------------------------\n\n" +
                "Create these CSV files in a folder:\n\n" +
                "1. students.csv\n" +
                "   -------------\n" +
                "   StudentID,Name\n" +
                "   S001,John Doe\n" +
                "   S002,Jane Smith\n" +
                "   S003,Bob Johnson\n" +
                "   \n" +
                "2. courses.csv\n" +
                "   -------------\n" +
                "   CourseCode,CourseName,Instructor,Capacity\n" +
                "   CS101,Computer Science,Dr. Adams,50\n" +
                "   MATH201,Calculus,Dr. Brown,40\n" +
                "   \n" +
                "3. classrooms.csv\n" +
                "   ----------------\n" +
                "   ClassroomID,Capacity\n" +
                "   A101,60\n" +
                "   B202,50\n" +
                "   \n" +
                "4. attendance.csv (optional)\n" +
                "   --------------------------\n" +
                "   CourseCode,StudentID\n" +
                "   CS101,S001\n" +
                "   CS101,S002\n" +
                "   MATH201,S003\n\n" +
                "┌─────────────────────────────────────────┐\n" +
                "│         LOAD YOUR DATA                  │\n" +
                "└─────────────────────────────────────────┘\n\n" +
                "STEP 2: IMPORT FILES\n" +
                "---------------------\n\n" +
                "1. Launch Exam Scheduler\n\n" +
                "2. Click the 📁 LOAD DATA button\n\n" +
                "3. Select the folder containing your CSV files\n\n" +
                "4. Wait for confirmation messages:\n" +
                "   ✓ Data loaded successfully\n" +
                "   ✓ Students: [number]\n" +
                "   ✓ Courses: [number]\n" +
                "   ✓ Classrooms: [number]\n\n" +
                "┌─────────────────────────────────────────┐\n" +
                "│        CONFIGURE SETTINGS               │\n" +
                "└─────────────────────────────────────────┘\n\n" +
                "STEP 3: SET UP EXAM PERIOD\n" +
                "---------------------------\n\n" +
                "In the LEFT PANEL (Configuration):\n\n" +
                "1. 📅 Exam Start Date\n" +
                "   • Click calendar icon\n" +
                "   • Select first exam day\n" +
                "   \n" +
                "2. Exam Period (Days)\n" +
                "   • Use spinner or type: 5 (recommended)\n" +
                "   \n" +
                "3. ⏰ Time Slots\n" +
                "   • Keep defaults or modify:\n" +
                "     09:00-11:00\n" +
                "     11:00-13:00  \n" +
                "     13:00-15:00\n" +
                "     15:00-17:00\n" +
                "     17:00-19:00\n\n" +
                "┌─────────────────────────────────────────┐\n" +
                "│      GENERATE SCHEDULE                  │\n" +
                "└─────────────────────────────────────────┘\n\n" +
                "STEP 4: CREATE SCHEDULE\n" +
                "-----------------------\n\n" +
                "1. Click ⚡ GENERATE SCHEDULE button\n\n" +
                "2. Watch progress in Messages panel:\n" +
                "   • Starting schedule generation...\n" +
                "   • Parameters: 5 days, 3 slots/day\n" +
                "   • Course assignments appear\n\n" +
                "3. Check Statistics:\n" +
                "   • Total Exams: [number]\n" +
                "   • Placed Exams: [number]\n" +
                "   • Unplaced Exams: [number]\n\n" +
                "┌─────────────────────────────────────────┐\n" +
                "│       VALIDATE & REVIEW                 │\n" +
                "└─────────────────────────────────────────┘\n\n" +
                "STEP 5: CHECK FOR ISSUES\n" +
                "-------------------------\n\n" +
                "1. Click ✓ VALIDATE button\n\n" +
                "2. Review results:\n" +
                "   • Green: No critical issues\n" +
                "   • Red: Conflicts found\n\n" +
                "3. View detailed report:\n" +
                "   • Schedule → Conflict Report\n\n" +
                "┌─────────────────────────────────────────┐\n" +
                "│      STUDENT SCHEDULES                  │\n" +
                "└─────────────────────────────────────────┘\n\n" +
                "STEP 6: SHARE WITH STUDENTS\n" +
                "----------------------------\n\n" +
                "1. Click 👤 STUDENT PORTAL button\n\n" +
                "2. Select a student ID from dropdown\n\n" +
                "3. View their personal schedule:\n" +
                "   • Calendar view (visual)\n" +
                "   • List view (detailed)\n" +
                "   • Summary statistics\n\n" +
                "4. Export their schedule:\n" +
                "   • Click 📄 Export My Schedule\n" +
                "   • Save as text file\n" +
                "   • Share with student\n\n" +
                "┌─────────────────────────────────────────┐\n" +
                "│        SAVE & EXPORT                    │\n" +
                "└─────────────────────────────────────────┘\n\n" +
                "STEP 7: FINALIZE SCHEDULE\n" +
                "--------------------------\n\n" +
                "1. Save full schedule:\n" +
                "   • Click 💾 SAVE SCHEDULE\n" +
                "   • Choose location\n" +
                "   • Name: spring_exams_2024.csv\n\n" +
                "2. Export options:\n" +
                "   • CSV: For spreadsheets\n" +
                "   • JSON: For web apps\n" +
                "   • Print: For distribution\n\n" +
                "⭐ PRO TIPS FOR BEGINNERS\n" +
                "-------------------------\n\n" +
                "TIP 1: START SMALL\n" +
                "• Test with 50 students, 5 courses first\n" +
                "• Understand the workflow\n" +
                "• Then scale up\n\n" +
                "TIP 2: CHECK CAPACITIES\n" +
                "• Ensure classrooms fit course enrollments\n" +
                "• Add buffer (e.g., 60-capacity room for 50 students)\n\n" +
                "TIP 3: USE DEFAULT SLOTS\n" +
                "• 3 slots/day works for most cases\n" +
                "• 2-hour slots allow for 30min breaks\n\n" +
                "TIP 4: VALIDATE EARLY\n" +
                "• Check after each major change\n" +
                "• Fix conflicts as they appear\n\n" +
                "TIP 5: EXPORT OFTEN\n" +
                "• Save versions as you work\n" +
                "• Name files with dates\n\n" +
                "🚨 COMMON PITFALLS TO AVOID\n" +
                "----------------------------\n\n" +
                "PITFALL 1: Missing data\n" +
                "• Ensure all CSV files are in same folder\n" +
                "• Check column headers exactly\n\n" +
                "PITFALL 2: Too few exam days\n" +
                "• Start with more days than you think\n" +
                "• Reduce after successful generation\n\n" +
                "PITFALL 3: Room capacity issues\n" +
                "• Match largest course to largest room\n" +
                "• Consider splitting large courses\n\n" +
                "PITFALL 4: Ignoring warnings\n" +
                "• Address consecutive exam warnings\n" +
                "• Consider student fatigue\n\n" +
                "🎯 NEXT STEPS\n" +
                "-------------\n\n" +
                "AFTER MASTERING BASICS:\n\n" +
                "1. ADVANCED FEATURES:\n" +
                "   • Manual schedule editing\n" +
                "   • Custom constraints\n" +
                "   • Room preferences\n\n" +
                "2. DATA OPTIMIZATION:\n" +
                "   • Analyze placement rates\n" +
                "   • Optimize room utilization\n" +
                "   • Balance student schedules\n\n" +
                "3. AUTOMATION:\n" +
                "   • Batch processing\n" +
                "   • Regular schedule updates\n" +
                "   • Integration with school systems\n\n" +
                "📞 NEED HELP?\n" +
                "-------------\n\n" +
                "Quick support options:\n\n" +
                "• Check FAQ section (Help → FAQ)\n" +
                "• Read full User Manual (Help → User Manual)\n" +
                "• Email: quickstart@examscheduler.edu\n\n" +
                "Remember: The first schedule might have issues.\n" +
                "Adjust settings and try again!\n\n" +
                "Happy Scheduling! 🎓\n\n" +
                "Version: 2.0 | Quick Start Guide";
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