package com.ui;

import com.examscheduler.logic.*;
import com.examscheduler.model.*;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ExamSchedulerApp extends Application {

    private final DataManager dataManager = DataManager.getInstance();
    private final ObservableList<Exam> scheduledExams = FXCollections.observableArrayList();
    private final ObservableList<String> messages = FXCollections.observableArrayList();

    private TableView<Exam> table;
    private final Spinner<Integer> daysSpinner = new Spinner<>(1, 30, 5);
    private ListView<String> crView = new ListView<>();
    private ListView<String> tsView = new ListView<>();

    private Supplier<List<String>> getTimeSlotsFromUI;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("📅 Exam Scheduler - Direct Exam Model");

        String primaryColor = "#0078D4";
        String secondaryColor = "#106EBE";
        String toolBarStyle = "-fx-background-color: " + primaryColor
                + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);";
        String buttonStyle = "-fx-background-color: " + secondaryColor
                + "; -fx-text-fill: white; -fx-background-radius: 3; -fx-padding: 8px 16px; -fx-font-weight: bold; -fx-cursor: hand;";
        String titleStyle = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + primaryColor + ";";

        MenuBar menuBar = createMenuBar(stage);
        ToolBar toolBar = createToolBar(stage, buttonStyle, toolBarStyle);

        VBox leftPane = createConfigurationPanel(buttonStyle);

        table = createExamTable(stage);
        Label scheduleTitle = new Label("📋 Exam Schedule");
        scheduleTitle.setStyle(titleStyle);
        VBox centerPane = new VBox(10, scheduleTitle, table);
        centerPane.setPadding(new Insets(15));
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox rightPane = createMessagesPanel(titleStyle);

        BorderPane root = new BorderPane(centerPane, new VBox(menuBar, toolBar), rightPane, null, leftPane);

        messages.add("✓ System is ready. Using direct Exam model.");
        simulateInitialDataLoad();

        Scene sc = new Scene(root, 1400, 800);
        stage.setScene(sc);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.show();
    }

    private void simulateInitialDataLoad() {
        Course c101 = new Course("C101");
        Course m202 = new Course("M202");
        Classroom r101 = new Classroom("R101", 50);
        Classroom r202 = new Classroom("R202", 100);
        TimeSlot ts1 = new TimeSlot(1, 1);
        TimeSlot ts2 = new TimeSlot(2, 2);

        dataManager.setStudents(List.of(new Student("S001")));
        dataManager.setCourses(List.of(c101, m202));
        dataManager.setClassrooms(List.of(r101, r202));

        updateClassroomsView();

        Exam e1 = new Exam(c101);
        e1.setTimeSlot(ts1);
        e1.setClassroom(r101);
        Exam e2 = new Exam(m202);
        e2.setTimeSlot(ts2);
        e2.setClassroom(r202);

        scheduledExams.addAll(e1, e2);
        messages.add("Mock data loaded and UI updated.");
    }

    private TableView<Exam> createExamTable(Stage stage) {
        TableView<Exam> tableView = new TableView<>();
        tableView.setItems(scheduledExams);
        tableView.setPlaceholder(new Label("No exam schedule generated yet. Click 'Generate Schedule'."));

        TableColumn<Exam, String> colCourse = new TableColumn<>("Course Code");
        colCourse.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getCourse().getCourseCode()));
        colCourse.setPrefWidth(120);

        TableColumn<Exam, Integer> colDay = new TableColumn<>("Day");
        colDay.setCellValueFactory(
                cellData -> new SimpleIntegerProperty(cellData.getValue().getTimeSlot().getDay()).asObject());
        colDay.setPrefWidth(60);

        TableColumn<Exam, String> colTime = new TableColumn<>("Time Slot");
        colTime.setCellValueFactory(cellData -> {
            TimeSlot ts = cellData.getValue().getTimeSlot();
            return new SimpleStringProperty("Day " + ts.getDay() + " Slot " + ts.getSlotNumber());
        });
        colTime.setPrefWidth(140);

        TableColumn<Exam, String> colRoom = new TableColumn<>("Classroom");
        colRoom.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getClassroom().getClassroomID()));
        colRoom.setPrefWidth(100);

        TableColumn<Exam, Integer> colEnrolled = new TableColumn<>("Students");
        colEnrolled.setCellValueFactory(
                cellData -> new SimpleIntegerProperty(cellData.getValue().getStudentCount()).asObject());
        colEnrolled.setPrefWidth(80);

        TableColumn<Exam, Void> colAct = new TableColumn<>("Actions");
        colAct.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏ Edit");
            private final Button deleteBtn = new Button("🗑");

            {
                String editStyle = "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 3px 8px; -fx-font-size: 10px; -fx-background-radius: 3; -fx-cursor: hand;";
                String deleteStyle = "-fx-background-color: #F44336; -fx-text-fill: white; -fx-padding: 3px 8px; -fx-font-size: 10px; -fx-background-radius: 3; -fx-cursor: hand;";

                editBtn.setStyle(editStyle);
                deleteBtn.setStyle(deleteStyle);

                editBtn.setOnAction(ev -> {
                    Exam e = getTableView().getItems().get(getIndex());
                    editExam(stage, e);
                });

                deleteBtn.setOnAction(ev -> {
                    Exam e = getTableView().getItems().get(getIndex());
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

        tableView.getColumns().addAll(colCourse, colDay, colTime, colRoom, colEnrolled, colAct);
        return tableView;
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
        exportItem.setOnAction(e -> messages.add("Menu: Export Clicked (Simulated)"));
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(loadItem, saveItem, exportItem, new SeparatorMenuItem(), exitItem);

        Menu editMenu = new Menu("Edit");
        MenuItem manageCourses = new MenuItem("Manage Courses...");
        MenuItem manageStudents = new MenuItem("Manage Students...");
        MenuItem manageClassrooms = new MenuItem("Manage Classrooms...");

        manageCourses.setOnAction(e -> showMessage("Menu: Manage Courses Clicked (Simulated)"));
        manageStudents.setOnAction(e -> showMessage("Menu: Manage Students Clicked (Simulated)"));
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

        Menu helpMenu = new Menu("Help");
        MenuItem about = new MenuItem("About");
        about.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(about);

        menuBar.getMenus().addAll(fileMenu, editMenu, scheduleMenu, helpMenu);
        return menuBar;
    }

    private ToolBar createToolBar(Stage stage, String buttonStyle, String toolBarStyle) {
        ToolBar toolBar = new ToolBar();
        Button loadBtn = new Button("📁 Load Data");
        Button generateBtn = new Button("⚡ Generate Schedule");
        Button saveBtn = new Button("💾 Save Schedule");
        Button validateBtn = new Button("✓ Validate");
        Button exportBtn = new Button("📤 Export");

        loadBtn.setStyle(buttonStyle);
        generateBtn.setStyle(buttonStyle);
        saveBtn.setStyle(buttonStyle);
        validateBtn.setStyle(buttonStyle);
        exportBtn.setStyle(buttonStyle);
        toolBar.setStyle(toolBarStyle);

        toolBar.getItems().addAll(loadBtn, new Separator(), generateBtn, validateBtn, new Separator(), saveBtn,
                exportBtn);

        loadBtn.setOnAction(e -> handleLoad(stage));
        generateBtn.setOnAction(e -> handleGenerateSchedule());
        saveBtn.setOnAction(e -> handleSave(stage));
        validateBtn.setOnAction(e -> handleValidate());
        exportBtn.setOnAction(e -> messages.add("ToolBar: Export Clicked (Simulated)"));

        return toolBar;
    }

    private VBox createConfigurationPanel(String buttonStyle) {
        VBox leftPane = new VBox(15);
        leftPane.setPadding(new Insets(15));
        leftPane.setPrefWidth(320);
        leftPane.setStyle("-fx-background-color: #F3F3F3; -fx-border-color: #D0D0D0; -fx-border-width: 0 1px 0 0;");

        Label configTitle = new Label("⚙ Configuration");
        configTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0078D4;");

        HBox daysRow = new HBox(10, new Label("Exam Period (Days):"), daysSpinner);
        daysRow.setAlignment(Pos.CENTER_LEFT);
        daysSpinner.setPrefWidth(80);
        daysSpinner.setEditable(true);

        Label tsLabel = new Label("⏰ Time Slots (Per Day):");
        tsLabel.setStyle("-fx-font-weight: bold;");
        tsView.setEditable(true);
        tsView.setCellFactory(TextFieldListCell.forListView());
        tsView.getItems().addAll("09:00-11:00", "12:00-14:00", "15:00-17:00");
        tsView.setPrefHeight(140);

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
        crView.setPrefHeight(180);

        Button manageClassroomsBtn = new Button("Manage Classrooms");
        manageClassroomsBtn.setStyle(buttonStyle + "-fx-font-size: 11px;");
        manageClassroomsBtn.setOnAction(e -> showManageClassrooms((Stage) crView.getScene().getWindow()));

        getTimeSlotsFromUI = () -> new ArrayList<>(tsView.getItems());

        leftPane.getChildren().addAll(
                configTitle,
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

        TextArea statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setPrefHeight(150);
        statsArea.setText(dataManager.getDetailedStats());

        VBox rightPane = new VBox(10, msgTitle, msgView, statsTitle, statsArea);
        rightPane.setPadding(new Insets(15));
        rightPane.setPrefWidth(380);
        rightPane.setStyle("-fx-background-color: #F3F3F3; -fx-border-color: #D0D0D0; -fx-border-width: 0 0 0 1px;");

        return rightPane;
    }

    private void handleLoad(Stage owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder with CSV files (Simulated Load)");
        File dir = chooser.showDialog(owner);
        if (dir == null) {
            messages.add("⚠ Load cancelled.");
            return;
        }

        messages.add("📂 DataManager.clearAllData() called.");
        messages.add("📂 CSV Parser simulation started: " + dir.getName() + "...");
        dataManager.clearAllData();

        try {
            dataManager.setStudents(List.of(new Student("Ali"), new Student("Can")));
            dataManager.setCourses(List.of(new Course("CE315"), new Course("CE323")));
            dataManager.setClassrooms(List.of(new Classroom("M101", 50), new Classroom("M202", 100)));

            updateClassroomsView();

            messages.add("✓ Data loaded successfully (Simulated):");
            messages.add("  • Students: " + dataManager.getStudents().size());
            messages.add("  • Courses: " + dataManager.getCourses().size());
            messages.add("  • Classrooms: " + dataManager.getClassrooms().size());
            messages.add("📍 Ready to generate schedule.");

        } catch (Exception e) {
            messages.add("❌ File I/O Error (Simulated): " + e.getMessage());
            dataManager.clearAllData();
        }
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

        messages.add("⚡ Starting automatic schedule generation (Simulated)...");

        int days = daysSpinner.getValue();
        List<String> timeSlotLabels = getTimeSlotsFromUI.get();
        dataManager.setSchedule(new Schedule(days, timeSlotLabels.size()));

        scheduledExams.clear();

        Course c1 = dataManager.getCourses().get(0);
        Course c2 = dataManager.getCourses().get(1);
        Classroom r1 = dataManager.getClassrooms().get(0);
        Classroom r2 = dataManager.getClassrooms().get(1);

        Exam e1 = new Exam(c1);
        e1.setTimeSlot(new TimeSlot(1, 1));
        e1.setClassroom(r1);
        Exam e2 = new Exam(c2);
        e2.setTimeSlot(new TimeSlot(1, 2));
        e2.setClassroom(r2);

        scheduledExams.addAll(e1, e2);

        messages.add("✓ Schedule generation completed! (Simulated)");
        messages.add("📊 Results: " + scheduledExams.size() + " exams placed (Simulated)");
        table.refresh();
    }

    private void handleValidate() {
        if (dataManager.getSchedule() == null) {
            showWarning("No Schedule", "No schedule to validate. Generate a schedule first.");
            return;
        }

        messages.add("🔍 Schedule validation (Simulated)...");

        messages.add("✓ Validation passed: No conflicts found! (Simulated)");
        showInfo("Validation Success", "Schedule is valid with no conflicts.");
    }

    private void handleSave(Stage owner) {
        if (dataManager.getSchedule() == null) {
            showWarning("No Schedule", "No schedule to save. Generate a schedule first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Schedule to File (Simulated Save)");
        chooser.setInitialFileName("exam_schedule.dat");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Data Files", "*.dat"));
        File f = chooser.showSaveDialog(owner);
        if (f == null) {
            messages.add("⚠ Save cancelled.");
            return;
        }

        try {
            messages.add("✓ DataManager.saveToFile() called.");
            messages.add("✓ Schedule saved to: " + f.getName());
            showInfo("Save Success", "Schedule saved successfully (Simulated)");
        } catch (Exception e) {
            messages.add("❌ Save error: " + e.getMessage());
            showError("Save Failed", "Could not save file (Simulated)");
        }
    }

    private void showManageClassrooms(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Manage Classrooms");

        ObservableList<Classroom> observableClassrooms = FXCollections.observableArrayList(dataManager.getClassrooms());
        ListView<Classroom> classroomList = new ListView<>(observableClassrooms);

        TextField idField = new TextField();
        idField.setPromptText("Classroom ID (e.g., M303)");

        Spinner<Integer> capacitySpinner = new Spinner<>(1, 500, 40);
        capacitySpinner.setEditable(true);

        Button addBtn = new Button("➕ Add");
        Button removeBtn = new Button("➖ Remove");
        Button closeBtn = new Button("Close");

        addBtn.setOnAction(e -> {
            String id = idField.getText().trim();
            int cap = capacitySpinner.getValue();
            if (!id.isEmpty()) {
                Classroom newRoom = new Classroom(id, cap);
                dataManager.addClassroom(newRoom);
                observableClassrooms.add(newRoom);
                updateClassroomsView();
                idField.clear();
                messages.add("✓ Classroom added: " + id);
            }
        });

        removeBtn.setOnAction(e -> {
            Classroom selected = classroomList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                dataManager.removeClassroom(selected);
                observableClassrooms.remove(selected);
                updateClassroomsView();
                messages.add("✓ Classroom removed: " + selected.getClassroomID());
            }
        });

        closeBtn.setOnAction(e -> dialog.close());

        VBox layout = new VBox(10,
                new Label("Classrooms:"),
                classroomList,
                new HBox(10, new Label("ID:"), idField),
                new HBox(10, new Label("Capacity:"), capacitySpinner),
                new HBox(10, addBtn, removeBtn, closeBtn));
        layout.setPadding(new Insets(15));

        dialog.setScene(new Scene(layout, 400, 450));
        dialog.show();
    }

    private void editExam(Stage owner, Exam e) {
        showInfo("Edit Exam",
                "Editing exam " + e.getCourse().getCourseCode() + " is simulated. UI elements would appear here.");
        messages.add("✏ Exam " + e.getCourse().getCourseCode() + " edit dialog opened (Simulated).");
    }

    private void deleteExam(Exam e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Exam");
        confirm.setHeaderText("Delete exam: " + e.getCourse().getCourseCode() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            scheduledExams.remove(e);
            table.refresh();
            messages.add("🗑 Exam deleted: " + e.getCourse().getCourseCode());
        }
    }

    private void showConflictReport() {
        showInfo("Conflict Report", "Conflict Report generation is simulated.");
        messages.add("📢 Conflict Report generated (Simulated).");
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Exam Scheduler");
        alert.setHeaderText("Exam Scheduler - Windows Desktop Application");
        alert.setContentText(" SE302 Software Engineering Project");
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showMessage(String message) {
        messages.add("💡 " + message);
    }
}
