package com.examscheduler;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ExamSchedulerApp extends Application {
    //sample
    @Override
    public void start(Stage stage) {
        stage.setTitle("Exam Scheduler");

        Label label = new Label("Exam Scheduler Started");
        Scene scene = new Scene(new StackPane(label), 640, 480);
        
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}