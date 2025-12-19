package com.examscheduler.ui;

import com.examscheduler.logic.CSVParser;
import com.examscheduler.logic.DataManager;
import com.examscheduler.model.Classroom;
import com.examscheduler.model.Course;
import com.examscheduler.model.Exam;
import com.examscheduler.model.Schedule;
import com.examscheduler.model.Student;
import com.examscheduler.model.TimeSlot;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldListCell;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ExamSchedulerApp extends Application {

    private final DataManager dataManager = DataManager.getInstance();
    private final ObservableList<ExamEntry> exams = FXCollections.observableArrayList();
    private final ObservableList<String> messages = FXCollections.observableArrayList();
    private FilteredList<ExamEntry> filteredExams;

    private TableView<ExamEntry> table;
    private TextArea statsArea;
    private final Spinner<Integer> daysSpinner = new Spinner<>(1, 30, 5);
    private ListView<String> crView = new ListView<>();
    private ListView<String> tsView = new ListView<>();
    private Supplier<List<String>> getTimeSlotsFromUI;

    private DatePicker examStartDatePicker;
    private List<String> unplacedCourses = new ArrayList<>();
    private File lastSelectedDirectory = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        showWelcomeScreen(stage);
    }

    private void showWelcomeScreen(Stage stage) {
        stage.setTitle("Exam Scheduler - Welcome");

        // --- Design Elements ---

        // Title
        Label titleLabel = new Label("🎓 Exam Scheduler");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #0078D4;");

        Label subTitleLabel = new Label("Exam Planning and Management System v2.0");
        subTitleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");

        // Button Styles
        String primaryBtnStyle = "-fx-background-color: #0078D4; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12px 30px; -fx-background-radius: 5; -fx-cursor: hand;";
        String secondaryBtnStyle = "-fx-background-color: #555555; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 25px; -fx-background-radius: 5; -fx-cursor: hand;";
        String helpBtnStyle = "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 25px; -fx-background-radius: 5; -fx-cursor: hand;";
        String exitBtnStyle = "-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 25px; -fx-background-radius: 5; -fx-cursor: hand;";

        // Admin/Main Entry Button
        Button startBtn = new Button("Admin Login / Start ➤");
        startBtn.setStyle(primaryBtnStyle);
        addStyledTooltip(startBtn, "Start the application or login as admin");
        startBtn.setOnAction(e -> showMainApplication(stage));

        // Settings Button
        Button settingsBtn = new Button("⚙ Settings");
        settingsBtn.setStyle(secondaryBtnStyle);
        addStyledTooltip(settingsBtn, "Configure application settings");
        settingsBtn.setOnAction(e -> showSettingsDialog()); // showSettingsDialog çağrıldı

        // Help Buttons (New additions)
        Button manualBtn = new Button("📚 User Manual");
        manualBtn.setStyle(helpBtnStyle);
        addStyledTooltip(manualBtn, "Read the user manual");
        manualBtn.setOnAction(e -> showHelpDialog("📚 User Manual", getUserManualText()));

        Button quickStartBtn = new Button("🚀 Quick Start Guide");
        quickStartBtn.setStyle(helpBtnStyle);
        addStyledTooltip(quickStartBtn, "Quick start instructions");
        quickStartBtn.setOnAction(e -> showHelpDialog("🚀 Quick Start Guide", getQuickStartText()));

        Button faqBtn = new Button("❓ FAQ");
        faqBtn.setStyle(helpBtnStyle);
        addStyledTooltip(faqBtn, "Frequently Asked Questions");
        faqBtn.setOnAction(e -> showHelpDialog("❓ Frequently Asked Questions", getFAQText()));

        // Exit Button
        Button exitBtn = new Button("❌ Exit");
        exitBtn.setStyle(exitBtnStyle);
        addStyledTooltip(exitBtn, "Exit the application");
        exitBtn.setOnAction(e -> stage.close());

        // Group the secondary buttons (Settings, Help, Exit)
        HBox utilityButtons = new HBox(20, settingsBtn, exitBtn);
        utilityButtons.setAlignment(Pos.CENTER);

        HBox helpButtons = new HBox(10, manualBtn, quickStartBtn, faqBtn);
        helpButtons.setAlignment(Pos.CENTER);

        // Footer
        Label footerLabel = new Label(
                "Developed by Team 11-Abdulhamid Yıldırım,Ahmet Emir Doğan,Ali Uğur Yalçın,Can Ulaş Akgün,Furkan Pala © "
                        + LocalDate.now().getYear());
        footerLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 12px;");

        // --- Layout Arrangement (VBox) ---
        VBox layout = new VBox(25); // 25px spacing between elements
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));
        layout.setStyle("-fx-background-color: linear-gradient(to bottom right, #ffffff, #f0f4f8);");

        // Add elements
        layout.getChildren().addAll(
                titleLabel,
                subTitleLabel,
                new Separator(),
                startBtn,
                new Separator(),
                helpButtons,
                new Separator(),
                utilityButtons,
                new Region(), // Spacer
                footerLabel);

        // Create and show the scene
        Scene scene = new Scene(layout, 800, 600);
        ThemeManager.getInstance().registerScene(scene);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    // Örnek bir Ayarlar Diyaloğu Metodu
    private void showSettingsDialog() {
        Stage settingsStage = new Stage();
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.setTitle("⚙ Application Settings");

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Global Preferences");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0078D4;");

        // --- 2. Dark Mode Toggle ---
        CheckBox darkModeCheck = new CheckBox("🌙 Enable Dark Mode");
        darkModeCheck.setSelected(ThemeManager.getInstance().isDarkMode());
        darkModeCheck.setStyle("-fx-font-size: 14px;");

        Label themeDescription = new Label("Dark mode applies to all windows and dialogs");
        themeDescription.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        VBox themeBox = new VBox(5, darkModeCheck, themeDescription);
        // --- 2. Theme Setting Example ---
        ComboBox<String> themeCombo = new ComboBox<>(
                FXCollections.observableArrayList("Light", "Dark (Not Implemented)", "Default"));
        themeCombo.setValue("Default");

        // --- 3. Auto-Save Setting Example ---
        CheckBox autoSaveCheck = new CheckBox("Enable Auto-Save (Every 5 mins)");
        autoSaveCheck.setSelected(true);

        Button saveBtn = new Button("Apply Changes");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8px 16px;");
        saveBtn.setOnAction(e -> {
            // Apply dark mode
            ThemeManager.getInstance().setDarkMode(darkModeCheck.isSelected());

            showInfo("Settings Applied", "Settings saved successfully!" +
                    (darkModeCheck.isSelected() ? "\n\n🌙 Dark mode is now enabled."
                            : "\n\n☀ Light mode is now enabled."));
            // Apply logic here
            showInfo("Settings Applied",
                    "Settings saved successfully! Changes will take effect on next generation/restart.");
            settingsStage.close();
        });

        layout.getChildren().addAll(
                title,

                new Separator(),
                new Label("Appearance & Automation"),
                themeBox,
                autoSaveCheck,
                new Separator(),
                saveBtn);

        Scene scene = new Scene(layout, 500, 450);
        ThemeManager.getInstance().registerScene(scene);
        settingsStage.setScene(scene);
        settingsStage.showAndWait();
    }

    private void showMainApplication(Stage stage) {
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
        Button resetBtn = new Button("🔄 Reset");
        loadBtn.setStyle(buttonStyle);
        generateBtn.setStyle(buttonStyle);
        saveBtn.setStyle(buttonStyle);
        validateBtn.setStyle(buttonStyle);
        exportBtn.setStyle(buttonStyle);
        importScheduleBtn.setStyle(buttonStyle);
        studentPortalBtn.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8px 16px; -fx-font-weight: bold; -fx-cursor: hand;");
        resetBtn.setStyle(
                "-fx-background-color: #E53935; -fx-text-fill: white; -fx-padding: 8px 16px; -fx-font-weight: bold; -fx-cursor: hand;");
        toolBar.setStyle(toolBarStyle);

        // Add tooltips to buttons
        addStyledTooltip(loadBtn, "Load CSV data files (courses, classrooms, students, attendance)");
        addStyledTooltip(generateBtn, "Automatically generate exam schedule based on loaded data");
        addStyledTooltip(saveBtn, "Save the current exam schedule to a file");
        addStyledTooltip(validateBtn, "Check the schedule for conflicts and constraint violations");
        addStyledTooltip(exportBtn, "Export the schedule to PDF or other formats");
        addStyledTooltip(importScheduleBtn, "Import a previously saved exam schedule");
        addStyledTooltip(studentPortalBtn, "Open student portal to view individual exam schedules");
        addStyledTooltip(resetBtn, "Clear all data and reset the application");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolBar.getItems().addAll(loadBtn, new Separator(), generateBtn, validateBtn, new Separator(),
                saveBtn, exportBtn, importScheduleBtn, new Separator(), studentPortalBtn, spacer, resetBtn);

        loadBtn.setOnAction(e -> handleLoad(stage));
        generateBtn.setOnAction(e -> handleGenerateSchedule());
        saveBtn.setOnAction(e -> handleSave(stage));
        validateBtn.setOnAction(e -> handleValidate());
        exportBtn.setOnAction(e -> handleExport(stage));
        importScheduleBtn.setOnAction(e -> handleImportSchedule(stage));
        studentPortalBtn.setOnAction(e -> showStudentPortal(stage));
        resetBtn.setOnAction(e -> handleReset(stage));

        VBox leftPane = createConfigurationPanel(buttonStyle);
        table = createExamTable(stage);

        Label scheduleTitle = new Label("📋 Exam Schedule");
        scheduleTitle.setStyle(titleStyle);

        // Search filter TextField
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search by course code or classroom...");
        searchField.setStyle(
                "-fx-font-size: 13px; -fx-padding: 8px 12px; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #D0D0D0;");
        searchField.setPrefWidth(350);

        // Initialize filtered list
        filteredExams = new FilteredList<>(exams, p -> true);

        // Bind filter to search field text
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredExams.setPredicate(examEntry -> {
                // If filter text is empty, display all exams
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                // Filter by course code
                if (examEntry.getCourseId() != null
                        && examEntry.getCourseId().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                // Filter by classroom ID
                if (examEntry.getRoomId() != null && examEntry.getRoomId().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false; // Does not match
            });
        });

        // Wrap filtered list in sorted list for table column sorting
        SortedList<ExamEntry> sortedExams = new SortedList<>(filteredExams);
        sortedExams.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedExams);

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        Label searchLabel = new Label("🔍 Filter:");
        searchLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + primaryColor + ";");
        searchBox.getChildren().addAll(searchLabel, searchField);

        VBox centerPane = new VBox(10, scheduleTitle, searchBox, table);
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
        ThemeManager.getInstance().registerScene(sc);
        stage.setScene(sc);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.centerOnScreen();
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
        loginStage.setTitle("🎓 Student Portal");

        // Modern, gradient arka plan
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%);");

        // Üst banner
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(30, 20, 20, 20));

        Label brandLabel = new Label("🎓");
        brandLabel.setStyle("-fx-font-size: 48px;");

        Label titleLabel = new Label("STUDENT PORTAL");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");

        Label subtitleLabel = new Label("Secure Access to Your Exam Schedule");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.9);");

        header.getChildren().addAll(brandLabel, titleLabel, subtitleLabel);
        root.setTop(header);

        // Ana login kartı
        VBox centerBox = new VBox(25);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(0, 40, 40, 40));

        VBox loginCard = new VBox(20);
        loginCard.setAlignment(Pos.CENTER);
        loginCard.setPadding(new Insets(40, 50, 40, 50));
        loginCard.setMaxWidth(500);
        loginCard.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 5);");

        // Hoş geldiniz mesajı
        Label welcomeLabel = new Label("Welcome Back");
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        Label instructionLabel = new Label("Enter your student ID to view your exam schedule");
        instructionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #636e72;");
        instructionLabel.setWrapText(true);
        instructionLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        VBox welcomeBox = new VBox(8, welcomeLabel, instructionLabel);
        welcomeBox.setAlignment(Pos.CENTER);

        // Arama kutusu - Modern tasarım
        VBox searchBox = new VBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPrefWidth(380);

        Label searchLabel = new Label("🔍 Student ID");
        searchLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #636e72;");

        TextField searchField = new TextField();
        searchField.setPromptText("Type to search...");
        searchField.setPrefHeight(45);
        searchField.setStyle("-fx-font-size: 14px; " +
                "-fx-background-radius: 8; " +
                "-fx-border-color: #dfe6e9; " +
                "-fx-border-radius: 8; " +
                "-fx-border-width: 2; " +
                "-fx-padding: 0 15 0 15;");

        // Focus efekti
        searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                searchField.setStyle("-fx-font-size: 14px; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #667eea; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 2; " +
                        "-fx-padding: 0 15 0 15;");
            } else {
                searchField.setStyle("-fx-font-size: 14px; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #dfe6e9; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 2; " +
                        "-fx-padding: 0 15 0 15;");
            }
        });

        // ComboBox - Dropdown listesi
        ComboBox<String> studentCombo = new ComboBox<>();
        studentCombo.setPromptText("Select your student ID");
        studentCombo.setPrefHeight(45);
        studentCombo.setPrefWidth(380);
        studentCombo.setStyle("-fx-font-size: 14px; -fx-background-radius: 8;");

        List<String> sortedStudentIDs = dataManager.getStudents().stream()
                .map(Student::getStudentID)
                .sorted()
                .collect(Collectors.toList());
        studentCombo.getItems().addAll(sortedStudentIDs);

        // Arama fonksiyonu
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                studentCombo.getItems().setAll(sortedStudentIDs);
                studentCombo.setValue(null);
            } else {
                List<String> filtered = sortedStudentIDs.stream()
                        .filter(id -> id.toLowerCase().contains(newValue.toLowerCase()))
                        .collect(Collectors.toList());
                studentCombo.getItems().setAll(filtered);
                if (!filtered.isEmpty()) {
                    studentCombo.setValue(filtered.get(0));
                }
            }
        });

        searchBox.getChildren().addAll(searchLabel, searchField, studentCombo);

        // Login butonu - Modern, gradient
        Button loginBtn = new Button("ACCESS SCHEDULE →");
        addStyledTooltip(loginBtn, "View your personal exam schedule");

        loginBtn.setPrefHeight(50);
        loginBtn.setPrefWidth(380);
        loginBtn.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 8, 0, 0, 3);");

        // Hover efekti
        loginBtn.setOnMouseEntered(
                e -> loginBtn.setStyle("-fx-background-color: linear-gradient(to right, #5a67d8, #6b3fa0); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand; " +
                        "-fx-scale-x: 1.02; " +
                        "-fx-scale-y: 1.02; " +
                        "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.6), 12, 0, 0, 4);"));

        loginBtn.setOnMouseExited(
                e -> loginBtn.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 8, 0, 0, 3);"));

        loginBtn.setOnAction(e -> {
            String studentId = studentCombo.getValue();
            if (studentId != null && !studentId.isEmpty()) {
                Student student = dataManager.getStudentByID(studentId);
                if (student != null) {
                    loginStage.close();
                    showStudentSchedule(owner, student);
                }
            } else {
                // Hata animasyonu
                loginCard.setStyle("-fx-background-color: white; " +
                        "-fx-background-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(231,76,60,0.3), 20, 0, 0, 5);");

                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(0.5));
                pause.setOnFinished(ev -> loginCard.setStyle("-fx-background-color: white; " +
                        "-fx-background-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 5);"));
                pause.play();

                showWarning("Selection Required", "Please select a student ID from the dropdown list.");
            }
        });

        // Enter tuşu desteği
        searchField.setOnAction(e -> loginBtn.fire());
        studentCombo.setOnAction(e -> searchField.requestFocus());

        // İstatistik kartları
        HBox statsBox = new HBox(15);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setPadding(new Insets(10, 0, 0, 0));

        VBox totalStudentsCard = createInfoCard("👥", String.valueOf(dataManager.getStudents().size()),
                "Total Students");
        VBox totalExamsCard = createInfoCard("📝", String.valueOf(dataManager.getCourses().size()), "Total Exams");
        VBox examDaysCard = createInfoCard("📅", String.valueOf(daysSpinner.getValue()), "Exam Days");

        statsBox.getChildren().addAll(totalStudentsCard, totalExamsCard, examDaysCard);

        loginCard.getChildren().addAll(welcomeBox, new Separator(), searchBox, loginBtn, new Separator(), statsBox);
        centerBox.getChildren().add(loginCard);
        root.setCenter(centerBox);

        // Alt bilgi
        Label footerLabel = new Label("Secure Access • Data Protected • Privacy First");
        footerLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px;");

        VBox footer = new VBox(footerLabel);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(0, 0, 20, 0));
        root.setBottom(footer);

        Scene scene = new Scene(root, 600, 700);
        ThemeManager.getInstance().registerScene(scene);
        loginStage.setScene(scene);
        loginStage.show();
    }

    // Yardımcı metod: Bilgi kartları oluşturur
    private VBox createInfoCard(String icon, String value, String label) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        card.setPrefWidth(100);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        Label textLabel = new Label(label);
        textLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #636e72;");
        textLabel.setWrapText(true);
        textLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        card.getChildren().addAll(iconLabel, valueLabel, textLabel);
        return card;
    }

    private void showStudentSchedule(Stage owner, Student student) {
        Stage scheduleStage = new Stage();
        scheduleStage.initOwner(owner);
        scheduleStage.setTitle("📅 My Exam Schedule");
        scheduleStage.setMaximized(false);

        // Modern gradient background
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f6f8;"); // Biraz daha yumuşak gri

        // --- 1. HEADER (AYNI KALIYOR) ---
        VBox header = new VBox(15);
        header.setPadding(new Insets(25, 30, 40, 30)); // Alt padding artırıldı (Navigasyon için yer)
        header.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 5);");

        // ... (Header içindeki Avatar, İsim, İstatistik Kartları kodları AYNEN kalsın)
        // ...
        // (Buraya senin yazdığın Header kodlarını tekrar yapıştırabilirsin, veya
        // aşağıda özet geçeyim mi?)
        // --- SENİN KODLARININ AYNISI BAŞLANGIÇ ---
        HBox profileBox = new HBox(20);
        profileBox.setAlignment(Pos.CENTER_LEFT);

        VBox avatarBox = new VBox();
        avatarBox.setAlignment(Pos.CENTER);
        avatarBox.setPrefSize(70, 70);
        avatarBox.setStyle(
                "-fx-background-color: white; -fx-background-radius: 35; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        Label avatarLabel = new Label("👤");
        avatarLabel.setStyle("-fx-font-size: 36px;");
        avatarBox.getChildren().add(avatarLabel);

        VBox studentInfo = new VBox(5);
        Label studentNameLabel = new Label(student.getStudentID());
        studentNameLabel.setStyle(
                "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");

        LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                ? examStartDatePicker.getValue()
                : LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysSpinner.getValue() - 1);
        Label dateLabel = new Label("📅 " + startDate.toString() + " - " + endDate.toString());
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.9);");
        studentInfo.getChildren().addAll(studentNameLabel, dateLabel);

        // İstatistikleri hesapla
        List<Exam> studentExams = getStudentExams(student);
        int totalExams = studentExams.size();
        long busyDays = studentExams.stream().map(e -> e.getTimeSlot().getDay()).distinct().count();
        double avgExamsPerDay = busyDays > 0 ? (double) totalExams / busyDays : 0;

        HBox statsBox = new HBox(15);
        statsBox.setAlignment(Pos.CENTER_RIGHT);
        // Kart renklerini biraz şeffaflaştırdım ki background üzerinde sırııtmasın
        statsBox.getChildren().addAll(
                createModernStatCard("📝", String.valueOf(totalExams), "Total Exams", "rgba(255,255,255,0.2)"),
                createModernStatCard("📅", String.valueOf(busyDays), "Busy Days", "rgba(255,255,255,0.2)"),
                createModernStatCard("📊", String.format("%.1f", avgExamsPerDay), "Avg/Day", "rgba(255,255,255,0.2)"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        profileBox.getChildren().addAll(avatarBox, studentInfo, spacer, statsBox);
        header.getChildren().add(profileBox);

        // --- Header'ı Root'a ekle ---
        // Ancak navigasyonu header'ın içine değil, hemen altına bindireceğiz (Overlay
        // etkisi)
        root.setTop(header);

        // --- 2. MODERN NAVİGASYON (TAB YERİNE) ---

        // İçerik Alanı (Değişken Kısım)
        StackPane contentArea = new StackPane();
        contentArea.setPadding(new Insets(30, 20, 20, 20));

        // Görünümleri Hazırla
        VBox dashboardView = createDashboardView(studentExams);
        ThemeManager.getInstance().styleNode(dashboardView);

        ScrollPane calendarScroll = new ScrollPane(createEnhancedCalendarView(studentExams));
        calendarScroll.setFitToWidth(true);
        calendarScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        ThemeManager.getInstance().styleNode(calendarScroll);

        // Buradaki scrollPane mantığına dikkat (Önceki sorudaki düzeltme)
        Node analyticsView = createStatisticsView(student, studentExams); // VBox değil Node dönüyor artık
        ThemeManager.getInstance().styleNode(analyticsView);

        // Navigasyon Butonları
        HBox navBar = new HBox(15);
        navBar.setAlignment(Pos.CENTER);
        navBar.setPadding(new Insets(10, 20, 10, 20));
        // Yüzen Menü Stili (Floating Pill Design)
        navBar.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 30; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4); " +
                "-fx-max-width: 500;"); // Genişliği sınırla

        // Toggle Grubu (Sadece biri seçili olsun diye)
        ToggleGroup navGroup = new ToggleGroup();

        ToggleButton btnDash = createNavButton("📊 Dashboard", navGroup, "View dashboard overview");
        ToggleButton btnCal = createNavButton("📅 Calendar", navGroup, "View exam calendar");
        ToggleButton btnStats = createNavButton("📈 Analytics", navGroup, "View performance analytics");

        // Aksiyonlar
        btnDash.setOnAction(e -> contentArea.getChildren().setAll(dashboardView));
        btnCal.setOnAction(e -> contentArea.getChildren().setAll(calendarScroll));
        btnStats.setOnAction(e -> contentArea.getChildren().setAll(analyticsView));

        // Varsayılan Seçim
        btnDash.setSelected(true);
        contentArea.getChildren().setAll(dashboardView);

        navBar.getChildren().addAll(btnDash, btnCal, btnStats);

        // Header ile İçeriği Birleştiren Layout
        // Navigasyon çubuğunu header'ın bittiği yere "yarı yarıya" bindirmek için
        // StackPane kullanabiliriz
        // Ama şimdilik basit olması için Header'ın altına VBox ile ekleyelim, fakat
        // negatif margin verelim.

        VBox mainContent = new VBox();
        mainContent.getChildren().addAll(navBar, contentArea);
        VBox.setMargin(navBar, new Insets(-25, 0, 0, 0)); // YUKARI KAYDIR (Header'ın üstüne binsin)
        mainContent.setAlignment(Pos.TOP_CENTER);

        root.setCenter(mainContent);

        // --- 3. FOOTER (EXPORT) ---
        HBox bottomBox = new HBox(20);
        bottomBox.setPadding(new Insets(20, 30, 20, 30));
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");

        Label exportLabel = new Label("Export Options:");
        exportLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        Button textBtn = createModernExportButton("📄", "Text File", "#3498db", "Export schedule to text file");
        textBtn.setOnAction(e -> exportStudentSchedule(scheduleStage, student, studentExams));

        Button icalBtn = createModernExportButton("📅", "Calendar", "#2ecc71", "Export schedule to iCal format");
        icalBtn.setOnAction(e -> exportAsICalendar(scheduleStage, student, studentExams));

        Button printBtn = createModernExportButton("🖨", "Print (PDF)", "#9e0d0d",
                "Export schedule to PDF for printing");
        printBtn.setOnAction(e -> exportAsPDF(scheduleStage, student, studentExams));

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #636e72; -fx-font-size: 13px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> scheduleStage.close());

        bottomBox.getChildren().addAll(exportLabel, textBtn, icalBtn, printBtn, bottomSpacer, closeBtn);
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 950, 700);
        ThemeManager.getInstance().registerScene(scene);
        scheduleStage.setScene(scene);
        scheduleStage.setMinWidth(900);
        scheduleStage.show();
    }

    // --- TOOLTIP HELPER ---
    private void addStyledTooltip(javafx.scene.control.Control node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(javafx.util.Duration.millis(100)); // Show almost instantly
        tooltip.setStyle(
                "-fx-font-size: 14px; -fx-background-color: #333; -fx-text-fill: white; -fx-padding: 8px; -fx-background-radius: 5;");
        node.setTooltip(tooltip);
    }

    // --- YENİ YARDIMCI METOT (Navigasyon Butonları İçin) ---
    private ToggleButton createNavButton(String text, ToggleGroup group, String tooltipText) {
        ToggleButton btn = new ToggleButton(text);
        addStyledTooltip(btn, tooltipText);
        btn.setToggleGroup(group);
        btn.setPrefWidth(140);
        btn.setPrefHeight(35);
        btn.setCursor(javafx.scene.Cursor.HAND);

        // CSS STİLİ (Normal ve Seçili Hali)
        // Seçili değilken: Beyaz arka plan, Gri yazı, Kenarlık yok
        // Seçiliyken: Mor/Mavi arka plan, Beyaz yazı, Yuvarlak köşe
        String baseStyle = "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-insets: 0;";

        btn.styleProperty().bind(javafx.beans.binding.Bindings.when(btn.selectedProperty())
                .then(baseStyle + "-fx-background-color: #667eea; -fx-text-fill: white;") // Seçili
                .otherwise(baseStyle
                        + "-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-border-color: transparent;") // Normal
        );

        // Hover Efekti (Opsiyonel)
        btn.setOnMouseEntered(e -> {
            if (!btn.isSelected())
                btn.setStyle(baseStyle + "-fx-background-color: #f0f2f5; -fx-text-fill: #2d3436;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isSelected())
                btn.setStyle(baseStyle + "-fx-background-color: transparent; -fx-text-fill: #7f8c8d;");
        });

        return btn;
    }

    // --- HEADER İÇİN KART METODU (Yazılar Beyaz) ---
    private VBox createModernStatCard(String icon, String value, String label, String bgColor) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 15, 10, 15));
        card.setAlignment(Pos.CENTER);

        card.setStyle("-fx-background-color: " + bgColor + "; " +
                "-fx-background-radius: 15;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 20px; -fx-text-fill: rgba(255,255,255,0.9);");

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.8);");

        card.getChildren().addAll(iconLbl, valueLbl, labelLbl);
        return card;
    }

    // Dashboard görünümü oluşturucu
    private VBox createDashboardView(List<Exam> studentExams) {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(20));

        // Yaklaşan sınavlar bölümü
        VBox upcomingSection = new VBox(15);
        Label upcomingTitle = new Label("📌 Upcoming Exams");
        upcomingTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        VBox upcomingExams = new VBox(10);
        LocalDate today = LocalDate.now();
        LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                ? examStartDatePicker.getValue()
                : today;

        studentExams.stream()
                .sorted(Comparator.comparing((Exam e) -> e.getTimeSlot().getDay())
                        .thenComparing(e -> e.getTimeSlot().getSlotNumber()))
                .limit(5)
                .forEach(exam -> upcomingExams.getChildren().add(createUpcomingExamCard(exam, startDate)));

        if (upcomingExams.getChildren().isEmpty()) {
            Label noExams = new Label("🎉 No exams scheduled");
            noExams.setStyle("-fx-font-size: 14px; -fx-text-fill: #95a5a6; -fx-font-style: italic;");
            upcomingExams.getChildren().add(noExams);
        }

        upcomingSection.getChildren().addAll(upcomingTitle, upcomingExams);

        // Hızlı istatistikler bölümü
        VBox quickStatsSection = new VBox(15);
        Label statsTitle = new Label("📊 Quick Statistics");
        statsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        GridPane quickStats = createQuickStatsGrid(studentExams);
        quickStatsSection.getChildren().addAll(statsTitle, quickStats);

        dashboard.getChildren().addAll(upcomingSection, new Separator(), quickStatsSection);

        ScrollPane scrollPane = new ScrollPane(dashboard);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        return new VBox(scrollPane);
    }

    // Yaklaşan sınav kartı
    private HBox createUpcomingExamCard(Exam exam, LocalDate startDate) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 1);");

        LocalDate examDate = startDate.plusDays(exam.getTimeSlot().getDay() - 1);

        // Tarih kutusu
        VBox dateBox = new VBox(2);
        dateBox.setAlignment(Pos.CENTER);
        dateBox.setPrefWidth(70);
        dateBox.setStyle("-fx-background-color: #667eea; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 10;");

        Label monthLabel = new Label(examDate.getMonth().toString().substring(0, 3));
        monthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.9); -fx-font-weight: bold;");

        Label dayLabel = new Label(String.valueOf(examDate.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");

        dateBox.getChildren().addAll(monthLabel, dayLabel);

        // Sınav detayları
        VBox detailsBox = new VBox(5);
        VBox.setVgrow(detailsBox, Priority.ALWAYS);

        Label courseLabel = new Label(exam.getCourse().getCourseCode());
        courseLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        Label nameLabel = new Label(exam.getCourse().getCourseName());
        nameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #636e72;");
        nameLabel.setWrapText(true);

        List<String> timeSlots = getTimeSlotsFromUI.get();
        String timeSlot = "";
        try {
            timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
        } catch (Exception e) {
            timeSlot = "Slot " + exam.getTimeSlot().getSlotNumber();
        }

        Label timeLabel = new Label("⏰ " + timeSlot + "  •  📍 Room " + exam.getClassroom().getClassroomID());
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");

        detailsBox.getChildren().addAll(courseLabel, nameLabel, timeLabel);

        card.getChildren().addAll(dateBox, detailsBox);
        return card;
    }

    // Hızlı istatistikler grid'i
    private GridPane createQuickStatsGrid(List<Exam> studentExams) {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);

        Map<Integer, Long> examsPerDay = studentExams.stream()
                .collect(Collectors.groupingBy(e -> e.getTimeSlot().getDay(), Collectors.counting()));

        int busiestDayNum = examsPerDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(1);

        long maxExamsInDay = examsPerDay.values().stream().max(Long::compare).orElse(0L);

        grid.add(createQuickStatItem("🏆", "Busiest Day", "Day " + busiestDayNum), 0, 0);
        grid.add(createQuickStatItem("📚", "Max Exams/Day", String.valueOf(maxExamsInDay)), 1, 0);
        grid.add(createQuickStatItem("✅", "Completion", "0/" + studentExams.size()), 2, 0);

        return grid;
    }

    // Hızlı stat item
    private VBox createQuickStatItem(String icon, String label, String value) {
        VBox item = new VBox(8);
        item.setPadding(new Insets(15));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle("-fx-background-color: #f8f9fa; " +
                "-fx-background-radius: 8; " +
                "-fx-min-width: 180;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        Label textLabel = new Label(label);
        textLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #636e72; -fx-font-weight: 600;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        item.getChildren().addAll(iconLabel, textLabel, valueLabel);
        return item;
    }

    // Detay item oluşturucu

    // Modern export butonu
    private Button createModernExportButton(String icon, String text, String color, String tooltipText) {
        Button btn = new Button(icon + " " + text);
        addStyledTooltip(btn, tooltipText);
        btn.setStyle("-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: 600; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 10px 20px; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, " + color + "40, 4, 0, 0, 2);");

        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: derive(" + color + ", -10%); " +
                    "-fx-text-fill: white; " +
                    "-fx-font-weight: 600; " +
                    "-fx-font-size: 13px; " +
                    "-fx-padding: 10px 20px; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand; " +
                    "-fx-scale-x: 1.03; " +
                    "-fx-scale-y: 1.03; " +
                    "-fx-effect: dropshadow(gaussian, " + color + "60, 6, 0, 0, 3);");
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: " + color + "; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-weight: 600; " +
                    "-fx-font-size: 13px; " +
                    "-fx-padding: 10px 20px; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, " + color + "40, 4, 0, 0, 2);");
        });

        return btn;
    }

    private GridPane createEnhancedCalendarView(List<Exam> studentExams) {
        GridPane calendar = new GridPane();
        calendar.setHgap(1);
        calendar.setVgap(1);
        calendar.setPadding(new Insets(20));
        calendar.setStyle("-fx-background-color: white; -fx-border-radius: 10;");

        int maxDay = Math.max(daysSpinner.getValue(),
                studentExams.stream().mapToInt(e -> e.getTimeSlot().getDay()).max().orElse(0));
        List<String> timeSlots = getTimeSlotsFromUI.get();

        LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                ? examStartDatePicker.getValue()
                : LocalDate.now();

        // Başlık satırı
        Label cornerLabel = new Label("TIME SLOT");
        cornerLabel.setStyle("-fx-font-weight: bold; -fx-padding: 15px; " +
                "-fx-background-color: #2c3e50; -fx-text-fill: white; " +
                "-fx-alignment: center;");
        cornerLabel.setMinWidth(150);
        calendar.add(cornerLabel, 0, 0);

        for (int day = 1; day <= maxDay; day++) {
            LocalDate currentDate = startDate.plusDays(day - 1);
            String dayLabel = "DAY " + day + "\n" + currentDate.getDayOfWeek().toString().substring(0, 3) +
                    " " + currentDate.getDayOfMonth();

            Label dayLabelNode = new Label(dayLabel);
            dayLabelNode.setStyle("-fx-font-weight: bold; -fx-padding: 15px; " +
                    "-fx-background-color: #3498db; -fx-text-fill: white; " +
                    "-fx-alignment: center; -fx-min-height: 100;");
            dayLabelNode.setMinWidth(200);
            calendar.add(dayLabelNode, day, 0);
        }

        // Zaman slotları için satırlar
        for (int slot = 0; slot < timeSlots.size(); slot++) {
            Label slotLabel = new Label(timeSlots.get(slot));
            slotLabel.setStyle("-fx-font-weight: bold; -fx-padding: 15px; " +
                    "-fx-background-color: #34495e; -fx-text-fill: white; " +
                    "-fx-alignment: center; -fx-min-height: 120;");
            slotLabel.setMinWidth(150);
            calendar.add(slotLabel, 0, slot + 1);

            for (int day = 1; day <= maxDay; day++) {
                VBox cell = new VBox(8);
                cell.setMinWidth(200);
                cell.setMinHeight(120);
                cell.setPadding(new Insets(10));
                cell.setAlignment(Pos.TOP_CENTER);
                cell.setStyle("-fx-border-color: #ecf0f1; -fx-border-width: 1; " +
                        "-fx-background-color: #f8f9fa;");

                // Bu gün ve slot için sınavı bul
                final int currentDay = day;
                final int currentSlot = slot + 1;
                Optional<Exam> examOpt = studentExams.stream()
                        .filter(e -> e.getTimeSlot().getDay() == currentDay &&
                                e.getTimeSlot().getSlotNumber() == currentSlot)
                        .findFirst();

                if (examOpt.isPresent()) {
                    Exam exam = examOpt.get();

                    // Renk kodu: öğrenci sayısına göre
                    String bgColor = exam.getStudentCount() > 50 ? "#fff3cd"
                            : exam.getStudentCount() > 30 ? "#d4edda" : "#d1ecf1";

                    cell.setStyle("-fx-border-color: #dee2e6; -fx-border-width: 2; " +
                            "-fx-background-color: " + bgColor + "; " +
                            "-fx-border-radius: 5; -fx-background-radius: 5;");

                    Label courseCode = new Label(exam.getCourse().getCourseCode());
                    courseCode.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; " +
                            "-fx-text-fill: #2c3e50;");

                    Label courseName = new Label(exam.getCourse().getCourseName());
                    courseName.setStyle("-fx-font-size: 11px; -fx-text-fill: #495057;");
                    courseName.setWrapText(true);

                    HBox details = new HBox(10);
                    details.setAlignment(Pos.CENTER);

                    Label roomLabel = new Label("📍 " + exam.getClassroom().getClassroomID());
                    roomLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");

                    Label timeLabel = new Label("⏰ " + timeSlots.get(slot));
                    timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");

                    details.getChildren().addAll(roomLabel, timeLabel);

                    cell.getChildren().addAll(courseCode, courseName, details);
                } else {
                    Label freeLabel = new Label("No Exam");
                    freeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6; " +
                            "-fx-font-style: italic;");
                    cell.getChildren().add(freeLabel);
                }

                calendar.add(cell, day, slot + 1);
            }
        }

        return calendar;
    }

    private VBox createStatisticsView(Student student, List<Exam> studentExams) {
        // 1. ANA KAPLAYICI (Return edilecek olan)
        VBox mainLayout = new VBox();
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: white; -fx-border-radius: 10; -fx-background-radius: 10;");

        // 2. İÇERİK KUTUSU (Scroll edilecek olan tüm istatistikler burada)
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(5)); // Scrollbar ile içerik arasına mesafe

        Label title = new Label("📊 Schedule Statistics");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // --- İSTATİSTİK KARTLARI ---
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(15);

        int totalExams = studentExams.size();
        Map<Integer, Long> examsPerDay = studentExams.stream()
                .collect(Collectors.groupingBy(e -> e.getTimeSlot().getDay(), Collectors.counting()));

        int busiestDay = examsPerDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(1);

        long busyDayCount = examsPerDay.size();

        statsGrid.add(createStatCard("Total Exams", String.valueOf(totalExams), "#3498db"), 0, 0);
        statsGrid.add(createStatCard("Active Days", String.valueOf(busyDayCount), "#2ecc71"), 1, 0);
        statsGrid.add(createStatCard("Heaviest Day", "Day " + busiestDay, "#e74c3c"), 2, 0);

        // --- GÜNLÜK DAĞILIM ---
        VBox distributionBox = new VBox(10);
        distributionBox.setPadding(new Insets(20));
        distributionBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label distTitle = new Label("📈 Daily Exam Load");
        distTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #34495e;");

        distributionBox.getChildren().add(distTitle);

        examsPerDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    HBox dayBar = new HBox(10);
                    dayBar.setAlignment(Pos.CENTER_LEFT);

                    Label dayLabel = new Label("Day " + entry.getKey());
                    dayLabel.setPrefWidth(60);
                    dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d;");

                    double progressValue = Math.min(entry.getValue() / 5.0, 1.0);
                    ProgressBar progress = new ProgressBar(progressValue);
                    progress.setPrefWidth(250);
                    String color = entry.getValue() <= 1 ? "#2ecc71" : (entry.getValue() <= 2 ? "#f1c40f" : "#e74c3c");
                    progress.setStyle("-fx-accent: " + color + "; -fx-control-inner-background: #ecf0f1;");

                    Label countLabel = new Label(entry.getValue() + " exam(s)");
                    countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

                    dayBar.getChildren().addAll(dayLabel, progress, countLabel);
                    distributionBox.getChildren().add(dayBar);
                });

        // --- ÖNERİLER ---
        VBox recommendationsBox = new VBox(10);
        recommendationsBox.setPadding(new Insets(20));
        recommendationsBox.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffeaa7; " +
                "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label recTitle = new Label("💡 Smart Insights");
        recTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #856404;");

        Label recText = new Label();
        recText.setWrapText(true);
        recText.setStyle("-fx-font-size: 13px; -fx-text-fill: #856404;");

        StringBuilder recBuilder = new StringBuilder();
        if (totalExams == 0) {
            recBuilder.append("• No exams found.\n");
        } else {
            if (examsPerDay.values().stream().anyMatch(count -> count >= 3)) {
                recBuilder.append("⚠ Warning: You have days with 3+ exams.\n");
            } else {
                recBuilder.append("✓ Balanced schedule.\n");
            }
        }
        recText.setText(recBuilder.toString());
        recommendationsBox.getChildren().addAll(recTitle, recText);

        // --- TÜM İÇERİĞİ CONTENT BOX'A EKLE ---
        contentBox.getChildren().addAll(title, statsGrid, distributionBox, recommendationsBox);

        // --- 3. SCROLLPANE OLUŞTURMA VE BAĞLAMA ---
        ScrollPane scrollPane = new ScrollPane(contentBox); // İçeriği ScrollPane'e koy
        scrollPane.setFitToWidth(true); // Yatayda sığdır
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // ScrollPane'i ana kutuya ekle ve büyüt
        mainLayout.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS); // Ana kutu büyürse ScrollPane de büyüsün

        return mainLayout; // VBox döndürür
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(150);
        card.setStyle("-fx-background-color: white; -fx-border-color: " + color + "40; " +
                "-fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-font-weight: bold;");

        card.getChildren().addAll(valueLabel, titleLabel);
        return card;
    }

    private void exportAsPDF(Stage owner, Student student, List<Exam> studentExams) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export as PDF");
        fileChooser.setInitialFileName(student.getStudentID() + "_schedule.pdf");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showSaveDialog(owner);
        if (file != null) {
            try {
                // PDF oluşturma işlemi
                createPDF(file, student, studentExams);
                showInfo("PDF Export", "Schedule exported successfully as PDF:\n" + file.getName());
                messages.add("📄 PDF exported for " + student.getStudentID());
            } catch (Exception e) {
                showError("PDF Export Failed", e.getMessage());
            }
        }
    }

    private void exportAsICalendar(Stage owner, Student student, List<Exam> studentExams) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export as iCalendar");
        fileChooser.setInitialFileName(student.getStudentID() + "_schedule.ics");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("iCalendar Files", "*.ics"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showSaveDialog(owner);
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {
                pw.println("BEGIN:VCALENDAR");
                pw.println("VERSION:2.0");
                pw.println("PRODID:-//Exam Scheduler//Student Schedule//EN");
                pw.println("CALSCALE:GREGORIAN");
                pw.println("METHOD:PUBLISH");
                pw.println("X-WR-CALNAME:Exam Schedule - " + student.getStudentID());
                pw.println("X-WR-TIMEZONE:Europe/Istanbul");

                List<String> timeSlots = getTimeSlotsFromUI.get();
                LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                        ? examStartDatePicker.getValue()
                        : LocalDate.now();

                for (Exam exam : studentExams) {
                    LocalDate examDate = startDate.plusDays(exam.getTimeSlot().getDay() - 1);
                    String timeSlot = "";
                    try {
                        timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
                    } catch (Exception e) {
                        timeSlot = "09:00-11:00";
                    }

                    String[] times = timeSlot.split("-");
                    String startTime = times[0].trim().replace(":", "") + "00";
                    String endTime = times.length > 1 ? times[1].trim().replace(":", "") + "00" : "110000";

                    String dateStr = examDate.toString().replace("-", "");

                    pw.println("BEGIN:VEVENT");
                    pw.println("UID:" + student.getStudentID() + "-" + exam.getCourse().getCourseCode() +
                            "-" + dateStr + "@examscheduler.edu");
                    pw.println("DTSTAMP:" + LocalDate.now().toString().replace("-", "") + "T120000Z");
                    pw.println("DTSTART:" + dateStr + "T" + startTime);
                    pw.println("DTEND:" + dateStr + "T" + endTime);
                    pw.println("SUMMARY:Exam: " + exam.getCourse().getCourseCode());
                    pw.println("DESCRIPTION:" + exam.getCourse().getCourseName() +
                            "\\nRoom: " + exam.getClassroom().getClassroomID());
                    pw.println("LOCATION:Room " + exam.getClassroom().getClassroomID());
                    pw.println("CATEGORIES:EXAM");
                    pw.println("STATUS:CONFIRMED");
                    pw.println("BEGIN:VALARM");
                    pw.println("TRIGGER:-P1D");
                    pw.println("ACTION:DISPLAY");
                    pw.println("DESCRIPTION:Reminder: Exam tomorrow!");
                    pw.println("END:VALARM");
                    pw.println("END:VEVENT");
                }

                pw.println("END:VCALENDAR");

                showInfo("Calendar Export",
                        "iCalendar file exported successfully!\n\n" +
                                "You can import this file to:\n" +
                                "• Google Calendar\n• Outlook\n• Apple Calendar\n• Any calendar app");
                messages.add("📅 iCalendar exported for " + student.getStudentID());

            } catch (Exception e) {
                showError("Export Failed", e.getMessage());
            }
        }
    }

    private void exportStudentSchedule(Stage owner, Student student, List<Exam> studentExams) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export My Schedule");
        chooser.setInitialFileName("my_exam_schedule_" + student.getStudentID() + ".txt");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {
                pw.println("╔══════════════════════════════════════════════════════╗");
                pw.println("║         PERSONAL EXAM SCHEDULE - " +
                        String.format("%-10s", student.getStudentID()) + "        ║");
                pw.println("╚══════════════════════════════════════════════════════╝\n");

                pw.println("Generated: " + LocalDate.now() + " " + java.time.LocalTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                pw.println();
                pw.println("═".repeat(60));

                List<String> timeSlots = getTimeSlotsFromUI.get();
                LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                        ? examStartDatePicker.getValue()
                        : LocalDate.now();

                int examNumber = 1;
                for (Exam exam : studentExams.stream()
                        .sorted(Comparator.comparing((Exam e) -> e.getTimeSlot().getDay())
                                .thenComparing(e -> e.getTimeSlot().getSlotNumber()))
                        .collect(Collectors.toList())) {

                    LocalDate examDate = startDate.plusDays(exam.getTimeSlot().getDay() - 1);
                    String timeSlot = "";
                    try {
                        timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
                    } catch (Exception e) {
                        timeSlot = "Slot " + exam.getTimeSlot().getSlotNumber();
                    }

                    String dayOfWeek = examDate.getDayOfWeek().toString();
                    pw.println("EXAM #" + examNumber++);
                    pw.println("-".repeat(40));
                    pw.println("📅 DATE:      " + examDate + " (" + dayOfWeek + ")");
                    pw.println("⏰ TIME:      " + timeSlot);
                    pw.println("📚 COURSE:    " + exam.getCourse().getCourseCode() + " - " +
                            exam.getCourse().getCourseName());

                    pw.println("📍 ROOM:      " + exam.getClassroom().getClassroomID() +
                            " (Capacity: " + exam.getClassroom().getCapacity() + ")");
                    pw.println("👥 STUDENTS:  " + exam.getStudentCount() + " enrolled");
                    pw.println();
                }

                // Statistics section
                pw.println("═".repeat(60));
                pw.println("\n📊 SCHEDULE STATISTICS");
                pw.println("-".repeat(30));
                pw.println("Total Exams: " + studentExams.size());

                Map<Integer, Long> examsPerDay = studentExams.stream()
                        .collect(Collectors.groupingBy(e -> e.getTimeSlot().getDay(), Collectors.counting()));

                pw.println("\nExams per Day:");
                examsPerDay.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            LocalDate dayDate = startDate.plusDays(entry.getKey() - 1);
                            pw.println("  Day " + entry.getKey() + " (" + dayDate.getDayOfWeek() +
                                    "): " + entry.getValue() + " exam(s)");
                        });

                // Recommendations
                pw.println("\n═".repeat(60));
                pw.println("\n💡 RECOMMENDATIONS");
                pw.println("-".repeat(20));

                if (studentExams.isEmpty()) {
                    pw.println("✓ No exams scheduled");
                } else {
                    boolean hasBusyDay = examsPerDay.values().stream().anyMatch(count -> count >= 3);

                    if (hasBusyDay) {
                        pw.println("⚠ You have days with 3+ exams. Plan your study time accordingly.");
                    }

                    pw.println("✓ Total study period: " + daysSpinner.getValue() + " days");
                    pw.println("✓ Average exams per day: " +
                            String.format("%.1f", studentExams.size() / (double) examsPerDay.size()));
                }

            } catch (Exception e) {
                showError("Export Failed", "Failed to export schedule:\n" + e.getMessage());
            }
        }
    }

    private void createPDF(File file, Student student, List<Exam> exams) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // --- TASARIM RENKLERİ ---
        BaseColor HEADER_COLOR = new BaseColor(0, 120, 212); // Kurumsal Mavi (#0078D4)
        BaseColor ROW_COLOR_ODD = new BaseColor(240, 240, 240); // Açık Gri
        BaseColor ROW_COLOR_EVEN = BaseColor.WHITE;

        // --- BAŞLIK KISMI ---
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
        Paragraph title = new Paragraph("Student Exam Schedule", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY);
        Paragraph subTitle = new Paragraph("Student ID: " + student.getStudentID(), subTitleFont);
        subTitle.setAlignment(Element.ALIGN_CENTER);
        subTitle.setSpacingAfter(20); // Altına boşluk bırak
        document.add(subTitle);

        // --- TABLO OLUŞTURMA (4 Sütun) ---
        PdfPTable table = new PdfPTable(new float[] { 2, 2, 4, 2 }); // Sütun genişlik oranları
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // --- TABLO BAŞLIKLARI ---
        String[] headers = { "Day / Date", "Time", "Course Info", "Location" };
        for (String headerTitle : headers) {
            PdfPCell header = new PdfPCell(
                    new Phrase(headerTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE)));
            header.setBackgroundColor(HEADER_COLOR);
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setVerticalAlignment(Element.ALIGN_MIDDLE);
            header.setPadding(8); // İç boşluk
            header.setBorderColor(BaseColor.GRAY);
            table.addCell(header);
        }

        // --- VERİLERİ DOLDURMA ---
        // Tarihe göre sırala
        exams.sort(Comparator.comparing((Exam e) -> e.getTimeSlot().getDay())
                .thenComparing(e -> e.getTimeSlot().getSlotNumber()));

        List<String> timeSlots = getTimeSlotsFromUI.get();
        LocalDate startDate = examStartDatePicker.getValue() != null ? examStartDatePicker.getValue() : LocalDate.now();

        boolean isOdd = true;
        for (Exam exam : exams) {
            BaseColor rowColor = isOdd ? ROW_COLOR_ODD : ROW_COLOR_EVEN;

            // Tarih ve Saat Hesapla
            LocalDate date = startDate.plusDays(exam.getTimeSlot().getDay() - 1);
            String timeSlotStr;
            try {
                timeSlotStr = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
            } catch (Exception e) {
                timeSlotStr = "Slot " + exam.getTimeSlot().getSlotNumber();
            }

            // 1. Hücre: Tarih
            addStyledCell(table, "Day " + exam.getTimeSlot().getDay() + "\n" + date.toString(), rowColor, true);

            // 2. Hücre: Saat
            addStyledCell(table, timeSlotStr, rowColor, true);

            // 3. Hücre: Ders Bilgisi (Ders Kodu kalın, adı normal)
            PdfPCell courseCell = new PdfPCell();

            // Ders Kodu Paragrafı
            Paragraph pCode = new Paragraph(exam.getCourse().getCourseCode(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
            pCode.setAlignment(Element.ALIGN_CENTER); // <--- İŞTE BU SATIR ORTALIYOR
            courseCell.addElement(pCode);

            courseCell.setBackgroundColor(rowColor);
            courseCell.setPadding(8);
            courseCell.setVerticalAlignment(Element.ALIGN_MIDDLE); // Dikeyde ortala
            courseCell.setUseAscender(true); // Dikey ortalamayı iyileştirir
            table.addCell(courseCell);

            // 4. Hücre: Oda
            PdfPCell roomCell = new PdfPCell(
                    new Phrase(exam.getClassroom().getClassroomID(), FontFactory.getFont(FontFactory.HELVETICA, 12)));
            roomCell.setBackgroundColor(rowColor);
            roomCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            roomCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            roomCell.setPadding(8);
            table.addCell(roomCell);

            isOdd = !isOdd; // Sıradaki satır rengini değiştir
        }

        document.add(table);

        // --- ALT BİLGİ ---
        Paragraph footer = new Paragraph("Generated by Exam Scheduler v2.0",
                FontFactory.getFont(FontFactory.COURIER, 8, BaseColor.GRAY));
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);

        document.close();
    }

    // Yardımcı Metod: Basit hücre ekleme
    private void addStyledCell(PdfPTable table, String text, BaseColor bgColor, boolean center) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(8);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (center)
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private List<Exam> getStudentExams(Student student) {
        List<Exam> studentExams = new ArrayList<>();

        if (dataManager.getSchedule() != null && student != null) {
            String studentId = student.getStudentID();
            for (Exam exam : dataManager.getSchedule().getExams()) {
                if (exam.isScheduled()) {
                    // Use studentID comparison instead of object reference
                    // This ensures newly added students are found
                    boolean isEnrolled = exam.getEnrolledStudents().stream()
                            .anyMatch(s -> s.getStudentID().equals(studentId));
                    if (isEnrolled) {
                        studentExams.add(exam);
                    }
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
        MenuItem returnHomeItem = new MenuItem("🏠 Return to Welcome Screen");
        MenuItem exitItem = new MenuItem("Exit");

        loadItem.setOnAction(e -> handleLoad(stage));
        saveItem.setOnAction(e -> handleSave(stage));
        exportItem.setOnAction(e -> handleExport(stage));
        returnHomeItem.setOnAction(e -> handleReturnToWelcome(stage));
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(loadItem, saveItem, exportItem, new SeparatorMenuItem(), returnHomeItem, exitItem);

        Menu editMenu = new Menu("Edit");
        MenuItem manageCourses = new MenuItem("Manage Courses...");
        MenuItem manageStudents = new MenuItem("Manage Students...");
        MenuItem manageClassrooms = new MenuItem("Manage Classrooms...");
        MenuItem manageAttendance = new MenuItem("Manage Attendance...");

        manageCourses.setOnAction(e -> showManageCourses(stage));
        manageStudents.setOnAction(e -> showManageStudents(stage));
        manageClassrooms.setOnAction(e -> showManageClassrooms(stage));
        manageAttendance.setOnAction(e -> showManageAttendance(stage));

        editMenu.getItems().addAll(manageCourses, manageStudents, manageClassrooms, manageAttendance);

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
        addStyledTooltip(addTs, "Add a new time slot");
        Button remTs = new Button("-");
        addStyledTooltip(remTs, "Remove selected time slot");
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
        addStyledTooltip(manageClassroomsBtn, "Manage classrooms list");
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

                addStyledTooltip(editBtn, "Edit this exam details");
                addStyledTooltip(deleteBtn, "Delete this exam from schedule");

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

        Stage loadDialog = new Stage();
        loadDialog.initOwner(owner);
        loadDialog.initModality(Modality.APPLICATION_MODAL);
        loadDialog.setTitle("📁 Load Data");

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("📁 Load Data");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0078D4;");

        Label subtitle = new Label("Choose how to load your data:");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        VBox option1 = createLoadOption(
                "📂 Load from Folder",
                "Load CSV files from a directory",
                "• students.csv\n• courses.csv\n• classrooms.csv\n• attendance.csv",
                "#4CAF50");

        Button loadFolderBtn = new Button("Select Folder");
        loadFolderBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8px 20px;");
        loadFolderBtn.setOnAction(e -> {
            loadDialog.close();
            loadFromFolder(owner);
        });
        option1.getChildren().add(loadFolderBtn);

        VBox option2 = createLoadOption(
                "📄 Load Individual Files",
                "Select each CSV file separately",
                "Choose files one by one for more control",
                "#2196F3");

        Button loadFilesBtn = new Button("Select Files");
        loadFilesBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 8px 20px;");
        loadFilesBtn.setOnAction(e -> {
            loadDialog.close();
            loadIndividualFiles(owner);
        });
        option2.getChildren().add(loadFilesBtn);

        VBox option3 = createLoadOption(
                "💼 Load from Backup",
                "Restore from a backup package",
                "Load a complete backup with all data",
                "#FF9800");

        Button loadBackupBtn = new Button("Select Backup");
        loadBackupBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 8px 20px;");
        loadBackupBtn.setOnAction(e -> {
            loadDialog.close();
            loadFromBackup(owner);
        });
        option3.getChildren().add(loadBackupBtn);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-padding: 8px 20px;");
        cancelBtn.setOnAction(e -> loadDialog.close());

        layout.getChildren().addAll(
                title,
                subtitle,
                new Separator(),
                option1,
                new Separator(),
                option2,
                new Separator(),
                option3,
                new Separator(),
                cancelBtn);

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white;");

        Scene scene = new Scene(scrollPane, 600, 750);
        ThemeManager.getInstance().registerScene(scene);
        loadDialog.setScene(scene);
        loadDialog.showAndWait();
    }

    private VBox createLoadOption(String title, String description, String details, String color) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-border-color: " + color
                + "; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

        Label detailsLabel = new Label(details);
        detailsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999; -fx-font-style: italic;");
        detailsLabel.setWrapText(true);

        box.getChildren().addAll(titleLabel, descLabel, detailsLabel);
        return box;
    }

    private void loadFromFolder(Stage owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder with CSV files");
        File dir = chooser.showDialog(owner);

        if (dir == null) {
            messages.add("⚠ Load cancelled.");
            return;
        }

        messages.add("📂 Loading data from: " + dir.getName() + "...");

        Stage progressStage = new Stage();
        progressStage.initOwner(owner);
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("Loading Data...");

        VBox progressBox = new VBox(15);
        progressBox.setPadding(new Insets(30));
        progressBox.setAlignment(Pos.CENTER);

        Label statusLabel = new Label("Scanning folder...");
        statusLabel.setStyle("-fx-font-size: 14px;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefSize(400, 200);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

        progressBox.getChildren().addAll(statusLabel, progressBar, logArea);
        Scene progressScene = new Scene(progressBox, 500, 350);
        ThemeManager.getInstance().registerScene(progressScene);
        progressStage.setScene(progressScene);
        progressStage.show();

        Task<LoadResult> loadTask = new Task<LoadResult>() {
            @Override
            protected LoadResult call() throws Exception {
                LoadResult result = new LoadResult();
                StringBuilder log = new StringBuilder();

                updateMessage("Scanning folder...");
                updateProgress(0, 100);
                log.append("=== LOADING DATA ===\n");
                log.append("Folder: ").append(dir.getAbsolutePath()).append("\n\n");

                try {
                    List<File> files = Files.list(dir.toPath())
                            .map(p -> p.toFile())
                            .collect(Collectors.toList());

                    log.append("Files found: ").append(files.size()).append("\n");
                    result.log = log.toString();
                    updateProgress(10, 100);

                    String studentsPath = findFile(files, "student");
                    String coursesPath = findFile(files, "course");
                    String classroomsPath = findFile(files, "classroom");
                    String attendancePath = findFile(files, "attendance");

                    log.append("\nFile Detection:\n");
                    log.append("  Students: ").append(studentsPath != null ? "✓ Found" : "✗ Missing").append("\n");
                    log.append("  Courses: ").append(coursesPath != null ? "✓ Found" : "✗ Missing").append("\n");
                    log.append("  Classrooms: ").append(classroomsPath != null ? "✓ Found" : "✗ Missing").append("\n");
                    log.append("  Attendance: ").append(attendancePath != null ? "✓ Found" : "✗ Missing")
                            .append("\n\n");

                    if (studentsPath == null || coursesPath == null || classroomsPath == null
                            || attendancePath == null) {
                        result.success = false;
                        result.error = "Missing required CSV files (students, courses, classrooms, attendance)";
                        log.append("❌ ERROR: Missing required files\n");
                        result.log = log.toString();
                        return result;
                    }

                    updateMessage("Clearing old data...");
                    updateProgress(20, 100);
                    dataManager.clearAllData();
                    log.append("✓ Cleared old data\n\n");

                    updateMessage("Loading students...");
                    updateProgress(30, 100);
                    log.append("Loading students...\n");
                    List<Student> loadedStudents = CSVParser.parseStudents(studentsPath);
                    dataManager.setStudents(loadedStudents);
                    log.append("✓ Loaded ").append(loadedStudents.size()).append(" students\n\n");
                    result.studentsCount = loadedStudents.size();

                    updateMessage("Loading courses...");
                    updateProgress(50, 100);
                    log.append("Loading courses...\n");
                    List<Course> loadedCourses = CSVParser.parseCourses(coursesPath);
                    dataManager.setCourses(loadedCourses);
                    log.append("✓ Loaded ").append(loadedCourses.size()).append(" courses\n\n");
                    result.coursesCount = loadedCourses.size();

                    updateMessage("Loading classrooms...");
                    updateProgress(70, 100);
                    log.append("Loading classrooms...\n");
                    List<Classroom> loadedClassrooms = CSVParser.parseClassrooms(classroomsPath);
                    dataManager.setClassrooms(loadedClassrooms);
                    log.append("✓ Loaded ").append(loadedClassrooms.size()).append(" classrooms\n\n");
                    result.classroomsCount = loadedClassrooms.size();

                    updateMessage("Loading attendance lists...");
                    updateProgress(85, 100);
                    log.append("Loading attendance lists...\n");
                    CSVParser.parseAttendanceLists(attendancePath, dataManager.getStudents(),
                            dataManager.getCourses());

                    int totalEnrollments = dataManager.getCourses().stream()
                            .mapToInt(c -> c.getEnrolledStudents().size())
                            .sum();
                    log.append("✓ Loaded attendance data (").append(totalEnrollments)
                            .append(" total enrollments)\n\n");
                    result.attendanceCount = totalEnrollments;

                    dataManager.setSourceFiles(
                            new File(studentsPath),
                            new File(coursesPath),
                            new File(classroomsPath),
                            attendancePath != null ? new File(attendancePath) : null);

                    updateMessage("Finalizing...");
                    updateProgress(95, 100);

                    log.append("=== VALIDATION ===\n");
                    int coursesWithStudents = (int) dataManager.getCourses().stream()
                            .filter(c -> !c.getEnrolledStudents().isEmpty())
                            .count();
                    log.append("Courses with students: ").append(coursesWithStudents)
                            .append(" / ").append(dataManager.getCourses().size()).append("\n");

                    if (coursesWithStudents == 0 && attendancePath != null) {
                        log.append("⚠ WARNING: No courses have enrolled students!\n");
                        result.warnings.add("Attendance data may be incorrectly formatted");
                    }

                    updateProgress(100, 100);
                    log.append("\n✅ DATA LOADED SUCCESSFULLY\n");
                    result.success = true;
                    result.log = log.toString();

                } catch (CSVParser.CSVParseException e) {
                    result.success = false;
                    result.error = "CSV Parse Error: " + e.getMessage();
                    log.append("\n❌ CSV Parse Error: ").append(e.getMessage()).append("\n");
                    result.log = log.toString();
                } catch (IOException e) {
                    result.success = false;
                    result.error = "File I/O Error: " + e.getMessage();
                    log.append("\n❌ I/O Error: ").append(e.getMessage()).append("\n");
                    result.log = log.toString();
                } catch (Exception e) {
                    result.success = false;
                    result.error = "Unexpected Error: " + e.getMessage();
                    log.append("\n❌ Unexpected Error: ").append(e.getMessage()).append("\n");
                    result.log = log.toString();
                }

                return result;
            }
        };

        statusLabel.textProperty().bind(loadTask.messageProperty());
        progressBar.progressProperty().bind(loadTask.progressProperty());
        loadTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            logArea.appendText(newMsg + "\n");
        });

        loadTask.setOnSucceeded(e -> {
            LoadResult result = loadTask.getValue();
            logArea.setText(result.log);

            progressStage.close();

            if (result.success) {
                updateClassroomsView();

                messages.add("✓ Data loaded successfully:");
                messages.add("  • Students: " + result.studentsCount);
                messages.add("  • Courses: " + result.coursesCount);
                messages.add("  • Classrooms: " + result.classroomsCount);
                if (result.attendanceCount > 0) {
                    messages.add("  • Enrollments: " + result.attendanceCount);
                }
                messages.add("🚀 Ready to generate schedule.");

                showInfo("Load Success",
                        "Data loaded successfully!\n\n" +
                                "Students: " + result.studentsCount + "\n" +
                                "Courses: " + result.coursesCount + "\n" +
                                "Classrooms: " + result.classroomsCount + "\n" +
                                (result.attendanceCount > 0 ? "Enrollments: " + result.attendanceCount : ""));
            } else {
                messages.add("❌ Load failed: " + result.error);
                showError("Load Failed", result.error + "\n\nCheck the log for details.");
                dataManager.clearAllData();
            }
        });

        loadTask.setOnFailed(e -> {
            progressStage.close();
            Throwable ex = loadTask.getException();
            messages.add("❌ Load failed: " + ex.getMessage());
            showError("Load Failed", "Unexpected error:\n" + ex.getMessage());
            dataManager.clearAllData();
        });

        new Thread(loadTask).start();
    }

    private void loadIndividualFiles(Stage owner) {
        Stage fileSelectionStage = new Stage();
        fileSelectionStage.initOwner(owner);
        fileSelectionStage.initModality(Modality.APPLICATION_MODAL);
        fileSelectionStage.setTitle("📄 Select Individual Files");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(15);

        Label title = new Label("Select CSV Files");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        grid.add(title, 0, 0, 3, 1);

        TextField studentsField = new TextField();
        studentsField.setPromptText("No file selected");
        studentsField.setEditable(false);
        studentsField.setPrefWidth(300);

        TextField coursesField = new TextField();
        coursesField.setPromptText("No file selected");
        coursesField.setEditable(false);
        coursesField.setPrefWidth(300);

        TextField classroomsField = new TextField();
        classroomsField.setPromptText("No file selected");
        classroomsField.setEditable(false);
        classroomsField.setPrefWidth(300);

        TextField attendanceField = new TextField();
        attendanceField.setPromptText("No file selected");
        attendanceField.setEditable(false);
        attendanceField.setPrefWidth(300);

        Button studentsBtn = new Button("Browse...");
        Button coursesBtn = new Button("Browse...");
        Button classroomsBtn = new Button("Browse...");
        Button attendanceBtn = new Button("Browse...");

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        // Set initial directory to last selected folder if available
        if (lastSelectedDirectory != null && lastSelectedDirectory.exists()) {
            fileChooser.setInitialDirectory(lastSelectedDirectory);
        }

        studentsBtn.setOnAction(e -> {
            if (lastSelectedDirectory != null && lastSelectedDirectory.exists()) {
                fileChooser.setInitialDirectory(lastSelectedDirectory);
            }
            File file = fileChooser.showOpenDialog(fileSelectionStage);
            if (file != null) {
                studentsField.setText(file.getAbsolutePath());
                lastSelectedDirectory = file.getParentFile();
            }
        });

        coursesBtn.setOnAction(e -> {
            if (lastSelectedDirectory != null && lastSelectedDirectory.exists()) {
                fileChooser.setInitialDirectory(lastSelectedDirectory);
            }
            File file = fileChooser.showOpenDialog(fileSelectionStage);
            if (file != null) {
                coursesField.setText(file.getAbsolutePath());
                lastSelectedDirectory = file.getParentFile();
            }
        });

        classroomsBtn.setOnAction(e -> {
            if (lastSelectedDirectory != null && lastSelectedDirectory.exists()) {
                fileChooser.setInitialDirectory(lastSelectedDirectory);
            }
            File file = fileChooser.showOpenDialog(fileSelectionStage);
            if (file != null) {
                classroomsField.setText(file.getAbsolutePath());
                lastSelectedDirectory = file.getParentFile();
            }
        });

        attendanceBtn.setOnAction(e -> {
            if (lastSelectedDirectory != null && lastSelectedDirectory.exists()) {
                fileChooser.setInitialDirectory(lastSelectedDirectory);
            }
            File file = fileChooser.showOpenDialog(fileSelectionStage);
            if (file != null) {
                attendanceField.setText(file.getAbsolutePath());
                lastSelectedDirectory = file.getParentFile();
            }
        });

        grid.add(new Label("Students CSV: *"), 0, 1);
        grid.add(studentsField, 1, 1);
        grid.add(studentsBtn, 2, 1);

        grid.add(new Label("Courses CSV: *"), 0, 2);
        grid.add(coursesField, 1, 2);
        grid.add(coursesBtn, 2, 2);

        grid.add(new Label("Classrooms CSV: *"), 0, 3);
        grid.add(classroomsField, 1, 3);
        grid.add(classroomsBtn, 2, 3);

        grid.add(new Label("Attendance CSV: *"), 0, 4);
        grid.add(attendanceField, 1, 4);
        grid.add(attendanceBtn, 2, 4);

        Label noteLabel = new Label("* Required files");
        noteLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
        grid.add(noteLabel, 0, 5, 3, 1);

        Button loadBtn = new Button("Load Files");
        loadBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10px 30px;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-padding: 10px 30px;");

        HBox buttonBox = new HBox(10, loadBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);
        grid.add(buttonBox, 0, 6, 3, 1);

        loadBtn.setOnAction(e -> {
            String studentsPath = studentsField.getText();
            String coursesPath = coursesField.getText();
            String classroomsPath = classroomsField.getText();
            String attendancePath = attendanceField.getText();

            if (studentsPath.isEmpty() || coursesPath.isEmpty() || classroomsPath.isEmpty()
                    || attendancePath.isEmpty()) {
                showWarning("Missing Files",
                        "Please select all required files (Students, Courses, Classrooms, Attendance).");
                return;
            }

            fileSelectionStage.close();
            loadFilesWithPaths(owner, studentsPath, coursesPath, classroomsPath,
                    attendancePath);
        });

        cancelBtn.setOnAction(e -> fileSelectionStage.close());

        Scene fileScene = new Scene(grid, 600, 350);
        ThemeManager.getInstance().registerScene(fileScene);
        fileSelectionStage.setScene(fileScene);
        fileSelectionStage.showAndWait();
    }

    private void loadFilesWithPaths(Stage owner, String studentsPath, String coursesPath,
                                    String classroomsPath, String attendancePath) {
        messages.add("📄 Loading individual files...");

        try {
            dataManager.clearAllData();

            List<Student> students = CSVParser.parseStudents(studentsPath);
            List<Course> courses = CSVParser.parseCourses(coursesPath);
            List<Classroom> classrooms = CSVParser.parseClassrooms(classroomsPath);

            dataManager.setStudents(students);
            dataManager.setCourses(courses);
            dataManager.setClassrooms(classrooms);

            CSVParser.parseAttendanceLists(attendancePath, students, courses);
            messages.add("✓ Attendance lists loaded");

            dataManager.setSourceFiles(
                    new File(studentsPath),
                    new File(coursesPath),
                    new File(classroomsPath),
                    new File(attendancePath));

            updateClassroomsView();

            messages.add("✓ Files loaded successfully:");
            messages.add("  • Students: " + students.size());
            messages.add("  • Courses: " + courses.size());
            messages.add("  • Classrooms: " + classrooms.size());

            showInfo("Load Success",
                    "Files loaded successfully!\n\n" +
                            "Students: " + students.size() + "\n" +
                            "Courses: " + courses.size() + "\n" +
                            "Classrooms: " + classrooms.size());

        } catch (Exception e) {
            messages.add("❌ Load failed: " + e.getMessage());
            showError("Load Failed", e.getMessage());
            dataManager.clearAllData();
        }
    }

    private void loadFromBackup(Stage owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Backup Folder");
        File backupDir = chooser.showDialog(owner);

        if (backupDir == null) {
            messages.add("⚠ Backup load cancelled");
            return;
        }

        messages.add("💼 Loading backup from: " + backupDir.getName() + "...");

        try {

            File scheduleFile = new File(backupDir, "schedule.csv");
            File studentsFile = new File(backupDir, "students.csv");
            File coursesFile = new File(backupDir, "courses.csv");
            File classroomsFile = new File(backupDir, "classrooms.csv");
            File attendanceFile = new File(backupDir, "attendance.csv");

            if (!studentsFile.exists() || !coursesFile.exists() || !classroomsFile.exists()) {
                showError("Invalid Backup",
                        "This doesn't appear to be a valid backup folder.\n" +
                                "Required files not found.");
                return;
            }

            dataManager.clearAllData();

            List<Student> students = CSVParser.parseStudents(studentsFile.getAbsolutePath());
            List<Course> courses = CSVParser.parseCourses(coursesFile.getAbsolutePath());
            List<Classroom> classrooms = CSVParser.parseClassrooms(classroomsFile.getAbsolutePath());

            dataManager.setStudents(students);
            dataManager.setCourses(courses);
            dataManager.setClassrooms(classrooms);

            if (attendanceFile.exists()) {
                CSVParser.parseAttendanceLists(attendanceFile.getAbsolutePath(), students, courses);
            }

            dataManager.setSourceFiles(studentsFile, coursesFile, classroomsFile,
                    attendanceFile.exists() ? attendanceFile : null);
            updateClassroomsView();

            boolean scheduleLoaded = false;
            if (scheduleFile.exists()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Schedule Found");
                confirm.setHeaderText("Backup contains a schedule");
                confirm.setContentText("Do you want to load the saved schedule?\n\n" +
                        "Yes: Load saved schedule\n" +
                        "No: Load only data (you can generate new schedule)");

                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        List<String> currentSlots = getTimeSlotsFromUI.get();
                        List<Exam> loadedExams = CSVParser.parseSchedule(scheduleFile, dataManager, currentSlots);

                        int days = daysSpinner.getValue();
                        Schedule newSchedule = new Schedule(days, currentSlots.size());
                        for (Exam exam : loadedExams) {
                            newSchedule.addExam(exam);
                        }
                        dataManager.setSchedule(newSchedule);
                        updateExamTableView(currentSlots);
                        scheduleLoaded = true;
                        messages.add("✓ Schedule loaded from backup");
                    } catch (Exception e) {
                        messages.add("⚠ Failed to load schedule: " + e.getMessage());
                    }
                }
            }

            messages.add("✓ Backup restored successfully:");
            messages.add("  • Students: " + students.size());
            messages.add("  • Courses: " + courses.size());
            messages.add("  • Classrooms: " + classrooms.size());
            if (scheduleLoaded) {
                messages.add("  • Schedule: Loaded");
            }

            showInfo("Backup Restored",
                    "Backup restored successfully!\n\n" +
                            "Students: " + students.size() + "\n" +
                            "Courses: " + courses.size() + "\n" +
                            "Classrooms: " + classrooms.size() + "\n" +
                            (scheduleLoaded ? "Schedule: Loaded" : "Schedule: Not loaded"));

        } catch (Exception e) {
            messages.add("❌ Backup restore failed: " + e.getMessage());
            showError("Backup Failed", "Failed to restore backup:\n" + e.getMessage());
            dataManager.clearAllData();
        }
    }

    private static class LoadResult {
        boolean success = false;
        String error = "";
        String log = "";
        int studentsCount = 0;
        int coursesCount = 0;
        int classroomsCount = 0;
        int attendanceCount = 0;
        List<String> warnings = new ArrayList<>();
    }

    private void handleImportSchedule(Stage owner) {

        if (!dataManager.isDataLoaded()) {
            showError("Data Required",
                    "Please load base data first using 'Load Data' button.\n\n" +
                            "Required data:\n" +
                            "• Students\n" +
                            "• Courses\n" +
                            "• Classrooms\n" +
                            "• Attendance");
            return;
        }

        Stage importDialog = new Stage();
        importDialog.initOwner(owner);
        importDialog.initModality(Modality.APPLICATION_MODAL);
        importDialog.setTitle("📥 Import Schedule");

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("📥 Import Schedule");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");

        Label subtitle = new Label("Choose how to import your schedule:");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        VBox option1 = createImportOption(
                "📄 Import from CSV File",
                "Standard CSV format import",
                "Format: ExamID,Course,Day,Slot,Room,Students",
                "#4CAF50");

        Button importFileBtn = new Button("Select CSV File");
        importFileBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8px 20px;");
        importFileBtn.setOnAction(e -> {
            importDialog.close();
            importFromCSV(owner);
        });
        option1.getChildren().add(importFileBtn);

        VBox option2 = createImportOption(
                "💼 Import from Backup Package",
                "Load schedule from backup folder",
                "Automatically finds schedule.csv in backup",
                "#FF9800");

        Button importBackupBtn = new Button("Select Backup Folder");
        importBackupBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 8px 20px;");
        importBackupBtn.setOnAction(e -> {
            importDialog.close();
            importFromBackupFolder(owner);
        });
        option2.getChildren().add(importBackupBtn);

        VBox option3 = createImportOption(
                "🔀 Merge with Current Schedule",
                "Add exams to existing schedule",
                "Combines imported exams with current schedule",
                "#9C27B0");

        Button mergeBtn = new Button("Select File to Merge");
        mergeBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-padding: 8px 20px;");
        mergeBtn.setOnAction(e -> {
            importDialog.close();
            mergeSchedule(owner);
        });
        option3.getChildren().add(mergeBtn);

        VBox statusBox = new VBox(5);
        statusBox.setPadding(new Insets(10));
        statusBox.setStyle(
                "-fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #F5F5F5;");

        Label statusTitle = new Label("📊 Current Data Status:");
        statusTitle.setStyle("-fx-font-weight: bold;");

        Label studentsLabel = new Label("✓ Students: " + dataManager.getStudents().size());
        Label coursesLabel = new Label("✓ Courses: " + dataManager.getCourses().size());
        Label classroomsLabel = new Label("✓ Classrooms: " + dataManager.getClassrooms().size());

        int coursesWithStudents = (int) dataManager.getCourses().stream()
                .filter(c -> !c.getEnrolledStudents().isEmpty())
                .count();
        Label enrollmentLabel = new Label(
                "✓ Courses with students: " + coursesWithStudents + " / " + dataManager.getCourses().size());

        if (coursesWithStudents == 0) {
            enrollmentLabel.setStyle("-fx-text-fill: #F44336;");
            Label warningLabel = new Label("⚠ Warning: No enrollment data! Import may fail.");
            warningLabel.setStyle("-fx-text-fill: #F44336; -fx-font-style: italic;");
            statusBox.getChildren().addAll(statusTitle, studentsLabel, coursesLabel, classroomsLabel, enrollmentLabel,
                    warningLabel);
        } else {
            statusBox.getChildren().addAll(statusTitle, studentsLabel, coursesLabel, classroomsLabel, enrollmentLabel);
        }

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-padding: 8px 20px;");
        cancelBtn.setOnAction(e -> importDialog.close());

        layout.getChildren().addAll(
                title,
                subtitle,
                new Separator(),
                statusBox,
                new Separator(),
                option1,
                new Separator(),
                option2,
                new Separator(),
                option3,
                new Separator(),
                cancelBtn);

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white;");

        Scene scene = new Scene(scrollPane, 600, 700);
        ThemeManager.getInstance().registerScene(scene);
        importDialog.setScene(scene);
        importDialog.showAndWait();
    }

    private VBox createImportOption(String title, String description, String details, String color) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-border-color: " + color
                + "; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

        Label detailsLabel = new Label(details);
        detailsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999; -fx-font-style: italic;");
        detailsLabel.setWrapText(true);

        box.getChildren().addAll(titleLabel, descLabel, detailsLabel);
        return box;
    }

    private void importFromCSV(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Schedule CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(owner);

        if (file == null) {
            messages.add("⚠ Import cancelled");
            return;
        }

        messages.add("📥 Importing schedule from: " + file.getName() + "...");

        Stage progressStage = new Stage();
        progressStage.initOwner(owner);
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("Importing Schedule...");

        VBox progressBox = new VBox(15);
        progressBox.setPadding(new Insets(30));
        progressBox.setAlignment(Pos.CENTER);

        Label statusLabel = new Label("Reading file...");
        statusLabel.setStyle("-fx-font-size: 14px;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefSize(500, 250);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 8px 20px;");
        closeBtn.setDisable(true);
        closeBtn.setOnAction(e -> progressStage.close());

        progressBox.getChildren().addAll(statusLabel, progressBar, logArea, closeBtn);
        Scene progressScene2 = new Scene(progressBox, 600, 450);
        ThemeManager.getInstance().registerScene(progressScene2);
        progressStage.setScene(progressScene2);
        progressStage.show();

        Task<ImportResult> importTask = new Task<ImportResult>() {
            @Override
            protected ImportResult call() throws Exception {
                ImportResult result = new ImportResult();
                StringBuilder log = new StringBuilder();

                updateMessage("Reading file...");
                updateProgress(0, 100);
                log.append("=== IMPORTING SCHEDULE ===\n");
                log.append("File: ").append(file.getName()).append("\n");
                log.append("Date: ").append(LocalDate.now()).append("\n\n");

                try {
                    List<String> currentSlots = getTimeSlotsFromUI.get();

                    updateMessage("Parsing CSV...");
                    updateProgress(20, 100);
                    log.append("Parsing CSV file...\n");

                    List<Exam> loadedExams = CSVParser.parseSchedule(file, dataManager, currentSlots);

                    log.append("✓ Parsed ").append(loadedExams.size()).append(" exam entries\n\n");
                    updateProgress(40, 100);

                    if (loadedExams.isEmpty()) {
                        result.success = false;
                        result.error = "No valid exams found in file";
                        log.append("❌ ERROR: No valid exams found\n");
                        result.log = log.toString();
                        return result;
                    }

                    updateMessage("Validating exams...");
                    updateProgress(60, 100);
                    log.append("=== VALIDATION ===\n");

                    int validExams = 0;
                    int invalidExams = 0;
                    List<String> validationErrors = new ArrayList<>();

                    for (Exam exam : loadedExams) {
                        String courseCode = exam.getCourse().getCourseCode();

                        if (exam.getEnrolledStudents().isEmpty()) {
                            validationErrors.add("Course " + courseCode + " has no enrolled students");
                            invalidExams++;
                            continue;
                        }

                        if (exam.getStudentCount() > exam.getClassroom().getCapacity()) {
                            validationErrors.add("Course " + courseCode + " exceeds room capacity");
                        }

                        validExams++;
                    }

                    log.append("Valid exams: ").append(validExams).append("\n");
                    log.append("Invalid exams: ").append(invalidExams).append("\n\n");

                    if (!validationErrors.isEmpty()) {
                        log.append("Validation warnings:\n");
                        for (String error : validationErrors) {
                            log.append("  ⚠ ").append(error).append("\n");
                        }
                        log.append("\n");
                    }

                    updateMessage("Creating schedule...");
                    updateProgress(80, 100);
                    log.append("Creating new schedule...\n");

                    int days = daysSpinner.getValue();
                    Schedule newSchedule = new Schedule(days, currentSlots.size());

                    for (Exam exam : loadedExams) {
                        newSchedule.addExam(exam);
                    }

                    dataManager.setSchedule(newSchedule);
                    log.append("✓ Schedule created\n\n");

                    updateMessage("Updating UI...");
                    updateProgress(90, 100);

                    result.success = true;
                    result.examsImported = loadedExams.size();
                    result.validExams = validExams;
                    result.invalidExams = invalidExams;
                    result.warnings = validationErrors;

                    updateProgress(100, 100);
                    log.append("=== IMPORT COMPLETE ===\n");
                    log.append("✅ Successfully imported ").append(validExams).append(" exams\n");

                    if (invalidExams > 0) {
                        log.append("⚠ ").append(invalidExams).append(" exams were skipped due to validation errors\n");
                    }

                    result.log = log.toString();

                } catch (IOException e) {
                    result.success = false;
                    result.error = "File I/O Error: " + e.getMessage();
                    log.append("\n❌ I/O Error:\n").append(e.getMessage()).append("\n");
                    result.log = log.toString();
                } catch (Exception e) {
                    result.success = false;
                    result.error = "Unexpected Error: " + e.getMessage();
                    log.append("\n❌ Unexpected Error:\n").append(e.getMessage()).append("\n");
                    e.printStackTrace();
                    result.log = log.toString();
                }

                return result;
            }
        };

        statusLabel.textProperty().bind(importTask.messageProperty());
        progressBar.progressProperty().bind(importTask.progressProperty());

        importTask.setOnSucceeded(e -> {
            ImportResult result = importTask.getValue();
            logArea.setText(result.log);
            closeBtn.setDisable(false);

            if (result.success) {
                updateExamTableView(getTimeSlotsFromUI.get());
                updateStatistics(result);

                messages.add("✅ Schedule imported successfully!");
                messages.add("  • Imported exams: " + result.examsImported);
                messages.add("  • Valid exams: " + result.validExams);
                if (result.invalidExams > 0) {
                    messages.add("  • Skipped exams: " + result.invalidExams);
                }

                Platform.runLater(() -> {
                    showImportSummary(owner, result);
                });
            } else {
                messages.add("❌ Import failed: " + result.error);
                showError("Import Failed", result.error + "\n\nCheck the log for details.");
            }
        });

        importTask.setOnFailed(e -> {
            closeBtn.setDisable(false);
            Throwable ex = importTask.getException();
            logArea.appendText("\n\n❌ FATAL ERROR:\n" + ex.getMessage());
            messages.add("❌ Import failed: " + ex.getMessage());
        });

        new Thread(importTask).start();
    }

    private void importFromBackupFolder(Stage owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Backup Folder");
        File backupDir = chooser.showDialog(owner);

        if (backupDir == null) {
            messages.add("⚠ Import cancelled");
            return;
        }

        File scheduleFile = new File(backupDir, "schedule.csv");

        if (!scheduleFile.exists()) {
            showError("Schedule Not Found",
                    "No schedule.csv file found in the selected backup folder.\n\n" +
                            "Expected location: " + scheduleFile.getAbsolutePath());
            return;
        }

        messages.add("📥 Importing schedule from backup: " + backupDir.getName() + "...");

        try {
            List<String> currentSlots = getTimeSlotsFromUI.get();
            List<Exam> loadedExams = CSVParser.parseSchedule(scheduleFile, dataManager, currentSlots);

            if (loadedExams.isEmpty()) {
                showWarning("No Exams", "No valid exams found in backup schedule.");
                return;
            }

            int days = daysSpinner.getValue();
            Schedule newSchedule = new Schedule(days, currentSlots.size());

            for (Exam exam : loadedExams) {
                newSchedule.addExam(exam);
            }

            dataManager.setSchedule(newSchedule);
            updateExamTableView(currentSlots);

            messages.add("✅ Schedule imported from backup successfully!");
            messages.add("  • Imported: " + loadedExams.size() + " exams");

            showInfo("Import Success",
                    "Schedule imported successfully from backup!\n\n" +
                            "Imported exams: " + loadedExams.size() + "\n" +
                            "Source: " + backupDir.getName());

        } catch (Exception e) {
            messages.add("❌ Import failed: " + e.getMessage());
            showError("Import Failed", "Failed to import schedule from backup:\n" + e.getMessage());
        }
    }

    private void mergeSchedule(Stage owner) {
        if (dataManager.getSchedule() == null || dataManager.getSchedule().getExams().isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("No Current Schedule");
            confirm.setHeaderText("You don't have a current schedule");
            confirm.setContentText("Do you want to import as a new schedule instead?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                importFromCSV(owner);
            }
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Schedule to Merge");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(owner);

        if (file == null) {
            messages.add("⚠ Merge cancelled");
            return;
        }

        messages.add("🔀 Merging schedule from: " + file.getName() + "...");

        try {
            List<String> currentSlots = getTimeSlotsFromUI.get();
            List<Exam> newExams = CSVParser.parseSchedule(file, dataManager, currentSlots);

            if (newExams.isEmpty()) {
                showWarning("No Exams", "No valid exams found in file to merge.");
                return;
            }

            int conflicts = 0;
            int merged = 0;
            List<String> conflictList = new ArrayList<>();

            for (Exam newExam : newExams) {
                boolean hasConflict = false;

                for (Exam existingExam : dataManager.getSchedule().getExams()) {
                    if (existingExam.getCourse().getCourseCode().equals(newExam.getCourse().getCourseCode())) {
                        conflictList.add("Course " + newExam.getCourse().getCourseCode() + " already scheduled");
                        hasConflict = true;
                        conflicts++;
                        break;
                    }
                }

                if (!hasConflict) {
                    dataManager.getSchedule().addExam(newExam);
                    merged++;
                }
            }

            updateExamTableView(currentSlots);

            StringBuilder msg = new StringBuilder();
            msg.append("Merge completed!\n\n");
            msg.append("New exams merged: ").append(merged).append("\n");
            msg.append("Conflicts (skipped): ").append(conflicts).append("\n\n");

            if (!conflictList.isEmpty() && conflictList.size() <= 10) {
                msg.append("Conflicts:\n");
                for (String conflict : conflictList) {
                    msg.append("• ").append(conflict).append("\n");
                }
            } else if (conflictList.size() > 10) {
                msg.append("Too many conflicts to display (").append(conflictList.size()).append(")\n");
            }

            messages.add("✅ Schedule merged successfully!");
            messages.add("  • Merged: " + merged + " exams");
            messages.add("  • Conflicts: " + conflicts + " exams");

            showInfo("Merge Complete", msg.toString());

        } catch (Exception e) {
            messages.add("❌ Merge failed: " + e.getMessage());
            showError("Merge Failed", "Failed to merge schedule:\n" + e.getMessage());
        }
    }

    private void showImportSummary(Stage owner, ImportResult result) {
        Alert summary = new Alert(Alert.AlertType.INFORMATION);
        summary.initOwner(owner);
        summary.setTitle("Import Summary");
        summary.setHeaderText("Schedule Import Completed");

        StringBuilder content = new StringBuilder();
        content.append("✅ Import successful!\n\n");
        content.append("Total exams imported: ").append(result.examsImported).append("\n");
        content.append("Valid exams: ").append(result.validExams).append("\n");

        if (result.invalidExams > 0) {
            content.append("Skipped exams: ").append(result.invalidExams).append("\n\n");
            content.append("⚠ Some exams were skipped due to:\n");
            content.append("  • Missing enrollment data\n");
            content.append("  • Invalid course codes\n");
            content.append("  • Missing classrooms\n");
        }

        if (!result.warnings.isEmpty() && result.warnings.size() <= 5) {
            content.append("\nWarnings:\n");
            for (int i = 0; i < Math.min(5, result.warnings.size()); i++) {
                content.append("  • ").append(result.warnings.get(i)).append("\n");
            }
            if (result.warnings.size() > 5) {
                content.append("  ... and ").append(result.warnings.size() - 5).append(" more\n");
            }
        }

        content.append("\nYou can now validate the schedule using the 'Validate' button.");

        summary.setContentText(content.toString());
        summary.showAndWait();
    }

    private void updateStatistics(ImportResult result) {
        if (statsArea != null) {
            int totalCourses = dataManager.getCourses().size();
            int placedExams = result.validExams;
            int unplacedExams = totalCourses - placedExams;

            String statsText = String.format(
                    "Total Courses: %d\nImported Exams: %d\nUnplaced Courses: %d\nStatus: Imported from file",
                    totalCourses,
                    placedExams,
                    unplacedExams);

            statsArea.setText(statsText);
        }
    }

    private static class ImportResult {
        boolean success = false;
        String error = "";
        String log = "";
        int examsImported = 0;
        int validExams = 0;
        int invalidExams = 0;
        List<String> warnings = new ArrayList<>();
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

        // 1. Veri Yüklü mü Kontrolü
        if (!dataManager.isDataLoaded()) {
            showError("Data not loaded", "Please load data from CSV files first.");
            messages.add("❌ ERROR: No data loaded. Click 'Load Data' first.");
            return;
        }

        // 2. Parametreleri Hazırla
        List<Course> coursesToSchedule = dataManager.getCourses();
        List<Classroom> availableClassrooms = new ArrayList<>(dataManager.getClassrooms());
        List<String> timeSlotsRaw = getTimeSlotsFromUI.get();
        Integer days = daysSpinner.getValue();

        if (coursesToSchedule.isEmpty() || availableClassrooms.isEmpty() || timeSlotsRaw.isEmpty()) {
            showError("Configuration Error", "Please check configuration (Days, Slots, Courses).");
            return;
        }

        messages.add("⚡ Starting schedule generation...");
        messages.add("📊 Parameters: " + days + " days, " + timeSlotsRaw.size() + " slots/day");

        // 3. Ham Sınav Listesini Oluştur
        List<Exam> initialExams = coursesToSchedule.stream()
                .filter(c -> c.getStudentCount() > 0)
                .map(Exam::new)
                .collect(Collectors.toList());

        // --- ADIM 1: BÜYÜK SINAVLARI DENGELİ PARÇALA (BALANCED SPLIT) ---

        int maxRoomCapacity = availableClassrooms.stream()
                .mapToInt(Classroom::getCapacity)
                .max().orElse(0);

        if (maxRoomCapacity == 0) {
            showError("Room Error", "No classrooms with valid capacity found.");
            return;
        }

        List<Exam> examsToPlace = new ArrayList<>();

        for (Exam exam : initialExams) {
            int totalStudents = exam.getEnrolledStudents().size();

            // Eğer öğrenci sayısı en büyük sınıftan fazlaysa BÖL
            if (totalStudents > maxRoomCapacity) {
                List<Student> allStudents = exam.getEnrolledStudents();
                int parts = (int) Math.ceil((double) totalStudents / maxRoomCapacity);
                int baseSize = totalStudents / parts;
                int remainder = totalStudents % parts;

                messages.add("ℹ Locking large exam: " + exam.getCourse().getCourseCode() +
                        " (" + totalStudents + " students) into " + parts + " rooms simultaneously.");

                int currentStartIndex = 0;
                for (int i = 0; i < parts; i++) {
                    int currentPartSize = baseSize + (i < remainder ? 1 : 0);
                    int end = currentStartIndex + currentPartSize;

                    List<Student> subList = allStudents.subList(currentStartIndex, end);
                    Exam examPart = new Exam(exam.getCourse());
                    examPart.setAssignedStudents(subList);
                    examsToPlace.add(examPart);

                    currentStartIndex = end;
                }
            } else {
                examsToPlace.add(exam);
            }
        }

        // Sıralama: En kalabalık grupları önce yerleştir
        examsToPlace.sort((a, b) -> Integer.compare(b.getStudentCount(), a.getStudentCount()));

        // -----------------------------------------------------------

        dataManager.setSchedule(new Schedule(days, timeSlotsRaw.size()));

        // Takip Map'leri
        Map<Student, Set<TimeSlot>> studentScheduledSlots = new HashMap<>();
        Map<TimeSlot, Set<String>> roomOccupancy = new HashMap<>();

        // DERS İÇİN KİLİTLENMİŞ ZAMAN (Aynı dersin parçaları aynı saate gelsin diye)
        Map<String, TimeSlot> courseLockedSlots = new HashMap<>();

        int placedCount = 0;
        unplacedCourses.clear();

        // --- ADIM 2: YERLEŞTİRME ALGORİTMASI ---
        for (Exam exam : examsToPlace) {
            boolean placed = false;
            List<Student> studentsOfCourse = exam.getEnrolledStudents();
            int enrolledCount = exam.getStudentCount();
            String courseCode = exam.getCourse().getCourseCode();

            // Eğer bu dersin bir parçası daha önce yerleştiyse, ZORUNLU olarak o saati al
            TimeSlot forcedSlot = courseLockedSlots.get(courseCode);

            // Sınıfları karıştır
            Collections.shuffle(availableClassrooms, new Random());

            // DÖNGÜ AYARLARI
            int startDay = (forcedSlot != null) ? forcedSlot.getDay() : 1;
            int endDay = (forcedSlot != null) ? forcedSlot.getDay() : days;

            outerLoop: for (int day = startDay; day <= endDay; day++) {

                // DÜZELTME: Lambda içinde kullanmak için 'final' kopya oluşturuyoruz
                final int currentDay = day;

                int startSlot = (forcedSlot != null) ? forcedSlot.getSlotNumber() : 1;
                int endSlot = (forcedSlot != null) ? forcedSlot.getSlotNumber() : timeSlotsRaw.size();

                for (int slotNum = startSlot; slotNum <= endSlot; slotNum++) {
                    TimeSlot currentSlot = new TimeSlot(day, slotNum);

                    // 1. ÖĞRENCİ ÇAKIŞMASI KONTROLÜ
                    boolean studentConflict = false;
                    for (Student student : studentsOfCourse) {
                        Set<TimeSlot> busySlots = studentScheduledSlots.getOrDefault(student, Collections.emptySet());

                        if (busySlots.contains(currentSlot)) {
                            studentConflict = true;
                            break;
                        }

                        // Günlük limit kontrolü (BURASI DÜZELTİLDİ: 'day' yerine 'currentDay'
                        // kullanıldı)
                        long examsOnDay = busySlots.stream().filter(ts -> ts.getDay() == currentDay).count();
                        if (examsOnDay >= 2) {
                            studentConflict = true;
                            break;
                        }

                        // Ardışık sınav kontrolü
                        if (slotNum > 1) {
                            TimeSlot previousSlot = new TimeSlot(day, slotNum - 1);
                            if (busySlots.contains(previousSlot)) {
                                studentConflict = true;
                                break;
                            }
                        }
                    }
                    if (studentConflict)
                        continue;

                    // 3. ODA SEÇİMİ
                    for (Classroom room : availableClassrooms) {
                        if (!room.canAccommodate(enrolledCount))
                            continue;

                        roomOccupancy.putIfAbsent(currentSlot, new HashSet<>());
                        if (roomOccupancy.get(currentSlot).contains(room.getClassroomID()))
                            continue;

                        // --- YERLEŞTİR ---
                        exam.setTimeSlot(currentSlot);
                        exam.setClassroom(room);
                        dataManager.getSchedule().addExam(exam);

                        // Kayıtları güncelle
                        roomOccupancy.get(currentSlot).add(room.getClassroomID());

                        for (Student student : studentsOfCourse) {
                            studentScheduledSlots.computeIfAbsent(student, k -> new HashSet<>()).add(currentSlot);
                        }

                        // BU DERSİN SAATİNİ KİLİTLE
                        if (!courseLockedSlots.containsKey(courseCode)) {
                            courseLockedSlots.put(courseCode, currentSlot);
                        }

                        placed = true;
                        placedCount++;

                        String suffix = (forcedSlot != null || examsToPlace.stream()
                                .filter(e -> e.getCourse().getCourseCode().equals(courseCode)).count() > 1)
                                ? " [Part]"
                                : "";

                        messages.add("  ✓ " + courseCode + suffix +
                                " → Day " + day + ", Slot " + slotNum +
                                ", Room " + room.getClassroomID() +
                                " (" + enrolledCount + " students)");

                        break outerLoop;
                    }
                }
            }

            if (!placed) {
                if (!unplacedCourses.contains(courseCode)) {
                    unplacedCourses.add(courseCode);
                }
                messages.add("❌ FAILED: " + courseCode + " could not be placed.");
            }
        }

        // 4. Sonuçlar
        updateExamTableView(timeSlotsRaw);

        int total = examsToPlace.size();
        int unplacedCount = unplacedCourses.size();

        String statsText = String.format(
                "Total Exam Sessions: %d\nPlaced: %d\nUnplaced Courses: %d",
                total, placedCount, unplacedCount);

        if (statsArea != null)
            statsArea.setText(statsText);

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
                            exam,
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
        Scene dialogScene = new Scene(root, 300, 400);
        ThemeManager.getInstance().registerScene(dialogScene);
        dialog.setScene(dialogScene);
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

        // --- DÜZELTME 1: LİSTE DOLDURMA ---
        // Sadece '[' ile başlamayan, geçerli dersleri listeye ekle
        dataManager.getCourses().stream()
                .filter(c -> {
                    String code = c.getCourseCode();
                    // Null değilse, boş değilse VE köşeli parantez ile başlamıyorsa ekle
                    return code != null && !code.trim().isEmpty() && !code.trim().startsWith("[");
                })
                .forEach(c -> listView.getItems().add(c.getCourseCode()));
        // ----------------------------------

        TextField codeField = new TextField();
        codeField.setPromptText("Course Code");
        TextField nameField = new TextField();
        nameField.setPromptText("Course Name");

        Button addBtn = new Button("Add");
        Button remBtn = new Button("Remove");

        addBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (!code.isEmpty()) {
                // Yeni eklenen derslerde sorun yok, doğrudan ekle
                Course c = new Course(code, nameField.getText(), 1);
                dataManager.addCourse(c);
                listView.getItems().add(code);
                messages.add("Course added: " + code);
                codeField.clear();
                nameField.clear();
            }
        });

        // --- DÜZELTME 2: SİLME İŞLEMİ (GÜVENLİ) ---
        remBtn.setOnAction(e -> {
            // İndeks yerine seçilen yazıyı (String) alıyoruz
            String selectedCode = listView.getSelectionModel().getSelectedItem();

            if (selectedCode != null) {
                // DataManager içinden bu koda sahip olan dersi buluyoruz
                Course toRemove = dataManager.getCourses().stream()
                        .filter(c -> c.getCourseCode().equals(selectedCode))
                        .findFirst()
                        .orElse(null);

                if (toRemove != null) {
                    dataManager.removeCourse(toRemove); // Hafızadan sil
                    listView.getItems().remove(selectedCode); // Listeden sil
                    messages.add("Course removed: " + selectedCode);
                }
            }
        });
        // ------------------------------------------

        VBox inputs = new VBox(5, codeField, nameField);
        VBox root = new VBox(10, new Label("Courses"), listView, inputs, new HBox(5, addBtn, remBtn));
        root.setPadding(new Insets(10));
        Scene dialogScene = new Scene(root, 300, 450);
        ThemeManager.getInstance().registerScene(dialogScene);
        dialog.setScene(dialogScene);
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
                Classroom roomToRemove = dataManager.getClassrooms().get(idx);
                dataManager.removeClassroom(roomToRemove);
                classroomList.getItems().remove(idx);
                updateClassroomsView();
                messages.add("Classroom removed: " + roomToRemove.getClassroomID());
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

        Scene dialogScene = new Scene(grid, 400, 400);
        ThemeManager.getInstance().registerScene(dialogScene);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    private void showManageAttendance(Stage owner) {
        if (!dataManager.isDataLoaded()) {
            showError("No Data", "Please load data first.");
            return;
        }

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Manage Attendance");

        ComboBox<Course> courseCombo = new ComboBox<>();
        List<Course> validCourses = dataManager.getCourses().stream()
                .filter(c -> c.getCourseCode() != null && !c.getCourseCode().trim().startsWith("["))
                .collect(Collectors.toList());
        courseCombo.setItems(FXCollections.observableArrayList(validCourses));
        courseCombo.setPrefWidth(300);

        // Converter: Dersteki güncel öğrenci sayısını gösterir
        courseCombo.setConverter(new javafx.util.StringConverter<Course>() {
            @Override
            public String toString(Course c) {
                if (c == null)
                    return "";
                return c.getCourseCode() + " (" + c.getEnrolledStudents().size() + " students)";
            }

            @Override
            public Course fromString(String string) {
                return null;
            }
        });

        ListView<Student> availableList = new ListView<>();
        ListView<Student> enrolledList = new ListView<>();

        Label lblAvail = new Label("Available Students");
        Label lblEnroll = new Label("Enrolled Students");
        lblAvail.setStyle("-fx-font-weight: bold;");
        lblEnroll.setStyle("-fx-font-weight: bold;");

        // Cell Factory: Sadece ID göster
        availableList.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getStudentID());
            }
        });
        enrolledList.setCellFactory(availableList.getCellFactory());

        Button btnAdd = new Button("Add ->");
        Button btnRemove = new Button("<- Remove");
        btnAdd.setDisable(true);
        btnRemove.setDisable(true);

        VBox btnBox = new VBox(10, btnAdd, btnRemove);
        btnBox.setAlignment(Pos.CENTER);

        // --- MANTIK KISMI ---

        // Ders Seçilince Listeleri Doldur
        courseCombo.setOnAction(e -> {
            Course selectedCourse = courseCombo.getValue();
            if (selectedCourse != null) {
                List<Student> enrolled = selectedCourse.getEnrolledStudents();
                List<Student> available = new ArrayList<>(dataManager.getStudents());
                available.removeAll(enrolled); // Kayıtlıları çıkar

                enrolledList.setItems(FXCollections.observableArrayList(enrolled));
                availableList.setItems(FXCollections.observableArrayList(available));

                lblEnroll.setText("Enrolled Students (" + enrolled.size() + ")");
                lblAvail.setText("Available Students (" + available.size() + ")");

                btnAdd.setDisable(false);
                btnRemove.setDisable(false);
            }
        });

        // EKLEME İŞLEMİ
        btnAdd.setOnAction(e -> {
            Student s = availableList.getSelectionModel().getSelectedItem();
            Course c = courseCombo.getValue();

            if (s == null) {
                new Alert(Alert.AlertType.WARNING, "Please select a student to add.").show();
                return;
            }

            if (c != null) {
                try {
                    // Backend'e ekle
                    dataManager.enrollStudentToCourse(c, s);

                    // LİSTELERİ SIFIRDAN YÜKLE (En Garanti Yol)
                    List<Student> freshEnrolled = c.getEnrolledStudents();
                    List<Student> freshAvailable = new ArrayList<>(dataManager.getStudents());
                    freshAvailable.removeAll(freshEnrolled); // Farkını al

                    // UI Listelerini Güncelle
                    enrolledList.setItems(FXCollections.observableArrayList(freshEnrolled));
                    availableList.setItems(FXCollections.observableArrayList(freshAvailable));

                    // Başlıkları Güncelle
                    lblEnroll.setText("Enrolled Students (" + freshEnrolled.size() + ")");
                    lblAvail.setText("Available Students (" + freshAvailable.size() + ")");

                    // ComboBox yazısını güncelle (Sayı artsın diye)
                    int currentIndex = courseCombo.getSelectionModel().getSelectedIndex();
                    ObservableList<Course> items = courseCombo.getItems();
                    courseCombo.setItems(null);
                    courseCombo.setItems(items);
                    courseCombo.getSelectionModel().select(currentIndex);

                    messages.add("Enrolled " + s.getStudentID() + " to " + c.getCourseCode());

                } catch (Exception ex) {
                    showError("Add Failed", ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        // SİLME İŞLEMİ
        btnRemove.setOnAction(e -> {
            Student s = enrolledList.getSelectionModel().getSelectedItem();
            Course c = courseCombo.getValue();

            if (s == null) {
                new Alert(Alert.AlertType.WARNING, "Please select a student to remove.").show();
                return;
            }

            if (c != null) {
                try {
                    dataManager.unenrollStudentFromCourse(c, s);

                    List<Student> freshEnrolled = c.getEnrolledStudents();
                    List<Student> freshAvailable = new ArrayList<>(dataManager.getStudents());
                    freshAvailable.removeAll(freshEnrolled);

                    // UI Listelerini Güncelle
                    enrolledList.setItems(FXCollections.observableArrayList(freshEnrolled));
                    availableList.setItems(FXCollections.observableArrayList(freshAvailable));

                    // Başlıkları Güncelle
                    lblEnroll.setText("Enrolled Students (" + freshEnrolled.size() + ")");
                    lblAvail.setText("Available Students (" + freshAvailable.size() + ")");

                    // ComboBox yazısını güncelle
                    int currentIndex = courseCombo.getSelectionModel().getSelectedIndex();
                    ObservableList<Course> items = courseCombo.getItems();
                    courseCombo.setItems(null);
                    courseCombo.setItems(items);
                    courseCombo.getSelectionModel().select(currentIndex);

                    messages.add("Removed " + s.getStudentID() + " from " + c.getCourseCode());

                } catch (Exception ex) {
                    showError("Remove Failed", ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(15));
        grid.setHgap(15);
        grid.setVgap(10);
        grid.add(new Label("Select Course:"), 0, 0);
        grid.add(courseCombo, 1, 0, 2, 1);
        grid.add(lblAvail, 0, 1);
        grid.add(new Label(""), 1, 1);
        grid.add(lblEnroll, 2, 1);
        grid.add(availableList, 0, 2);
        grid.add(btnBox, 1, 2);
        grid.add(enrolledList, 2, 2);

        Scene dialogScene = new Scene(grid, 700, 500);
        ThemeManager.getInstance().registerScene(dialogScene);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    private void editExam(Stage owner, ExamEntry e) {
        Stage d = new Stage();
        d.initOwner(owner);
        d.initModality(Modality.APPLICATION_MODAL);
        d.setTitle("Edit Exam: " + e.getId());

        TextField course = new TextField(e.getCourseId());
        course.setEditable(false); // Course code should not be changed directly on exam instance

        Spinner<Integer> day = new Spinner<>(1, 30, e.getDay());
        day.setEditable(true);

        // Use ComboBox for Time Slot to ensure validity
        ComboBox<String> slot = new ComboBox<>(FXCollections.observableArrayList(getTimeSlotsFromUI.get()));
        slot.setValue(e.getTimeSlot());

        TextField room = new TextField(e.getRoomId());

        // Enrolled count as label (editable via Manage Students button)
        Label enrolledLabel = new Label(String.valueOf(e.getEnrolled()));
        enrolledLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Button save = new Button("💾 Save");
        addStyledTooltip(save, "Save changes to this exam");
        save.setOnAction(ev -> {
            int newDay = day.getValue();
            int newSlotIdx = slot.getSelectionModel().getSelectedIndex() + 1;
            String newRoomId = room.getText();
            // Enrollment is now managed via Manage Students button, get current count from
            // course
            int newEnrollment = e.getExam() != null ? e.getExam().getCourse().getStudentCount() : e.getEnrolled();

            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // --- VALIDATION LOGIC ---
            if (e.getExam() != null && dataManager.getSchedule() != null) {
                // 1. Check Room Capacity
                Classroom targetRoom = dataManager.getClassroomByID(newRoomId);
                if (targetRoom == null) {
                    errors.add("Invalid Room ID: " + newRoomId);
                } else if (newEnrollment > targetRoom.getCapacity()) {
                    errors.add(
                            "Classroom Capacity Exceeded: " + newEnrollment + " students > " + targetRoom.getCapacity()
                                    + " capacity. Change room or reduce enrollment.");
                }

                // 2. Check Room Double Booking
                boolean roomOccupied = dataManager.getSchedule().getExams().stream()
                        .filter(ex -> ex != e.getExam()) // Skip current exam
                        .filter(Exam::isScheduled)
                        .anyMatch(ex -> ex.getTimeSlot().getDay() == newDay &&
                                ex.getTimeSlot().getSlotNumber() == newSlotIdx &&
                                ex.getClassroom().getClassroomID().equals(newRoomId));

                if (roomOccupied) {
                    errors.add("Room Conflict: " + newRoomId + " is already booked at Day " + newDay + " Slot "
                            + newSlotIdx);
                }

                // 3. Check Student Conflicts
                List<Student> students = e.getExam().getEnrolledStudents();

                List<String> overloadedStudents = new ArrayList<>();
                List<String> conflictingStudents = new ArrayList<>();

                for (Student s : students) {
                    long examsAtSameTime = dataManager.getSchedule().getExams().stream()
                            .filter(ex -> ex != e.getExam())
                            .filter(Exam::isScheduled)
                            .filter(ex -> ex.getEnrolledStudents().contains(s))
                            .filter(ex -> ex.getTimeSlot().getDay() == newDay &&
                                    ex.getTimeSlot().getSlotNumber() == newSlotIdx)
                            .count();

                    if (examsAtSameTime > 0) {

                        conflictingStudents.add(s.getStudentID());
                    }

                    long examsOnSameDay = dataManager.getSchedule().getExams().stream()
                            .filter(ex -> ex != e.getExam())
                            .filter(Exam::isScheduled)
                            .filter(ex -> ex.getEnrolledStudents().contains(s))
                            .filter(ex -> ex.getTimeSlot().getDay() == newDay)
                            .count();

                    // If they already have 2 or more exams on this day, adding one more makes it >
                    // 2
                    if (examsOnSameDay >= 2) {
                        overloadedStudents.add(s.getStudentID());
                    }
                }

                if (!conflictingStudents.isEmpty()) {
                    String affectedIds = String.join(", ", conflictingStudents);
                    if (conflictingStudents.size() > 5) {
                        affectedIds = conflictingStudents.subList(0, 5).stream().collect(Collectors.joining(", "))
                                + ", ... (+" + (conflictingStudents.size() - 5) + " more)";
                    }
                    errors.add("Student Time Conflict: " + affectedIds
                            + " are already taking an exam at this time.");
                }

                if (!overloadedStudents.isEmpty()) {
                    String affectedIds = String.join(", ", overloadedStudents);
                    // Limit output length if too many students
                    if (overloadedStudents.size() > 5) {
                        affectedIds = overloadedStudents.subList(0, 5).stream().collect(Collectors.joining(", "))
                                + ", ... (+" + (overloadedStudents.size() - 5) + " more)";
                    }
                    errors.add("Student Overload: " + affectedIds
                            + " will have more than 2 exams on Day " + newDay);
                }
            }

            // --- SHOW ALERTS ---
            if (!errors.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Validation Error");
                alert.setHeaderText("Cannot Save Changes");
                alert.setContentText(String.join("\n", errors));
                ThemeManager.getInstance().styleAlert(alert);
                alert.showAndWait();
                return; // Stop save
            }

            if (!warnings.isEmpty()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Validation Warnings");
                confirm.setHeaderText("Warning: Potential Issues");
                confirm.setContentText(String.join("\n", warnings) + "\n\nDo you want to proceed?");
                ThemeManager.getInstance().styleAlert(confirm);
                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isEmpty() || res.get() != ButtonType.OK) {
                    return; // Stop save
                }
            }

            // --- PROCEED WITH SAVE ---

            // Update ExamEntry (View)
            e.setDay(day.getValue());
            e.setTimeSlot(slot.getValue());
            e.setRoomId(room.getText());
            // Enrollment is now managed via Manage Students, use current course student
            // count
            if (e.getExam() != null) {
                e.setEnrolled(e.getExam().getCourse().getStudentCount());
            }

            // Update Real Exam Model (Logic)
            if (e.getExam() != null) {
                if (newSlotIdx > 0) {
                    TimeSlot newTimeSlot = new TimeSlot(newDay, newSlotIdx);
                    e.getExam().setTimeSlot(newTimeSlot);
                }

                Classroom newRoom = dataManager.getClassroomByID(room.getText());
                if (newRoom != null) {
                    e.getExam().setClassroom(newRoom);
                }

                // Student count is now managed via Manage Students, no need to set manually
            }

            if (dataManager.getSchedule() != null) {
                dataManager.getSchedule().rebuildTimeSlotMap();
            }

            table.refresh();
            messages.add("✓ Exam " + e.getId() + " updated");
            d.close();
        });

        // Manage Students button
        Button manageStudentsBtn = new Button("👥 Manage Students");
        manageStudentsBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        addStyledTooltip(manageStudentsBtn, "Manage students enrolled in this exam");
        manageStudentsBtn.setOnAction(ev -> {
            showManageExamStudents(d, e, enrolledLabel);
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
        HBox enrollBox = new HBox(10, enrolledLabel, manageStudentsBtn);
        enrollBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(enrollBox, 1, 4);
        grid.add(save, 1, 5);

        Scene dialogScene = new Scene(grid, 450, 350);
        ThemeManager.getInstance().registerScene(dialogScene);
        d.setScene(dialogScene);
        d.showAndWait();
    }

    /**
     * Shows a dialog to manage students enrolled in a specific exam.
     * Allows adding students (not in course) and removing students (already
     * enrolled).
     */
    private void showManageExamStudents(Stage owner, ExamEntry examEntry, Label enrolledLabel) {
        if (examEntry.getExam() == null || examEntry.getExam().getCourse() == null) {
            showError("Error", "Cannot manage students for this exam.");
            return;
        }

        Course course = examEntry.getExam().getCourse();
        Exam exam = examEntry.getExam();

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Manage Students - " + course.getCourseCode());

        // Get current classroom capacity for validation
        Classroom currentRoom = exam.getClassroom();
        int roomCapacity = currentRoom != null ? currentRoom.getCapacity() : Integer.MAX_VALUE;

        // Left list: Enrolled students
        ListView<String> enrolledList = new ListView<>();
        enrolledList.setPrefSize(200, 300);
        enrolledList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Right list: Available students (not enrolled in this course)
        ListView<String> availableList = new ListView<>();
        availableList.setPrefSize(200, 300);
        availableList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Populate lists
        Runnable refreshLists = () -> {
            enrolledList.getItems().clear();
            availableList.getItems().clear();

            List<Student> enrolledStudents = course.getEnrolledStudents();
            Set<String> enrolledIds = enrolledStudents.stream()
                    .map(Student::getStudentID)
                    .collect(Collectors.toSet());

            enrolledStudents.forEach(s -> enrolledList.getItems().add(s.getStudentID()));

            dataManager.getStudents().stream()
                    .filter(s -> !enrolledIds.contains(s.getStudentID()))
                    .forEach(s -> availableList.getItems().add(s.getStudentID()));
        };
        refreshLists.run();

        // Search fields
        TextField enrolledSearch = new TextField();
        enrolledSearch.setPromptText("🔍 Search enrolled...");
        enrolledSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            enrolledList.getItems().clear();
            String filter = newVal.toLowerCase();
            course.getEnrolledStudents().stream()
                    .filter(s -> s.getStudentID().toLowerCase().contains(filter))
                    .forEach(s -> enrolledList.getItems().add(s.getStudentID()));
        });

        TextField availableSearch = new TextField();
        availableSearch.setPromptText("🔍 Search available...");
        availableSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            availableList.getItems().clear();
            String filter = newVal.toLowerCase();
            Set<String> enrolledIds = course.getEnrolledStudents().stream()
                    .map(Student::getStudentID)
                    .collect(Collectors.toSet());
            dataManager.getStudents().stream()
                    .filter(s -> !enrolledIds.contains(s.getStudentID()))
                    .filter(s -> s.getStudentID().toLowerCase().contains(filter))
                    .forEach(s -> availableList.getItems().add(s.getStudentID()));
        });

        // Capacity label
        Label capacityLabel = new Label();
        Runnable updateCapacityLabel = () -> {
            int enrolled = course.getStudentCount();
            String text = "Enrolled: " + enrolled + " / " + roomCapacity;
            capacityLabel.setText(text);
            if (enrolled > roomCapacity) {
                capacityLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            } else if (enrolled >= roomCapacity * 0.9) {
                capacityLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            } else {
                capacityLabel.setStyle("-fx-text-fill: green;");
            }
        };
        updateCapacityLabel.run();

        // Add button (Available → Enrolled)
        Button addBtn = new Button("Add →");
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addBtn.setOnAction(ev -> {
            List<String> selectedIds = new ArrayList<>(availableList.getSelectionModel().getSelectedItems());
            if (selectedIds.isEmpty()) {
                showWarning("No Selection", "Please select student(s) to add.");
                return;
            }

            int addedCount = 0;
            List<String> skippedStudents = new ArrayList<>();

            for (String selectedId : selectedIds) {
                Student student = dataManager.getStudentByID(selectedId);
                if (student == null)
                    continue;

                // Check capacity
                if (course.getStudentCount() >= roomCapacity) {
                    skippedStudents.add(selectedId + " (capacity exceeded)");
                    continue;
                }

                // Check for conflicts at exam time
                boolean hasConflict = false;
                if (exam.isScheduled()) {
                    TimeSlot examSlot = exam.getTimeSlot();
                    int examDay = examSlot.getDay();
                    int examSlotNum = examSlot.getSlotNumber();

                    // Check time conflict
                    boolean hasTimeConflict = dataManager.getSchedule().getExams().stream()
                            .filter(ex -> ex != exam)
                            .filter(Exam::isScheduled)
                            .filter(ex -> ex.getTimeSlot().getDay() == examDay &&
                                    ex.getTimeSlot().getSlotNumber() == examSlotNum)
                            .anyMatch(ex -> ex.getEnrolledStudents().contains(student));

                    if (hasTimeConflict) {
                        skippedStudents.add(selectedId + " (time conflict)");
                        hasConflict = true;
                    }
                }

                if (!hasConflict) {
                    // Enroll student (memory only)
                    course.addStudent(student);
                    student.addCourse(course);
                    addedCount++;
                }
            }

            // Refresh UI
            refreshLists.run();
            enrolledSearch.clear();
            availableSearch.clear();
            updateCapacityLabel.run();
            enrolledLabel.setText(String.valueOf(course.getStudentCount()));

            // Show result message
            if (addedCount > 0) {
                messages.add("✓ Added " + addedCount + " student(s) to " + course.getCourseCode());
            }
            if (!skippedStudents.isEmpty()) {
                showWarning("Some Students Skipped",
                        "The following students could not be added:\n" +
                                String.join("\n", skippedStudents));
            }
        });

        // Remove button (Enrolled → Available)
        Button removeBtn = new Button("← Remove");
        removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        removeBtn.setOnAction(ev -> {
            List<String> selectedIds = new ArrayList<>(enrolledList.getSelectionModel().getSelectedItems());
            if (selectedIds.isEmpty()) {
                showWarning("No Selection", "Please select student(s) to remove.");
                return;
            }

            // Confirm removal
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Removal");
            confirm.setHeaderText("Remove " + selectedIds.size() + " student(s) from course?");
            confirm.setContentText("Remove selected students from " + course.getCourseCode() + "?\n\n" +
                    "This will also remove them from this exam.");
            ThemeManager.getInstance().styleAlert(confirm);
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) {
                return;
            }

            int removedCount = 0;
            for (String selectedId : selectedIds) {
                Student student = dataManager.getStudentByID(selectedId);
                if (student == null)
                    continue;

                // Unenroll student (memory only)
                course.removeStudent(student);
                if (student.getCourses() != null) {
                    student.getCourses().removeIf(c -> c.getCourseCode().equals(course.getCourseCode()));
                }
                removedCount++;
            }

            // Refresh UI
            refreshLists.run();
            enrolledSearch.clear();
            availableSearch.clear();
            updateCapacityLabel.run();
            enrolledLabel.setText(String.valueOf(course.getStudentCount()));

            messages.add("✓ Removed " + removedCount + " student(s) from " + course.getCourseCode());
        });

        // Close button
        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(ev -> dialog.close());

        // Layout
        VBox enrolledBox = new VBox(5,
                new Label("📚 Enrolled Students"),
                enrolledSearch,
                enrolledList);

        VBox availableBox = new VBox(5,
                new Label("👤 Available Students"),
                availableSearch,
                availableList);

        VBox buttonBox = new VBox(10, addBtn, removeBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(50, 10, 50, 10));

        HBox listsBox = new HBox(10, availableBox, buttonBox, enrolledBox);
        listsBox.setAlignment(Pos.CENTER);

        HBox bottomBox = new HBox(20, capacityLabel, closeBtn);
        bottomBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(15,
                new Label("Manage students for: " + course.getCourseCode() + " - " + course.getCourseName()),
                listsBox,
                bottomBox);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(root, 550, 450);
        ThemeManager.getInstance().registerScene(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void deleteExam(ExamEntry e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Exam");
        confirm.setContentText("Delete " + e.getId() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Remove from DataModel (Logic)
            if (e.getExam() != null && dataManager.getSchedule() != null) {
                dataManager.getSchedule().removeExam(e.getExam());
            }

            // Remove from View
            exams.remove(e);
            table.refresh();
            messages.add("🗑 Deleted: " + e.getId());
        }
    }

    private boolean handleSave(Stage owner) {
        if (dataManager.getSchedule() == null || exams.isEmpty()) {
            showWarning("No Schedule", "Nothing to save. Please generate a schedule first.");
            return false;
        }

        // Track if save was completed
        final boolean[] saveCompleted = { false };

        Stage saveDialog = new Stage();
        saveDialog.initOwner(owner);
        saveDialog.initModality(Modality.APPLICATION_MODAL);
        saveDialog.setTitle("💾 Save Schedule");

        Label formatLabel = new Label("Select save format:");
        formatLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        ComboBox<String> formatCombo = new ComboBox<>(FXCollections.observableArrayList(
                "📊 Standard CSV (Re-importable)",
                "📋 Detailed CSV (All Information)",
                "💼 Backup Package (All Files)"));
        formatCombo.setValue("📊 Standard CSV (Re-importable)");
        formatCombo.setPrefWidth(350);

        TextArea descArea = new TextArea();
        descArea.setEditable(false);
        descArea.setPrefHeight(100);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-font-size: 12px; -fx-background-color: #f5f5f5;");

        formatCombo.setOnAction(e -> {
            String selected = formatCombo.getValue();
            String desc = getSaveFormatDescription(selected);
            descArea.setText(desc);
        });
        descArea.setText(getSaveFormatDescription(formatCombo.getValue()));

        Label fileNameLabel = new Label("File name:");
        TextField fileNameField = new TextField("exam_schedule_" + LocalDate.now());
        fileNameField.setPrefWidth(350);

        Button saveButton = new Button("💾 Save");
        saveButton.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10px 30px; -fx-font-size: 14px;");

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-padding: 10px 30px; -fx-font-size: 14px;");
        cancelButton.setOnAction(e -> saveDialog.close());

        saveButton.setOnAction(e -> {
            String format = formatCombo.getValue();
            String fileName = fileNameField.getText().trim();
            saveCompleted[0] = true;
            saveDialog.close();

            try {
                if (format.contains("Standard CSV")) {
                    saveStandardCSV(owner, fileName);
                } else if (format.contains("Detailed CSV")) {
                    saveDetailedCSV(owner, fileName);
                } else if (format.contains("Backup Package")) {
                    saveBackupPackage(owner, fileName);
                }
            } catch (Exception ex) {
                showError("Save Failed", "Error during save: " + ex.getMessage());
                messages.add("❌ Save failed: " + ex.getMessage());
            }
        });

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(25));

        Label title = new Label("💾 Save Schedule");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        VBox formatBox = new VBox(8, formatLabel, formatCombo);

        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-weight: bold;");
        VBox descBox = new VBox(5, descLabel, descArea);

        VBox fileNameBox = new VBox(8, fileNameLabel, fileNameField);

        HBox buttonBox = new HBox(10, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(title, new Separator(), formatBox, descBox, fileNameBox, buttonBox);

        Scene scene = new Scene(layout, 500, 450);
        ThemeManager.getInstance().registerScene(scene);
        saveDialog.setScene(scene);
        saveDialog.showAndWait();

        return saveCompleted[0];
    }

    private String getSaveFormatDescription(String format) {
        if (format.contains("Standard CSV")) {
            return "✓ Basic schedule data (ExamID, Course, Day, Slot, Room, Students)\n" +
                    "✓ Can be re-imported using 'Import Schedule' button\n" +
                    "✓ Lightweight and fast\n" +
                    "✓ Compatible with older versions";
        } else if (format.contains("Detailed CSV")) {
            return "✓ Complete information including course names, dates\n" +
                    "✓ Better for sharing and documentation\n" +
                    "✓ Excel/Google Sheets friendly\n" +
                    "✓ Includes capacity utilization\n" +
                    "⚠ Cannot be re-imported (use for export only)";
        } else if (format.contains("Backup Package")) {
            return "✓ Complete backup of all data and schedule\n" +
                    "✓ Includes students, courses, classrooms, attendance\n" +
                    "✓ Creates a folder with all CSV files\n" +
                    "✓ Perfect for archiving or transferring\n" +
                    "✓ Can be loaded as a complete project";
        }
        return "";
    }

    private void saveStandardCSV(Stage owner, String baseName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Schedule - Standard CSV");
        chooser.setInitialFileName(baseName + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {
                pw.println("ExamID,Course,Day,Slot,Room,Students");

                List<String> timeSlots = getTimeSlotsFromUI.get();
                int count = 0;

                for (Exam exam : dataManager.getSchedule().getExams()) {
                    if (exam.isScheduled()) {
                        String timeSlot = "";
                        try {
                            timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
                        } catch (Exception e) {
                            timeSlot = "Slot " + exam.getTimeSlot().getSlotNumber();
                        }

                        pw.printf("%s,%s,%d,%s,%s,%d%n",
                                "EX" + String.format("%03d", Math.abs(exam.hashCode() % 1000)),
                                exam.getCourse().getCourseCode(),
                                exam.getTimeSlot().getDay(),
                                timeSlot,
                                exam.getClassroom().getClassroomID(),
                                exam.getStudentCount());
                        count++;
                    }
                }

                if (count == 0) {
                    showWarning("Empty Schedule",
                            "The saved file contains no exams because no exams have been scheduled yet.");
                }

                showInfo("Save Success",
                        "Schedule saved successfully!\n\n" +
                                "File: " + file.getName() + "\n" +
                                "Format: Standard CSV (Re-importable)\n" +
                                "Exams: " + count);
                messages.add("✓ Schedule saved: " + file.getName());

                // Open the folder
                try {
                    java.awt.Desktop.getDesktop().open(file.getParentFile());
                } catch (Exception e) {
                    // Ignore if we can't open the folder
                }

            } catch (Exception e) {
                showError("Save Failed", e.getMessage());
                messages.add("❌ Save failed: " + e.getMessage());
            }
        }
    }

    private void saveDetailedCSV(Stage owner, String baseName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Schedule - Detailed CSV");
        chooser.setInitialFileName(baseName + "_detailed.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {

                pw.write('\ufeff');

                pw.println(
                        "Exam ID,Course Code,Course Name,Day,Date,Day of Week,Time Slot,Room ID,Room Capacity,Enrolled Students,Utilization %,Status");

                LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                        ? examStartDatePicker.getValue()
                        : LocalDate.now();

                List<String> timeSlots = getTimeSlotsFromUI.get();

                for (Exam exam : dataManager.getSchedule().getExams()) {
                    if (exam.isScheduled()) {
                        LocalDate examDate = startDate.plusDays(exam.getTimeSlot().getDay() - 1);
                        String dayOfWeek = examDate.getDayOfWeek().toString();

                        String timeSlot = "";
                        try {
                            timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
                        } catch (Exception e) {
                            timeSlot = "Slot " + exam.getTimeSlot().getSlotNumber();
                        }

                        int enrolled = exam.getStudentCount();
                        int capacity = exam.getClassroom().getCapacity();
                        double utilization = (enrolled * 100.0) / capacity;
                        String status = utilization > 100 ? "OVERCAPACITY" : (utilization > 90 ? "FULL" : "OK");

                        pw.printf("%s,%s,\"%s\",%d,%s,%s,\"%s\",%s,%d,%d,%.1f%%,%s%n",
                                "EX" + String.format("%03d", Math.abs(exam.hashCode() % 1000)),
                                exam.getCourse().getCourseCode(),
                                exam.getCourse().getCourseName(),
                                exam.getTimeSlot().getDay(),
                                examDate.toString(),
                                dayOfWeek,
                                timeSlot,
                                exam.getClassroom().getClassroomID(),
                                capacity,
                                enrolled,
                                utilization,
                                status);
                    }
                }

                showInfo("Save Success",
                        "Detailed schedule saved successfully!\n\n" +
                                "File: " + file.getName() + "\n" +
                                "Format: Detailed CSV (Excel-ready)\n" +
                                "Exams: " + dataManager.getSchedule().getExams().size() + "\n\n" +
                                "This file can be opened directly in Excel.");
                messages.add("✓ Detailed schedule saved: " + file.getName());

            } catch (Exception e) {
                showError("Save Failed", e.getMessage());
                messages.add("❌ Save failed: " + e.getMessage());
            }
        }
    }

    private void saveBackupPackage(Stage owner, String baseName) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Location for Backup Package");
        File parentDir = chooser.showDialog(owner);

        if (parentDir != null) {
            try {

                String folderName = baseName + "_backup_" + LocalDate.now();
                File backupDir = new File(parentDir, folderName);
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }

                int savedFiles = 0;
                StringBuilder report = new StringBuilder();
                report.append("BACKUP REPORT\n");
                report.append("=".repeat(50)).append("\n\n");

                File scheduleFile = new File(backupDir, "schedule.csv");
                try (PrintWriter pw = new PrintWriter(scheduleFile, "UTF-8")) {
                    pw.println("ExamID,Course,Day,Slot,Room,Students");
                    List<String> timeSlots = getTimeSlotsFromUI.get();

                    for (Exam exam : dataManager.getSchedule().getExams()) {
                        if (exam.isScheduled()) {
                            String timeSlot = "";
                            try {
                                timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
                            } catch (Exception e) {
                                timeSlot = "Slot " + exam.getTimeSlot().getSlotNumber();
                            }

                            pw.printf("%s,%s,%d,%s,%s,%d%n",
                                    "EX" + String.format("%03d", Math.abs(exam.hashCode() % 1000)),
                                    exam.getCourse().getCourseCode(),
                                    exam.getTimeSlot().getDay(),
                                    timeSlot,
                                    exam.getClassroom().getClassroomID(),
                                    exam.getStudentCount());
                        }
                    }
                    savedFiles++;
                    report.append("✓ schedule.csv - ").append(dataManager.getSchedule().getExams().size())
                            .append(" exams\n");
                }

                File studentsFile = new File(backupDir, "students.csv");
                try (PrintWriter pw = new PrintWriter(studentsFile, "UTF-8")) {
                    pw.println("StudentID");
                    for (Student s : dataManager.getStudents()) {
                        pw.println(s.getStudentID());
                    }
                    savedFiles++;
                    report.append("✓ students.csv - ").append(dataManager.getStudents().size()).append(" students\n");
                }

                File coursesFile = new File(backupDir, "courses.csv");
                try (PrintWriter pw = new PrintWriter(coursesFile, "UTF-8")) {
                    pw.println("CourseCode,CourseName");
                    for (Course c : dataManager.getCourses()) {
                        pw.printf("%s,%s%n",
                                c.getCourseCode(),
                                c.getCourseName());
                    }
                    savedFiles++;
                    report.append("✓ courses.csv - ").append(dataManager.getCourses().size()).append(" courses\n");
                }

                File classroomsFile = new File(backupDir, "classrooms.csv");
                try (PrintWriter pw = new PrintWriter(classroomsFile, "UTF-8")) {
                    pw.println("ClassroomID;Capacity");
                    for (Classroom c : dataManager.getClassrooms()) {
                        pw.printf("%s;%d%n", c.getClassroomID(), c.getCapacity());
                    }
                    savedFiles++;
                    report.append("✓ classrooms.csv - ").append(dataManager.getClassrooms().size())
                            .append(" classrooms\n");
                }

                File attendanceFile = new File(backupDir, "attendance.csv");
                try (PrintWriter pw = new PrintWriter(attendanceFile, "UTF-8")) {
                    pw.println("CourseCode");
                    for (Course c : dataManager.getCourses()) {
                        if (!c.getEnrolledStudents().isEmpty()) {
                            pw.println(c.getCourseCode());
                            pw.print("[");
                            List<String> studentIds = c.getEnrolledStudents().stream()
                                    .map(Student::getStudentID)
                                    .collect(Collectors.toList());
                            pw.print(String.join(", ", studentIds));
                            pw.println("]");
                        }
                    }
                    savedFiles++;
                    report.append("✓ attendance.csv - Enrollment data\n");
                }

                File configFile = new File(backupDir, "config.txt");
                try (PrintWriter pw = new PrintWriter(configFile, "UTF-8")) {
                    pw.println("EXAM SCHEDULER CONFIGURATION");
                    pw.println("=".repeat(50));
                    pw.println("Backup Date: " + LocalDate.now());
                    pw.println("Backup Time: " + java.time.LocalTime.now());
                    pw.println();
                    pw.println("SCHEDULE PARAMETERS:");
                    pw.println("Exam Start Date: "
                            + (examStartDatePicker != null ? examStartDatePicker.getValue() : "N/A"));
                    pw.println("Exam Period (Days): " + daysSpinner.getValue());
                    pw.println("Time Slots Per Day: " + getTimeSlotsFromUI.get().size());
                    pw.println();
                    pw.println("TIME SLOTS:");
                    List<String> slots = getTimeSlotsFromUI.get();
                    for (int i = 0; i < slots.size(); i++) {
                        pw.println("  " + (i + 1) + ". " + slots.get(i));
                    }
                    pw.println();
                    pw.println("STATISTICS:");
                    pw.println("Total Students: " + dataManager.getStudents().size());
                    pw.println("Total Courses: " + dataManager.getCourses().size());
                    pw.println("Total Classrooms: " + dataManager.getClassrooms().size());
                    pw.println("Scheduled Exams: " + dataManager.getSchedule().getExams().size());
                    savedFiles++;
                    report.append("✓ config.txt - Configuration backup\n");
                }

                File readmeFile = new File(backupDir, "README.txt");
                try (PrintWriter pw = new PrintWriter(readmeFile, "UTF-8")) {
                    pw.println("EXAM SCHEDULER BACKUP PACKAGE");
                    pw.println("=".repeat(50));
                    pw.println();
                    pw.println("This backup package contains all data needed to restore");
                    pw.println("your exam schedule in the Exam Scheduler application.");
                    pw.println();
                    pw.println("CONTENTS:");
                    pw.println("  • schedule.csv - Complete exam schedule (re-importable)");
                    pw.println("  • students.csv - Student database");
                    pw.println("  • courses.csv - Course database");
                    pw.println("  • classrooms.csv - Classroom database");
                    pw.println("  • attendance.csv - Course enrollment data");
                    pw.println("  • config.txt - Schedule configuration");
                    pw.println("  • README.txt - This file");
                    pw.println();
                    pw.println("HOW TO RESTORE:");
                    pw.println("  1. Launch Exam Scheduler");
                    pw.println("  2. Click 'Load Data' and select this folder");
                    pw.println("  3. All data will be loaded automatically");
                    pw.println("  4. To restore the schedule, click 'Import Schedule'");
                    pw.println("     and select schedule.csv");
                    pw.println();
                    pw.println("Created: " + LocalDate.now() + " at " + java.time.LocalTime.now());
                    pw.println("Application Version: 2.0");
                    savedFiles++;
                    report.append("✓ README.txt - Instructions\n");
                }

                report.append("\n").append("=".repeat(50)).append("\n");
                report.append("Total files saved: ").append(savedFiles).append("\n");
                report.append("Location: ").append(backupDir.getAbsolutePath()).append("\n");

                showInfo("Backup Success",
                        "Complete backup package created!\n\n" +
                                "Location: " + folderName + "\n" +
                                "Files saved: " + savedFiles + "\n\n" +
                                report.toString() + "\n" +
                                "You can now safely restore this backup by:\n" +
                                "1. Load Data → Select backup folder\n" +
                                "2. Import Schedule → Select schedule.csv");

                messages.add("✓ Complete backup created: " + folderName);
                messages.add("  → " + savedFiles + " files saved successfully");

            } catch (Exception e) {
                showError("Backup Failed", "Error creating backup package:\n" + e.getMessage());
                messages.add("❌ Backup failed: " + e.getMessage());
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
        dialog.setTitle("📤 Export Schedule");

        Label formatLabel = new Label("Select export format:");
        formatLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        ComboBox<String> formatCombo = new ComboBox<>(FXCollections.observableArrayList(
                "📊 Excel-Compatible CSV (Detailed)",
                "📅 iCalendar Format (.ics)",
                "📄 PDF Report (Print-Ready)",
                "📋 Full Schedule (CSV)",
                "👥 Student-wise Schedules (PDF)",
                "🏫 Room-wise Schedules (CSV)",
                "📈 Statistical Report (TXT)"));
        formatCombo.setValue("📊 Excel-Compatible CSV (Detailed)");
        formatCombo.setPrefWidth(350);

        TextArea descArea = new TextArea();
        descArea.setEditable(false);
        descArea.setPrefHeight(120);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-font-size: 12px;");

        formatCombo.setOnAction(e -> {
            String selected = formatCombo.getValue();
            String desc = getExportDescription(selected);
            descArea.setText(desc);
        });
        descArea.setText(getExportDescription(formatCombo.getValue()));

        Button exportButton = new Button("Export");
        exportButton.setStyle(
                "-fx-background-color: #0078D4; -fx-text-fill: white; -fx-padding: 10px 30px; -fx-font-size: 14px;");

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-padding: 10px 30px; -fx-font-size: 14px;");
        cancelButton.setOnAction(e -> dialog.close());

        exportButton.setOnAction(e -> {
            String format = formatCombo.getValue();
            dialog.close();

            try {
                if (format.contains("Excel-Compatible CSV")) {
                    exportDetailedCSV(owner);
                } else if (format.contains("iCalendar")) {
                    exportICalendar(owner);
                } else if (format.contains("PDF Report")) {
                    exportPDFReport(owner);
                } else if (format.contains("Full Schedule")) {
                    saveStandardCSV(owner, "exam_schedule_" + LocalDate.now());
                } else if (format.contains("Student-wise")) {
                    exportStudentWise(owner);
                } else if (format.contains("Room-wise")) {
                    exportRoomWise(owner);
                } else if (format.contains("Statistical")) {
                    exportStatisticalReport(owner);
                }
            } catch (Exception ex) {
                showError("Export Failed", "Error during export: " + ex.getMessage());
            }
        });

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(25));

        Label title = new Label("📤 Export Schedule");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0078D4;");

        VBox formatBox = new VBox(8, formatLabel, formatCombo);

        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-weight: bold;");
        VBox descBox = new VBox(5, descLabel, descArea);

        HBox buttonBox = new HBox(10, exportButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(title, new Separator(), formatBox, descBox, buttonBox);

        Scene scene = new Scene(layout, 500, 450);
        ThemeManager.getInstance().registerScene(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private String getExportDescription(String format) {
        if (format.contains("Excel-Compatible")) {
            return "Exports a detailed CSV file with all exam information including:\n" +
                    "• Course details\n" +
                    "• Date, time, and room assignments\n" +
                    "• Student counts and capacity utilization\n" +
                    "• Directly importable to Excel/Google Sheets";
        } else if (format.contains("iCalendar")) {
            return "Creates an .ics calendar file that can be:\n" +
                    "• Imported to Outlook, Google Calendar, Apple Calendar\n" +
                    "• Shared with students and faculty\n" +
                    "• Synchronized across devices\n" +
                    "• Includes reminders and locations";
        } else if (format.contains("PDF Report")) {
            return "Generates a formatted PDF document with:\n" +
                    "• Professional layout for printing\n" +
                    "• Complete schedule with visual organization\n" +
                    "• Summary statistics and charts\n" +
                    "• Ready for distribution or posting";
        } else if (format.contains("Full Schedule")) {
            return "Basic CSV export with essential information:\n" +
                    "• Exam ID, Course, Day, Time Slot, Room, Students\n" +
                    "• Simple format for quick reference\n" +
                    "• Easy to import back into the system";
        } else if (format.contains("Student-wise")) {
            return "Creates a PDF with all student schedules:\n" +
                    "• Professional PDF format\n" +
                    "• All schedules in one document\n" +
                    "• Easy to search and review\n" +
                    "• Ready for printing or distribution";
        } else if (format.contains("Room-wise")) {
            return "Exports schedule organized by classroom:\n" +
                    "• Shows which exams are in each room\n" +
                    "• Helps with room management and setup\n" +
                    "• Includes capacity utilization data\n" +
                    "• Useful for facility coordinators";
        } else if (format.contains("Statistical")) {
            return "Generates comprehensive statistics report:\n" +
                    "• Placement rates and efficiency metrics\n" +
                    "• Room utilization analysis\n" +
                    "• Student load distribution\n" +
                    "• Conflict analysis and recommendations";
        }
        return "";
    }

    private void exportDetailedCSV(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Detailed CSV");
        chooser.setInitialFileName("exam_schedule_detailed_" + LocalDate.now() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {

                pw.write('\ufeff'); // BOM for Excel

                pw.println(
                        "Exam ID,Course Code,Day,Date,Time Slot,Room ID,Room Capacity,Enrolled Students");

                LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                        ? examStartDatePicker.getValue()
                        : LocalDate.now();

                List<String> timeSlots = getTimeSlotsFromUI.get();

                for (Exam exam : dataManager.getSchedule().getExams()) {
                    if (exam.isScheduled()) {
                        LocalDate examDate = startDate.plusDays(exam.getTimeSlot().getDay() - 1);
                        String timeSlot = "";
                        try {
                            timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
                        } catch (Exception e) {
                            timeSlot = "Slot " + exam.getTimeSlot().getSlotNumber();
                        }

                        int enrolled = exam.getStudentCount();
                        int capacity = exam.getClassroom().getCapacity();

                        pw.printf("%s,%s,%d,%s,%s,%s,%d,%d%n",
                                "EX" + String.format("%03d", exam.hashCode() % 1000),
                                exam.getCourse().getCourseCode(),
                                exam.getTimeSlot().getDay(),
                                examDate.toString(),
                                "\"" + timeSlot + "\"",
                                exam.getClassroom().getClassroomID(),
                                capacity,
                                enrolled);
                    }
                }
                showInfo("Export Success", "Detailed CSV exported to:\n" + file.getName());
                messages.add("✓ Detailed CSV exported successfully");

            } catch (Exception e) {
                showError("Export Failed", e.getMessage());
            }
        }
    }

    private void exportICalendar(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export iCalendar");
        chooser.setInitialFileName("exam_schedule_" + LocalDate.now() + ".ics");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("iCalendar Files", "*.ics"));
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {
                pw.println("BEGIN:VCALENDAR");
                pw.println("VERSION:2.0");
                pw.println("PRODID:-//Exam Scheduler//EN");
                pw.println("CALSCALE:GREGORIAN");
                pw.println("METHOD:PUBLISH");
                pw.println("X-WR-CALNAME:Exam Schedule");
                pw.println("X-WR-TIMEZONE:UTC");

                LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                        ? examStartDatePicker.getValue()
                        : LocalDate.now();

                List<String> timeSlots = getTimeSlotsFromUI.get();

                for (Exam exam : dataManager.getSchedule().getExams()) {
                    if (exam.isScheduled()) {
                        LocalDate examDate = startDate.plusDays(exam.getTimeSlot().getDay() - 1);
                        String timeSlot = "";
                        try {
                            timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
                        } catch (Exception e) {
                            timeSlot = "09:00-11:00";
                        }

                        String[] times = timeSlot.split("-");
                        String startTime = times[0].replace(":", "") + "00";
                        String endTime = times.length > 1 ? times[1].replace(":", "") + "00" : "110000";

                        String dateStr = examDate.toString().replace("-", "");

                        pw.println("BEGIN:VEVENT");
                        pw.println("UID:" + exam.getCourse().getCourseCode() + "-" + dateStr + "@examscheduler");
                        pw.println("DTSTAMP:" + LocalDate.now().toString().replace("-", "") + "T120000Z");
                        pw.println("DTSTART:" + dateStr + "T" + startTime + "Z");
                        pw.println("DTEND:" + dateStr + "T" + endTime + "Z");
                        pw.println("SUMMARY:Exam: " + exam.getCourse().getCourseCode());
                        pw.println("DESCRIPTION:" + exam.getCourse().getCourseName());
                        pw.println("LOCATION:Room " + exam.getClassroom().getClassroomID());
                        pw.println("STATUS:CONFIRMED");
                        pw.println("BEGIN:VALARM");
                        pw.println("TRIGGER:-PT24H");
                        pw.println("ACTION:DISPLAY");
                        pw.println("DESCRIPTION:Exam tomorrow!");
                        pw.println("END:VALARM");
                        pw.println("END:VEVENT");
                    }
                }

                pw.println("END:VCALENDAR");

                showInfo("Export Success",
                        "iCalendar file exported to:\n" + file.getName() +
                                "\n\nYou can now import this file to:\n" +
                                "• Google Calendar\n• Outlook\n• Apple Calendar\n• Any calendar app");
                messages.add("✓ iCalendar exported successfully");

                // Open the folder
                try {
                    java.awt.Desktop.getDesktop().open(file.getParentFile());
                } catch (Exception e) {
                    // Ignore
                }
            } catch (Exception e) {
                showError("Export Failed", e.getMessage());
            }
        }
    }

    private void exportStudentWise(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export All Student Schedules");
        chooser.setInitialFileName("all_student_schedules.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            Document document = new Document();
            int exportCount = 0;
            try {
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
                Font studentFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

                Paragraph title = new Paragraph("ALL STUDENT SCHEDULES", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(10);
                document.add(title);

                Paragraph genDate = new Paragraph("Generated: " + LocalDate.now(), normalFont);
                genDate.setAlignment(Element.ALIGN_CENTER);
                genDate.setSpacingAfter(20);
                document.add(genDate);

                List<String> timeSlots = getTimeSlotsFromUI.get();
                LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                        ? examStartDatePicker.getValue()
                        : LocalDate.now();

                for (Student student : dataManager.getStudents()) {
                    List<Exam> studentExams = getStudentExams(student);
                    if (!studentExams.isEmpty()) {
                        Paragraph studentHeader = new Paragraph("Student: " + student.getStudentID(), studentFont);
                        studentHeader.setSpacingBefore(15);
                        studentHeader.setSpacingAfter(10);
                        document.add(studentHeader);

                        PdfPTable table = new PdfPTable(4);
                        table.setWidthPercentage(100);
                        table.setWidths(new float[] { 2, 2, 2, 1.5f });

                        String[] headers = { "Date", "Time", "Course Code", "Room" };
                        for (String header : headers) {
                            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            cell.setBackgroundColor(new BaseColor(70, 130, 180));
                            cell.setPadding(5);
                            table.addCell(cell);
                        }

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

                                    table.addCell(new Phrase(examDate.toString(), normalFont));
                                    table.addCell(new Phrase(timeSlot, normalFont));
                                    table.addCell(new Phrase(exam.getCourse().getCourseCode(), normalFont));
                                    table.addCell(new Phrase(exam.getClassroom().getClassroomID(), normalFont));
                                });

                        document.add(table);
                        exportCount++;
                    }
                }

                document.close();
                showInfo("Export Success",
                        "Exported schedules for " + exportCount + " students to:\n" + file.getName());
                messages.add("✓ Exported student schedules as PDF");

                // Open the file
                try {
                    java.awt.Desktop.getDesktop().open(file);
                } catch (Exception e) {
                    // Ignore
                }
            } catch (Exception e) {
                showError("Export Failed", e.getMessage());
            }
        }
    }

    private void exportRoomWise(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Room-wise Schedule");
        chooser.setInitialFileName("room_schedule_" + LocalDate.now() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {
                pw.write('\ufeff');
                pw.println("Room ID,Room Capacity,Day,Date,Time Slot,Course Code,Students");

                LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                        ? examStartDatePicker.getValue()
                        : LocalDate.now();

                List<String> timeSlots = getTimeSlotsFromUI.get();

                dataManager.getSchedule().getExams().stream()
                        .filter(Exam::isScheduled)
                        .sorted(Comparator.comparing((Exam e) -> e.getClassroom().getClassroomID())
                                .thenComparing(e -> e.getTimeSlot().getDay())
                                .thenComparing(e -> e.getTimeSlot().getSlotNumber()))
                        .forEach(exam -> {
                            LocalDate examDate = startDate.plusDays(exam.getTimeSlot().getDay() - 1);
                            String timeSlot = "";
                            try {
                                timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
                            } catch (Exception e) {
                                timeSlot = "Slot " + exam.getTimeSlot().getSlotNumber();
                            }

                            int enrolled = exam.getStudentCount();
                            int capacity = exam.getClassroom().getCapacity();

                            pw.printf("%s,%d,%d,%s,\"%s\",%s,%d%n",
                                    exam.getClassroom().getClassroomID(),
                                    capacity,
                                    exam.getTimeSlot().getDay(),
                                    examDate.toString(),
                                    timeSlot,
                                    exam.getCourse().getCourseCode(),
                                    enrolled);
                        });

                showInfo("Export Success", "Room-wise schedule exported to:\n" + file.getName());
                messages.add("✓ Room-wise schedule exported");
            } catch (Exception e) {
                showError("Export Failed", e.getMessage());
            }
        }
    }

    private void exportStatisticalReport(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Statistical Report");
        chooser.setInitialFileName("statistics_" + LocalDate.now() + ".txt");
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file)) {
                pw.println("╔══════════════════════════════════════════════╗");
                pw.println("║     EXAM SCHEDULE STATISTICAL REPORT         ║");
                pw.println("╚══════════════════════════════════════════════╝");
                pw.println("\nGenerated: " + LocalDate.now());
                pw.println("\n" + "=".repeat(50));

                pw.println("\n📊 BASIC STATISTICS");
                pw.println("-".repeat(50));
                int totalCourses = dataManager.getCourses().size();
                int placedExams = (int) dataManager.getSchedule().getExams().stream()
                        .filter(Exam::isScheduled).count();
                int unplacedExams = totalCourses - placedExams;

                pw.println("Total Courses: " + totalCourses);
                pw.println("Placed Exams: " + placedExams);
                pw.println("Unplaced Exams: " + unplacedExams);
                pw.printf("Placement Rate: %.1f%%\n", (placedExams * 100.0 / totalCourses));

                pw.println("\n🏫 ROOM UTILIZATION");
                pw.println("-".repeat(50));
                Map<String, List<Exam>> roomExams = dataManager.getSchedule().getExams().stream()
                        .filter(Exam::isScheduled)
                        .collect(Collectors.groupingBy(e -> e.getClassroom().getClassroomID()));

                roomExams.forEach((room, exams) -> {
                    double avgUtil = exams.stream()
                            .mapToDouble(e -> (e.getStudentCount() * 100.0) / e.getClassroom().getCapacity())
                            .average().orElse(0);
                    pw.printf("Room %s: %d exams, Avg Utilization: %.1f%%\n",
                            room, exams.size(), avgUtil);
                });

                pw.println("\n⏰ TIME SLOT DISTRIBUTION");
                pw.println("-".repeat(50));
                Map<Integer, Long> slotDist = dataManager.getSchedule().getExams().stream()
                        .filter(Exam::isScheduled)
                        .collect(Collectors.groupingBy(e -> e.getTimeSlot().getSlotNumber(), Collectors.counting()));

                slotDist.forEach((slot, count) -> pw.printf("Slot %d: %d exams\n", slot, count));

                pw.println("\n👥 STUDENT EXAM LOAD");
                pw.println("-".repeat(50));
                Map<Integer, Long> studentLoad = new HashMap<>();
                for (Student student : dataManager.getStudents()) {
                    int examCount = (int) dataManager.getSchedule().getExams().stream()
                            .filter(e -> e.isScheduled() && e.getEnrolledStudents().contains(student))
                            .count();
                    studentLoad.merge(examCount, 1L, Long::sum);
                }

                studentLoad.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> pw.printf("%d exams: %d students\n", e.getKey(), e.getValue()));

                showInfo("Export Success", "Statistical report exported to:\n" + file.getName());
                messages.add("✓ Statistical report exported");
            } catch (Exception e) {
                showError("Export Failed", e.getMessage());
            }
        }
    }

    private void exportPDFReport(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Print-Ready Report");
        chooser.setInitialFileName("exam_schedule_report_" + LocalDate.now() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            Document document = new Document();
            try {
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

                Paragraph title = new Paragraph("EXAM SCHEDULER - OFFICIAL REPORT", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);

                document.add(new Paragraph("Generated: " + LocalDate.now(), normalFont));
                document.add(new Paragraph(" ", normalFont)); // Spacer

                PdfPTable table = new PdfPTable(4); // 4 columns
                table.setWidthPercentage(100);
                table.setWidths(new float[] { 2, 2, 2, 1.5f });

                // Headers
                String[] headers = { "Date", "Time", "Course Code", "Room" };
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    cell.setPadding(5);
                    table.addCell(cell);
                }

                LocalDate startDate = examStartDatePicker != null && examStartDatePicker.getValue() != null
                        ? examStartDatePicker.getValue()
                        : LocalDate.now();

                List<String> timeSlots = getTimeSlotsFromUI.get();

                List<Exam> sortedExams = dataManager.getSchedule().getExams().stream()
                        .filter(Exam::isScheduled)
                        .sorted(Comparator.comparing((Exam e) -> e.getTimeSlot().getDay())
                                .thenComparing(e -> e.getTimeSlot().getSlotNumber()))
                        .collect(Collectors.toList());

                for (Exam exam : sortedExams) {
                    LocalDate examDate = startDate.plusDays(exam.getTimeSlot().getDay() - 1);
                    String timeSlot = "";
                    try {
                        timeSlot = timeSlots.get(exam.getTimeSlot().getSlotNumber() - 1);
                    } catch (Exception e) {
                        timeSlot = "Slot " + exam.getTimeSlot().getSlotNumber();
                    }

                    table.addCell(new Phrase(examDate.toString(), normalFont));
                    table.addCell(new Phrase(timeSlot, normalFont));
                    table.addCell(new Phrase(exam.getCourse().getCourseCode(), normalFont));
                    table.addCell(new Phrase(exam.getClassroom().getClassroomID(), normalFont));
                }

                document.add(table);

                // Add Summary Statistics Section
                document.add(new Paragraph("\n"));

                Paragraph statsTitle = new Paragraph("SUMMARY STATISTICS", headerFont);
                statsTitle.setAlignment(Element.ALIGN_CENTER);
                statsTitle.setSpacingBefore(20);
                statsTitle.setSpacingAfter(10);
                document.add(statsTitle);

                // Basic Statistics
                int totalExams = sortedExams.size();
                int totalCourses = dataManager.getCourses().size();
                int totalStudents = dataManager.getStudents().size();
                int totalRooms = dataManager.getClassrooms().size();

                PdfPTable statsTable = new PdfPTable(2);
                statsTable.setWidthPercentage(60);
                statsTable.setHorizontalAlignment(Element.ALIGN_CENTER);

                addStatRow(statsTable, "Total Scheduled Exams:", String.valueOf(totalExams), normalFont, headerFont);
                addStatRow(statsTable, "Total Courses:", String.valueOf(totalCourses), normalFont, headerFont);
                addStatRow(statsTable, "Total Students:", String.valueOf(totalStudents), normalFont, headerFont);
                addStatRow(statsTable, "Total Classrooms:", String.valueOf(totalRooms), normalFont, headerFont);

                document.add(statsTable);

                // Room Utilization
                document.add(new Paragraph("\nRoom Utilization Analysis", headerFont));
                document.add(new Paragraph(" ", normalFont));

                Map<String, List<Exam>> roomExams = sortedExams.stream()
                        .collect(Collectors.groupingBy(e -> e.getClassroom().getClassroomID()));

                PdfPTable roomTable = new PdfPTable(3);
                roomTable.setWidthPercentage(80);

                PdfPCell roomHeader1 = new PdfPCell(new Phrase("Room", headerFont));
                PdfPCell roomHeader2 = new PdfPCell(new Phrase("Exams", headerFont));
                PdfPCell roomHeader3 = new PdfPCell(new Phrase("Avg Utilization", headerFont));
                roomHeader1.setBackgroundColor(BaseColor.LIGHT_GRAY);
                roomHeader2.setBackgroundColor(BaseColor.LIGHT_GRAY);
                roomHeader3.setBackgroundColor(BaseColor.LIGHT_GRAY);
                roomHeader1.setHorizontalAlignment(Element.ALIGN_CENTER);
                roomHeader2.setHorizontalAlignment(Element.ALIGN_CENTER);
                roomHeader3.setHorizontalAlignment(Element.ALIGN_CENTER);
                roomTable.addCell(roomHeader1);
                roomTable.addCell(roomHeader2);
                roomTable.addCell(roomHeader3);

                roomExams.forEach((room, exams) -> {
                    double avgUtil = exams.stream()
                            .mapToDouble(e -> (e.getStudentCount() * 100.0) / e.getClassroom().getCapacity())
                            .average().orElse(0);
                    roomTable.addCell(new Phrase(room, normalFont));
                    roomTable.addCell(new Phrase(String.valueOf(exams.size()), normalFont));
                    roomTable.addCell(new Phrase(String.format("%.1f%%", avgUtil), normalFont));
                });

                document.add(roomTable);

                // Time Slot Distribution
                document.add(new Paragraph("\nTime Slot Distribution", headerFont));
                document.add(new Paragraph(" ", normalFont));

                Map<Integer, Long> slotDist = sortedExams.stream()
                        .collect(Collectors.groupingBy(e -> e.getTimeSlot().getSlotNumber(), Collectors.counting()));

                PdfPTable slotTable = new PdfPTable(2);
                slotTable.setWidthPercentage(50);

                PdfPCell slotHeader1 = new PdfPCell(new Phrase("Time Slot", headerFont));
                PdfPCell slotHeader2 = new PdfPCell(new Phrase("Exams", headerFont));
                slotHeader1.setBackgroundColor(BaseColor.LIGHT_GRAY);
                slotHeader2.setBackgroundColor(BaseColor.LIGHT_GRAY);
                slotHeader1.setHorizontalAlignment(Element.ALIGN_CENTER);
                slotHeader2.setHorizontalAlignment(Element.ALIGN_CENTER);
                slotTable.addCell(slotHeader1);
                slotTable.addCell(slotHeader2);

                slotDist.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            try {
                                String slotName = timeSlots.get(entry.getKey() - 1);
                                slotTable.addCell(new Phrase(slotName, normalFont));
                                slotTable.addCell(new Phrase(String.valueOf(entry.getValue()), normalFont));
                            } catch (Exception e) {
                                slotTable.addCell(new Phrase("Slot " + entry.getKey(), normalFont));
                                slotTable.addCell(new Phrase(String.valueOf(entry.getValue()), normalFont));
                            }
                        });

                document.add(slotTable);

                document.close();

                showInfo("Export Success", "PDF report exported to:\n" + file.getName());
                messages.add("✓ PDF report exported");

                // Open the file
                try {
                    java.awt.Desktop.getDesktop().open(file);
                } catch (Exception e) {
                    // Ignore
                }

            } catch (Exception e) {
                showError("Export Failed", "Error creating PDF: " + e.getMessage());
            }
        }
    }

    private void addStatRow(PdfPTable table, String label, String value, Font normalFont, Font headerFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, headerFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, normalFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void handleValidate() {
        if (dataManager.getSchedule() == null || dataManager.getSchedule().getExams().isEmpty()) {
            showWarning("No Schedule", "Please generate a schedule first to validate.");
            messages.add("⚠ Validation skipped: No schedule to validate");
            return;
        }

        Stage validationDialog = new Stage();
        validationDialog.initModality(Modality.APPLICATION_MODAL);
        validationDialog.setTitle("✓ Validation Options");

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("🔍 Schedule Validation");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0078D4;");

        Label subtitle = new Label("Select validation checks to perform:");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        CheckBox checkStudentConflicts = new CheckBox("Student Conflicts (Same time slot)");
        checkStudentConflicts.setSelected(true);
        checkStudentConflicts.setStyle("-fx-font-size: 13px;");

        CheckBox checkConsecutive = new CheckBox("Consecutive Exams (Back-to-back)");
        checkConsecutive.setSelected(true);
        checkConsecutive.setStyle("-fx-font-size: 13px;");

        CheckBox checkRoomConflicts = new CheckBox("Room Double-booking");
        checkRoomConflicts.setSelected(true);
        checkRoomConflicts.setStyle("-fx-font-size: 13px;");

        CheckBox checkCapacity = new CheckBox("Room Capacity Violations");
        checkCapacity.setSelected(true);
        checkCapacity.setStyle("-fx-font-size: 13px;");

        CheckBox checkStudentLoad = new CheckBox("Student Exam Load Analysis");
        checkStudentLoad.setSelected(false);
        checkStudentLoad.setStyle("-fx-font-size: 13px;");

        CheckBox checkRoomUtilization = new CheckBox("Room Utilization Analysis");
        checkRoomUtilization.setSelected(false);
        checkRoomUtilization.setStyle("-fx-font-size: 13px;");

        CheckBox checkTimeDistribution = new CheckBox("Time Distribution Balance");
        checkTimeDistribution.setSelected(false);
        checkTimeDistribution.setStyle("-fx-font-size: 13px;");

        VBox criticalChecks = new VBox(5);
        Label criticalLabel = new Label("Critical Checks (Errors):");
        criticalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #D32F2F;");
        criticalChecks.getChildren().addAll(
                criticalLabel,
                checkStudentConflicts,
                checkRoomConflicts,

                checkCapacity);

        VBox warningChecks = new VBox(5);
        Label warningLabel = new Label("Warning Checks:");
        warningLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #F57C00;");
        warningChecks.getChildren().addAll(
                warningLabel,
                checkConsecutive);

        VBox analysisChecks = new VBox(5);
        Label analysisLabel = new Label("Analysis & Statistics:");
        analysisLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1976D2;");
        analysisChecks.getChildren().addAll(
                analysisLabel,
                checkStudentLoad,
                checkRoomUtilization,
                checkTimeDistribution);

        Button validateBtn = new Button("🔍 Run Validation");
        validateBtn.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10px 30px; -fx-font-size: 14px;");

        Button quickValidateBtn = new Button("⚡ Quick Validate (Critical Only)");
        quickValidateBtn.setStyle(
                "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 10px 30px; -fx-font-size: 14px;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-padding: 10px 30px; -fx-font-size: 14px;");

        validateBtn.setOnAction(e -> {
            validationDialog.close();
            ValidationOptions options = new ValidationOptions(
                    checkStudentConflicts.isSelected(),
                    checkConsecutive.isSelected(),
                    checkRoomConflicts.isSelected(),
                    checkCapacity.isSelected(),
                    checkStudentLoad.isSelected(),
                    checkRoomUtilization.isSelected(),
                    checkTimeDistribution.isSelected());
            performValidation(options);
        });

        quickValidateBtn.setOnAction(e -> {
            validationDialog.close();
            ValidationOptions options = new ValidationOptions(true, false, true, true, false, false, false);
            performValidation(options);
        });

        cancelBtn.setOnAction(e -> validationDialog.close());

        VBox buttonBox = new VBox(10, validateBtn, quickValidateBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);

        // Make buttons consistent width
        validateBtn.setMaxWidth(Double.MAX_VALUE);
        quickValidateBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setMaxWidth(Double.MAX_VALUE);

        // Limit widest button width if needed, or let them fill the container with some
        // padding
        VBox.setMargin(validateBtn, new Insets(0, 50, 0, 50));
        VBox.setMargin(quickValidateBtn, new Insets(0, 50, 0, 50));
        VBox.setMargin(cancelBtn, new Insets(0, 50, 0, 50));

        layout.getChildren().addAll(
                title,
                subtitle,
                new Separator(),
                criticalChecks,
                new Separator(),
                warningChecks,
                new Separator(),
                analysisChecks,
                new Separator(),
                buttonBox);

        Scene scene = new Scene(layout, 550, 720);
        ThemeManager.getInstance().registerScene(scene);
        validationDialog.setScene(scene);
        validationDialog.showAndWait();
    }

    private static class ValidationOptions {
        boolean checkStudentConflicts;
        boolean checkConsecutive;
        boolean checkRoomConflicts;
        boolean checkCapacity;
        boolean checkStudentLoad;
        boolean checkRoomUtilization;
        boolean checkTimeDistribution;

        ValidationOptions(boolean studentConflicts, boolean consecutive, boolean roomConflicts,
                          boolean capacity, boolean studentLoad,
                          boolean roomUtilization, boolean timeDistribution) {
            this.checkStudentConflicts = studentConflicts;
            this.checkConsecutive = consecutive;
            this.checkRoomConflicts = roomConflicts;
            this.checkCapacity = capacity;
            this.checkStudentLoad = studentLoad;
            this.checkRoomUtilization = roomUtilization;
            this.checkTimeDistribution = timeDistribution;
        }
    }

    private void performValidation(ValidationOptions options) {
        messages.add("🔎 Starting comprehensive validation...");

        ValidationResult result = new ValidationResult();
        List<Exam> placedExams = dataManager.getSchedule().getExams().stream()
                .filter(Exam::isScheduled)
                .collect(Collectors.toList());

        if (placedExams.isEmpty()) {
            showWarning("No Exams", "No exams have been placed in the schedule.");
            return;
        }

        Map<Student, Set<TimeSlot>> studentScheduledSlots = new HashMap<>();
        Map<TimeSlot, Set<String>> roomOccupancy = new HashMap<>();
        for (Exam exam : placedExams) {
            TimeSlot currentSlot = exam.getTimeSlot();
            String roomID = exam.getClassroom().getClassroomID();
            String courseCode = exam.getCourse().getCourseCode();
            List<Student> students = exam.getEnrolledStudents();

            if (options.checkRoomConflicts) {
                if (roomOccupancy.getOrDefault(currentSlot, new HashSet<>()).contains(roomID)) {
                    result.addCritical("Room Double-booking: " + courseCode + " conflicts with another exam at Day " +
                            currentSlot.getDay() + ", Slot " + currentSlot.getSlotNumber() + " in Room " + roomID);
                }
                roomOccupancy.computeIfAbsent(currentSlot, k -> new HashSet<>()).add(roomID);
            }

            if (options.checkCapacity) {
                int enrolled = exam.getStudentCount();
                int capacity = exam.getClassroom().getCapacity();
                if (enrolled > capacity) {
                    result.addCritical("Capacity Violation: " + courseCode + " has " + enrolled +
                            " students but room " + roomID + " capacity is only " + capacity);
                }
            }

            for (Student student : students) {
                Set<TimeSlot> busySlots = studentScheduledSlots.computeIfAbsent(student, k -> new HashSet<>());

                if (options.checkStudentConflicts && busySlots.contains(currentSlot)) {
                    result.addCritical("Student Conflict: " + student.getStudentID() +
                            " has multiple exams at Day " + currentSlot.getDay() + ", Slot "
                            + currentSlot.getSlotNumber() +
                            " (includes " + courseCode + ")");
                }

                if (options.checkConsecutive && currentSlot.getSlotNumber() > 1) {
                    TimeSlot previousSlot = new TimeSlot(currentSlot.getDay(), currentSlot.getSlotNumber() - 1);
                    if (busySlots.contains(previousSlot)) {
                        result.addWarning("Consecutive Exam: " + student.getStudentID() +
                                " has " + courseCode + " immediately after another exam on Day "
                                + currentSlot.getDay());
                    }
                }

                busySlots.add(currentSlot);
            }
        }

        if (!unplacedCourses.isEmpty()) {
            for (String course : unplacedCourses) {
                result.addCritical("Unplaced Course: " + course + " could not be scheduled");
            }
        }

        if (options.checkStudentLoad) {
            analyzeStudentLoad(result, studentScheduledSlots);
        }

        if (options.checkRoomUtilization) {
            analyzeRoomUtilization(result, placedExams);
        }

        if (options.checkTimeDistribution) {
            analyzeTimeDistribution(result, placedExams);
        }

        showValidationResults(result, options);
    }

    private static class ValidationResult {
        List<String> critical = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> info = new ArrayList<>();
        List<String> analysis = new ArrayList<>();

        void addCritical(String msg) {
            critical.add(msg);
        }

        void addWarning(String msg) {
            warnings.add(msg);
        }

        void addInfo(String msg) {
            info.add(msg);
        }

        void addAnalysis(String msg) {
            analysis.add(msg);
        }

        int getCriticalCount() {
            return critical.size();
        }

        int getWarningCount() {
            return warnings.size();
        }

    }

    private void analyzeStudentLoad(ValidationResult result, Map<Student, Set<TimeSlot>> studentScheduledSlots) {
        result.addAnalysis("--- STUDENT LOAD ANALYSIS ---");

        Map<Integer, Integer> loadDistribution = new HashMap<>();
        int maxLoad = 0;
        int minLoad = Integer.MAX_VALUE;
        int totalExams = 0;

        for (Map.Entry<Student, Set<TimeSlot>> entry : studentScheduledSlots.entrySet()) {
            int examCount = entry.getValue().size();
            loadDistribution.merge(examCount, 1, Integer::sum);
            maxLoad = Math.max(maxLoad, examCount);
            minLoad = Math.min(minLoad, examCount);
            totalExams += examCount;
        }

        int studentCount = studentScheduledSlots.size();
        double avgLoad = studentCount > 0 ? (double) totalExams / studentCount : 0;

        result.addAnalysis("Total Students: " + studentCount);
        result.addAnalysis("Average Exams per Student: " + String.format("%.2f", avgLoad));
        result.addAnalysis("Min Exams: " + minLoad + " | Max Exams: " + maxLoad);
        result.addAnalysis("\nLoad Distribution:");

        loadDistribution.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    String bar = "█".repeat(Math.min(e.getValue() / 5, 20));
                    result.addAnalysis(String.format("  %2d exams: %3d students %s",
                            e.getKey(), e.getValue(), bar));
                });

        if (maxLoad > 8) {
            result.addWarning("High Student Load: Some students have " + maxLoad + " exams (consider spreading)");
        }
    }

    private void analyzeRoomUtilization(ValidationResult result, List<Exam> placedExams) {
        result.addAnalysis("\n--- ROOM UTILIZATION ANALYSIS ---");

        Map<String, List<Exam>> roomExams = placedExams.stream()
                .collect(Collectors.groupingBy(e -> e.getClassroom().getClassroomID()));

        double totalUtilization = 0;
        int roomCount = 0;

        for (Map.Entry<String, List<Exam>> entry : roomExams.entrySet()) {
            String roomId = entry.getKey();
            List<Exam> exams = entry.getValue();

            double avgUtil = exams.stream()
                    .mapToDouble(e -> (e.getStudentCount() * 100.0) / e.getClassroom().getCapacity())
                    .average().orElse(0);

            totalUtilization += avgUtil;
            roomCount++;

            String status = avgUtil > 90 ? "🔴 HIGH" : (avgUtil > 70 ? "🟡 GOOD" : "🟢 LOW");
            result.addAnalysis(String.format("  Room %s: %d exams, Avg: %.1f%% %s",
                    roomId, exams.size(), avgUtil, status));

            if (avgUtil < 50) {
                result.addInfo("Underutilized: Room " + roomId + " average utilization is only " +
                        String.format("%.1f%%", avgUtil));
            }
        }

        double overallUtil = roomCount > 0 ? totalUtilization / roomCount : 0;
        result.addAnalysis(String.format("\nOverall Room Utilization: %.1f%%", overallUtil));

        if (overallUtil < 60) {
            result.addWarning("Low Overall Utilization: Consider using fewer rooms or shorter exam period");
        }
    }

    private void analyzeTimeDistribution(ValidationResult result, List<Exam> placedExams) {
        result.addAnalysis("\n--- TIME DISTRIBUTION ANALYSIS ---");

        Map<Integer, Long> dayDistribution = placedExams.stream()
                .collect(Collectors.groupingBy(e -> e.getTimeSlot().getDay(), Collectors.counting()));

        Map<Integer, Long> slotDistribution = placedExams.stream()
                .collect(Collectors.groupingBy(e -> e.getTimeSlot().getSlotNumber(), Collectors.counting()));

        result.addAnalysis("Exams per Day:");
        dayDistribution.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    String bar = "█".repeat(Math.min(e.getValue().intValue(), 30));
                    result.addAnalysis(String.format("  Day %d: %2d exams %s", e.getKey(), e.getValue(), bar));
                });

        result.addAnalysis("\nExams per Time Slot:");
        slotDistribution.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    String bar = "█".repeat(Math.min(e.getValue().intValue(), 30));
                    List<String> slots = getTimeSlotsFromUI.get();
                    String slotLabel = e.getKey() <= slots.size() ? slots.get(e.getKey() - 1) : "Slot " + e.getKey();
                    result.addAnalysis(String.format("  %s: %2d exams %s", slotLabel, e.getValue(), bar));
                });

        long maxDay = dayDistribution.values().stream().max(Long::compare).orElse(0L);
        long minDay = dayDistribution.values().stream().min(Long::compare).orElse(0L);

        if (maxDay > minDay * 2) {
            result.addWarning("Unbalanced Schedule: Some days have significantly more exams than others");
        }
    }

    private void showValidationResults(ValidationResult result, ValidationOptions options) {
        Stage resultStage = new Stage();
        resultStage.initModality(Modality.APPLICATION_MODAL);
        resultStage.setTitle("✓ Validation Results");
        resultStage.setResizable(true);

        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
        resultArea.setWrapText(true);

        StringBuilder sb = new StringBuilder();

        sb.append("╔══════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                    VALIDATION REPORT                             ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════╝\n\n");

        sb.append("Generated: ").append(LocalDate.now()).append(" ").append(java.time.LocalTime.now()).append("\n");
        sb.append("Total Exams: ").append(dataManager.getSchedule().getExams().size()).append("\n");
        sb.append("Placed Exams: ").append(exams.size()).append("\n");
        sb.append("Unplaced Courses: ").append(unplacedCourses.size()).append("\n\n");

        sb.append("═══ SUMMARY ═══════════════════════════════════════════════════════\n");
        sb.append(String.format("❌ Critical Issues: %d\n", result.getCriticalCount()));
        sb.append(String.format("⚠️  Warnings: %d\n", result.getWarningCount()));
        sb.append(String.format("ℹ️  Info: %d\n", result.info.size()));
        sb.append(String.format("📊 Analysis Items: %d\n\n", result.analysis.size()));

        if (result.getCriticalCount() == 0 && unplacedCourses.isEmpty()) {
            sb.append("✅ VALIDATION PASSED - Schedule is valid!\n\n");
            messages.add("✅ Validation PASSED: No critical issues found");
        } else {
            sb.append("❌ VALIDATION FAILED - Critical issues found!\n\n");
            messages.add("❌ Validation FAILED: " + result.getCriticalCount() + " critical issues");
        }

        if (!result.critical.isEmpty()) {
            sb.append("═══ CRITICAL ISSUES (MUST FIX) ════════════════════════════════════\n");
            for (int i = 0; i < result.critical.size(); i++) {
                sb.append(String.format("%3d. ❌ %s\n", i + 1, result.critical.get(i)));
            }
            sb.append("\n");
        }

        if (!result.warnings.isEmpty()) {
            sb.append("═══ WARNINGS (SHOULD FIX) ═════════════════════════════════════════\n");
            for (int i = 0; i < Math.min(result.warnings.size(), 50); i++) {
                sb.append(String.format("%3d. ⚠️  %s\n", i + 1, result.warnings.get(i)));
            }
            if (result.warnings.size() > 50) {
                sb.append(String.format("     ... and %d more warnings\n", result.warnings.size() - 50));
            }
            sb.append("\n");
        }

        if (!result.info.isEmpty()) {
            sb.append("═══ INFORMATION ═══════════════════════════════════════════════════\n");
            result.info.forEach(info -> sb.append("ℹ️  ").append(info).append("\n"));
            sb.append("\n");
        }

        if (!result.analysis.isEmpty()) {
            sb.append("═══ DETAILED ANALYSIS ═════════════════════════════════════════════\n");
            result.analysis.forEach(analysis -> sb.append(analysis).append("\n"));
            sb.append("\n");
        }

        sb.append("═══ RECOMMENDATIONS ═══════════════════════════════════════════════\n");
        if (result.getCriticalCount() > 0) {
            sb.append("• Fix all critical issues before finalizing the schedule\n");
        }
        if (result.getCriticalCount() == 0 && result.getWarningCount() == 0) {
            sb.append("• Schedule is optimal! Ready to export and distribute\n");
        }

        resultArea.setText(sb.toString());
        resultArea.setScrollTop(0);

        Button exportBtn = new Button("📄 Export Report");
        exportBtn.setStyle(
                "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 8px 16px; -fx-cursor: hand;");
        exportBtn.setOnAction(e -> exportValidationReport(resultStage, sb.toString()));

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 8px 16px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> resultStage.close());

        HBox buttonBox = new HBox(10, exportBtn, closeBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        Label headerLabel = new Label("Validation & Analysis Report");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");

        root.setTop(headerLabel);
        root.setCenter(resultArea);
        root.setBottom(buttonBox);

        Scene scene = new Scene(root, 900, 600);
        ThemeManager.getInstance().registerScene(scene);
        resultStage.setScene(scene);

        resultStage.show();
        if (result.getCriticalCount() == 0 && unplacedCourses.isEmpty()) {
            if (result.getWarningCount() == 0) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.initOwner(resultStage); // Alert'i rapora bağla
                alert.setTitle("Validation Successful");
                alert.setHeaderText("Perfect Schedule!");
                alert.setContentText("No critical issues or warnings found.\nThe schedule is fully optimized.");
                alert.showAndWait();
            }

        }
    }

    private void exportValidationReport(Stage owner, String reportText) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Validation Report");
        chooser.setInitialFileName("validation_report_" + LocalDate.now() + ".txt");
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file)) {
                pw.println(reportText);
                showInfo("Export Success", "Validation report exported to:\n" + file.getAbsolutePath());
                messages.add("✓ Validation report exported: " + file.getName());
            } catch (Exception e) {
                showError("Export Failed", e.getMessage());
            }
        }
    }

    private void showConflictReport() {
        if (dataManager.getSchedule() == null || dataManager.getSchedule().getExams().isEmpty()) {
            showWarning("No Schedule", "Please generate a schedule first.");
            return;
        }

        messages.add("📄 Generating comprehensive conflict report...");

        ValidationResult result = new ValidationResult();
        List<Exam> placedExams = dataManager.getSchedule().getExams().stream()
                .filter(Exam::isScheduled)
                .collect(Collectors.toList());

        Map<Student, Set<TimeSlot>> studentScheduledSlots = new HashMap<>();
        Map<TimeSlot, Set<String>> roomOccupancy = new HashMap<>();

        for (Exam exam : placedExams) {
            TimeSlot currentSlot = exam.getTimeSlot();
            String roomID = exam.getClassroom().getClassroomID();
            String courseCode = exam.getCourse().getCourseCode();
            List<Student> students = exam.getEnrolledStudents();

            if (roomOccupancy.getOrDefault(currentSlot, new HashSet<>()).contains(roomID)) {
                result.addCritical("Room Double-booking: " + courseCode + " conflicts at Day " +
                        currentSlot.getDay() + ", Slot " + currentSlot.getSlotNumber() + " in Room " + roomID);
            }
            roomOccupancy.computeIfAbsent(currentSlot, k -> new HashSet<>()).add(roomID);

            int enrolled = exam.getStudentCount();
            int capacity = exam.getClassroom().getCapacity();
            if (enrolled > capacity) {
                result.addCritical("Capacity Violation: " + courseCode + " has " + enrolled +
                        " students but room " + roomID + " capacity is " + capacity);
            }

            for (Student student : students) {
                Set<TimeSlot> busySlots = studentScheduledSlots.computeIfAbsent(student, k -> new HashSet<>());

                if (busySlots.contains(currentSlot)) {
                    result.addCritical("Student Conflict: " + student.getStudentID() +
                            " has multiple exams at Day " + currentSlot.getDay() + ", Slot "
                            + currentSlot.getSlotNumber());
                }

                if (currentSlot.getSlotNumber() > 1) {
                    TimeSlot previousSlot = new TimeSlot(currentSlot.getDay(), currentSlot.getSlotNumber() - 1);
                    if (busySlots.contains(previousSlot)) {
                        result.addWarning("Consecutive Exam: " + student.getStudentID() +
                                " has " + courseCode + " after another exam on Day " + currentSlot.getDay());
                    }
                }

                busySlots.add(currentSlot);
            }
        }

        if (!unplacedCourses.isEmpty()) {
            for (String course : unplacedCourses) {
                result.addCritical("Unplaced Course: " + course + " could not be scheduled");
            }
        }

        analyzeStudentLoad(result, studentScheduledSlots);
        analyzeRoomUtilization(result, placedExams);
        analyzeTimeDistribution(result, placedExams);

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("📋 Conflict Report");

        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setPrefSize(800, 500);
        reportArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");

        StringBuilder sb = new StringBuilder();

        sb.append("╔══════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                     CONFLICT REPORT                              ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════╝\n\n");

        sb.append("Generated: ").append(LocalDate.now()).append(" ").append(java.time.LocalTime.now()).append("\n\n");

        sb.append("═══ SUMMARY ═══════════════════════════════════════════════════════\n");
        sb.append(String.format("Total Courses: %d\n", dataManager.getCourses().size()));
        sb.append(String.format("Placed Exams: %d\n", placedExams.size()));
        sb.append(String.format("Unplaced Courses: %d\n", unplacedCourses.size()));
        sb.append(String.format("\nCritical Conflicts: %d\n", result.getCriticalCount()));
        sb.append(String.format("Warnings: %d\n\n", result.getWarningCount()));

        if (result.getCriticalCount() == 0 && unplacedCourses.isEmpty()) {
            sb.append("✅ PERFECT SCHEDULE\n");
            sb.append("No critical conflicts or unplaced courses found!\n\n");
        } else {
            sb.append("❌ ISSUES FOUND\n");
            sb.append("Please review and fix the issues below.\n\n");
        }

        if (!unplacedCourses.isEmpty()) {
            sb.append("═══ UNPLACED COURSES (CRITICAL) ═══════════════════════════════════\n");
            for (int i = 0; i < unplacedCourses.size(); i++) {
                sb.append(String.format("%3d. ❌ %s\n", i + 1, unplacedCourses.get(i)));
            }
            sb.append("\nRECOMMENDATIONS:\n");
            sb.append("• Increase exam period (more days)\n");
            sb.append("• Add more classrooms\n");
            sb.append("• Add more time slots per day\n");
            sb.append("• Review student enrollments\n\n");
        }

        List<String> criticalConflicts = result.critical.stream()
                .filter(s -> !s.contains("Unplaced"))
                .collect(Collectors.toList());

        if (!criticalConflicts.isEmpty()) {
            sb.append("═══ CRITICAL CONFLICTS ════════════════════════════════════════════\n");
            for (int i = 0; i < criticalConflicts.size(); i++) {
                sb.append(String.format("%3d. %s\n", i + 1, criticalConflicts.get(i)));
            }
            sb.append("\n");
        }

        if (!result.warnings.isEmpty()) {
            sb.append("═══ WARNINGS ══════════════════════════════════════════════════════\n");
            int displayCount = Math.min(result.warnings.size(), 30);
            for (int i = 0; i < displayCount; i++) {
                sb.append(String.format("%3d. %s\n", i + 1, result.warnings.get(i)));
            }
            if (result.warnings.size() > 30) {
                sb.append(String.format("     ... and %d more warnings\n", result.warnings.size() - 30));
            }
            sb.append("\n");
        }

        if (!result.analysis.isEmpty()) {
            sb.append("═══ DETAILED ANALYSIS ═════════════════════════════════════════════\n");
            result.analysis.forEach(analysis -> sb.append(analysis).append("\n"));
            sb.append("\n");
        }

        sb.append("═══ RECOMMENDATIONS ═══════════════════════════════════════════════\n");
        if (!unplacedCourses.isEmpty()) {
            sb.append("1. PRIORITY: Place unplaced courses\n");
            sb.append("   → Increase exam days from ").append(daysSpinner.getValue()).append(" to ")
                    .append(daysSpinner.getValue() + 2).append(" days\n");
            sb.append("   → Or add more time slots per day\n\n");
        }

        if (result.critical.stream().anyMatch(s -> s.contains("Student Conflict"))) {
            sb.append("2. Fix student conflicts\n");
            sb.append("   → Re-generate schedule with different random seed\n");
            sb.append("   → Increase exam period length\n\n");
        }

        if (result.critical.stream().anyMatch(s -> s.contains("Capacity"))) {
            sb.append("3. Fix capacity violations\n");
            sb.append("   → Assign larger classrooms\n");
            sb.append("   → Split large courses if possible\n\n");
        }

        if (result.warnings.size() > 20) {
            sb.append("4. Address consecutive exam warnings\n");
            sb.append("   → Consider student fatigue\n");
            sb.append("   → Add breaks between exams if possible\n\n");
        }

        if (result.getCriticalCount() == 0 && result.getWarningCount() == 0 && unplacedCourses.isEmpty()) {
            sb.append("• Schedule is optimal and ready for use!\n");
            sb.append("• You can now export and distribute the schedule.\n");
        }

        sb.append("\n═══ END OF REPORT ═════════════════════════════════════════════════\n");

        reportArea.setText(sb.toString());

        Button exportBtn = new Button("📤 Export Report");
        exportBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 8px 16px;");
        exportBtn.setOnAction(e -> exportConflictReportFile(dialogStage, sb.toString()));

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 8px 16px;");
        closeBtn.setOnAction(e -> dialogStage.close());

        HBox buttonBox = new HBox(10, exportBtn, closeBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, new Label("Detailed Conflict Analysis:"), reportArea, buttonBox);
        root.setPadding(new Insets(10));

        Scene dialogScene = new Scene(root, 850, 600);
        ThemeManager.getInstance().registerScene(dialogScene);
        dialogStage.setScene(dialogScene);
        dialogStage.show();

        messages.add("✓ Conflict report generated");
    }

    private void exportConflictReportFile(Stage owner, String reportText) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Conflict Report");
        chooser.setInitialFileName("conflict_report_" + LocalDate.now() + ".txt");
        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file)) {
                pw.println(reportText);
                showInfo("Export Success", "Conflict report exported to:\n" + file.getAbsolutePath());
                messages.add("✓ Conflict report exported to: " + file.getName());
            } catch (Exception e) {
                showError("Export Failed", e.getMessage());
            }
        }
    }

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
        ThemeManager.getInstance().registerScene(scene);
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
                "• Project Lead: [Furkan Pala]\n" +
                "• Backend Developer: [Ahmet Emir Doğan , Abdülhamid Yıldırım]\n" +
                "• Frontend Developer: [Ali Uğur Yalçın , Can Ulaş Akgün]\n" +
                "• QA Tester: [All Team Members]\n\n" +
                "🔧 TECHNOLOGIES USED\n" +
                "• JavaFX for GUI\n" +
                "• Greedy Algorithm for Scheduling\n" +
                "• CSV Data Management\n\n" +
                "📄 LICENSE\n" +
                "• Educational Use License\n" +
                "• Version: 2.0.0\n" +
                "• Release Date: " + LocalDate.now().getYear() + "\n\n" +
                "⭐ FEATURES\n" +
                "✓ Load CSV Data Files\n" +
                "✓ Generate Optimal Schedules\n" +
                "✓ Student Conflict Prevention\n" +
                "✓ Room Capacity Management\n" +

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
                "   - courses.csv: CourseCode,CourseName,MaxCapacity\n" +
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

                "• Consecutive exam avoidance\n\n" +
                "GENERATION STEPS:\n" +
                "1. Sort courses by enrollment (largest first)\n" +
                "2. For each course, find suitable time slot\n" +
                "3. Check all constraints:\n" +
                "   - Student availability\n" +
                "   - Room capacity\n" +

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
                "8. IMPORTING SCHEDULES\n" +
                "--------------------------\n\n" +
                "PURPOSE:\n" +
                "Restore a previously created and exported schedule to continue working on it.\n\n" +
                "⚠ CRITICAL PREREQUISITE:\n" +
                "Before importing, you MUST load the original Base Data files (students, courses, classrooms).\n" +
                "The Import function maps the schedule to these existing data objects.\n\n" +
                "HOW TO IMPORT:\n" +
                "1. Click 'Load Data' and ensure base files are loaded.\n" +
                "2. Click the '📥 Import Schedule' button.\n" +
                "3. Select a 'schedule.csv' file previously created by this app.\n" +
                "4. The system will restore exam times, rooms, and specific student counts.\n\n" +
                "COMMON IMPORT ERRORS:\n" +
                "• \"Course not found\": The loaded courses.csv does not contain a course listed in the schedule.\n" +
                "• \"Room not found\": The loaded classrooms.csv is missing a room ID.\n\n" +
                "--------------------------\n" +
                "9. ADVANCED FEATURES\n" +
                "--------------------------\n\n" +
                "MANUAL EDITING:\n" +
                "• Edit individual exams: Click ✏ button\n" +
                "• Delete exams: Click 🗑 button\n" +
                "• Real-time validation updates\n\n" +
                "CUSTOM CONSTRAINTS:\n" +
                "• Adjust consecutive exam policy\n" +

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
                "    schedules.\n\n" +
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
                "CourseCode,CourseName,MaxCapacity\n" +
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
                "Q27: Are there video tutorials?\n" +
                "A27: Yes! Check our YouTube channel:\n" +
                "     • Basic setup: youtu.be/exam-scheduler-setup\n" +
                "     • Advanced features: youtu.be/exam-scheduler-advanced\n\n" +
                "Q28: Can I request new features?\n" +
                "A28: Absolutely! Send feature requests to:\n" +
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
                "LAST UPDATED: " + LocalDate.now().toString();
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
                "   CourseCode,CourseName,Capacity\n" +
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
        ThemeManager.getInstance().styleAlert(alert);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.getInstance().styleAlert(alert);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.getInstance().styleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Handles returning to the welcome screen with save confirmation.
     * Asks user if they want to save before leaving.
     */
    private void handleReturnToWelcome(Stage stage) {
        // Check if there's any data to potentially save
        boolean hasUnsavedData = dataManager.isDataLoaded() &&
                (dataManager.getSchedule() != null || !exams.isEmpty());

        if (hasUnsavedData) {
            // Create confirmation dialog with three options
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Return to Welcome Screen");
            confirmDialog.setHeaderText("Do you want to save before leaving?");
            confirmDialog.setContentText(
                    "You have unsaved changes. Would you like to save your schedule before returning to the welcome screen?");

            // Custom buttons
            ButtonType saveAndReturn = new ButtonType("💾 Save and Return");
            ButtonType returnWithoutSave = new ButtonType("🚪 Return without Saving");
            ButtonType cancel = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());

            confirmDialog.getButtonTypes().setAll(saveAndReturn, returnWithoutSave, cancel);
            ThemeManager.getInstance().styleAlert(confirmDialog);

            Optional<ButtonType> result = confirmDialog.showAndWait();

            if (result.isPresent()) {
                if (result.get() == saveAndReturn) {
                    // Save first, then return only if save was completed
                    boolean saved = handleSave(stage);
                    if (saved) {
                        // Clear data and show welcome
                        clearCurrentSession();
                        showWelcomeScreen(stage);
                        messages.add("✓ Returned to welcome screen (data saved)");
                    }
                    // If save was cancelled, do nothing
                } else if (result.get() == returnWithoutSave) {
                    // Just return without saving
                    clearCurrentSession();
                    showWelcomeScreen(stage);
                    messages.add("ℹ Returned to welcome screen (changes discarded)");
                }
                // If cancel, do nothing
            }
        } else {
            // No data to save, just return
            clearCurrentSession();
            showWelcomeScreen(stage);
            messages.add("✓ Returned to welcome screen");
        }
    }

    /**
     * Handles the reset action - resets the program in place without navigating
     * away.
     * Shows a confirmation dialog if there's data to save.
     */
    private void handleReset(Stage stage) {
        // Check if there's any data that could be saved
        boolean hasData = dataManager.isDataLoaded() &&
                (dataManager.getSchedule() != null || !exams.isEmpty());

        if (hasData) {
            // Create confirmation dialog with three options
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Reset Program");
            confirmDialog.setHeaderText("Program will be reset");
            confirmDialog.setContentText(
                    "All current data will be cleared. Do you want to save your schedule before resetting?");

            // Custom buttons
            ButtonType saveAndReset = new ButtonType("💾 Save and Reset");
            ButtonType resetWithoutSave = new ButtonType("🔄 Reset without Saving");
            ButtonType cancel = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());

            confirmDialog.getButtonTypes().setAll(saveAndReset, resetWithoutSave, cancel);
            ThemeManager.getInstance().styleAlert(confirmDialog);

            Optional<ButtonType> result = confirmDialog.showAndWait();

            if (result.isPresent()) {
                if (result.get() == saveAndReset) {
                    // Save first, then reset only if save was completed
                    boolean saved = handleSave(stage);
                    if (saved) {
                        // Clear data but stay on main screen
                        clearCurrentSession();
                        messages.add("✓ The system is reset.");
                        messages.add("✓ System is ready. Click 'Load Data' to import CSV files.");
                    }
                    // If save was cancelled, do nothing
                } else if (result.get() == resetWithoutSave) {
                    // Just reset without saving
                    clearCurrentSession();
                    messages.add("✓ The system is reset.");
                    messages.add("✓ System is ready. Click 'Load Data' to import CSV files.");
                }
                // If cancel, do nothing
            }
        } else {
            // No data, just confirm reset
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Reset Program");
            confirmDialog.setHeaderText("Reset Program?");
            confirmDialog.setContentText("Are you sure you want to reset the program?");
            ThemeManager.getInstance().styleAlert(confirmDialog);

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                clearCurrentSession();
                messages.add("✓ The system is reset.");
                messages.add("✓ System is ready. Click 'Load Data' to import CSV files.");
            }
        }
    }

    /**
     * Clears the current session data to prepare for a fresh start.
     * Note: Time slots (tsView) are preserved to maintain user's schedule
     * configuration.
     */
    private void clearCurrentSession() {
        exams.clear();
        messages.clear();
        unplacedCourses.clear();
        if (statsArea != null) {
            statsArea.clear();
        }
        if (crView != null) {
            crView.getItems().clear();
        }
        // Note: tsView (time slots) intentionally not cleared to preserve configuration
        // Reset DataManager
        dataManager.clearAllData();
    }

    public static class ExamEntry {
        private final SimpleStringProperty id, courseId, timeSlot, roomId;
        private final SimpleIntegerProperty day, enrolled;
        private final Exam exam;

        public ExamEntry(Exam exam, String id, String courseId, int day, String timeSlot, String roomId, int enrolled) {
            this.exam = exam;
            this.id = new SimpleStringProperty(id);
            this.courseId = new SimpleStringProperty(courseId);
            this.day = new SimpleIntegerProperty(day);
            this.timeSlot = new SimpleStringProperty(timeSlot);
            this.roomId = new SimpleStringProperty(roomId);
            this.enrolled = new SimpleIntegerProperty(enrolled);
        }

        public Exam getExam() {
            return exam;
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
