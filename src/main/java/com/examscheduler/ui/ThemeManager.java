package com.examscheduler.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;


public class ThemeManager {

    private static ThemeManager instance;
    private boolean darkMode = false;
    private final Set<Scene> registeredScenes = new HashSet<>();

    private static final String DARK_THEME_CSS;

    // Dark mode colors
    private static final String DARK_BG = "#1e1e1e";
    private static final String DARK_CARD_BG = "#2d2d2d";
    private static final String DARK_TEXT = "#e0e0e0";

    
    private static final String LIGHT_GRADIENT = "linear-gradient(to bottom right, #ffffff, #f0f4f8)";

    static {
        URL resource = ThemeManager.class.getResource("/com/examscheduler/ui/dark-theme.css");
        if (resource == null) {
            System.err.println("ERROR: Could not find dark-theme.css at /com/examscheduler/ui/dark-theme.css");
            System.err.println("Dark mode will be disabled. Please ensure the CSS file exists in:");
            System.err.println("  src/main/resources/com/examscheduler/ui/dark-theme.css");
            DARK_THEME_CSS = null;
        } else {
            DARK_THEME_CSS = resource.toExternalForm();
            System.out.println("Successfully loaded dark theme CSS from: " + DARK_THEME_CSS);
        }
    }

    private ThemeManager() {
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    
    public void registerScene(Scene scene) {
        if (scene != null) {
            registeredScenes.add(scene);
            applyTheme(scene);
        }
    }

    
     
    public void unregisterScene(Scene scene) {
        registeredScenes.remove(scene);
    }

    
    public void toggleDarkMode() {
        setDarkMode(!darkMode);
    }

   
    public void setDarkMode(boolean enabled) {
        
        if (enabled && DARK_THEME_CSS == null) {
            System.err.println("Cannot enable dark mode: CSS file not found");
            return;
        }
        
        this.darkMode = enabled;
        for (Scene scene : registeredScenes) {
            applyTheme(scene);
        }
    }

   
    public boolean isDarkMode() {
        return darkMode;
    }

    
    private void applyTheme(Scene scene) {
        if (scene == null)
            return;

        if (darkMode && DARK_THEME_CSS != null) {
            if (!scene.getStylesheets().contains(DARK_THEME_CSS)) {
                scene.getStylesheets().add(DARK_THEME_CSS);
            }
            
            applyDarkStyleToNode(scene.getRoot());
        } else {
            if (DARK_THEME_CSS != null) {
                scene.getStylesheets().remove(DARK_THEME_CSS);
            }
            
            removeDarkStyleFromNode(scene.getRoot());
        }
    }
    public void styleNode(Node node) {
        if (darkMode) {
            applyDarkStyleToNode(node);
        } else {
            removeDarkStyleFromNode(node);
        }
    }
    private void applyDarkStyleToNode(Node node) {
        if (node == null)
            return;

        if (node instanceof Region) {
            Region region = (Region) node;
            String currentStyle = region.getStyle();
            
            if (currentStyle != null && !currentStyle.isEmpty()) {
                String newStyle = currentStyle
                        
                        .replaceAll("(?i)-fx-background-color:\\s*#d4edda;?", "-fx-background-color: #1e4620;") // Green
                        .replaceAll("(?i)-fx-background-color:\\s*#d1ecf1;?", "-fx-background-color: #1a3a5e;") // Blue
                        .replaceAll("(?i)-fx-background-color:\\s*#fff3cd;?", "-fx-background-color: #554400;") // Yellow
                       
                        .replaceAll("(?i)-fx-background-color:\\s*linear-gradient[^;]+;?",
                                "-fx-background-color: " + DARK_BG + ";")
                        .replaceAll("(?i)-fx-background-color:\\s*#[c-fC-F][0-9a-fA-F]{5};?",
                                "-fx-background-color: " + DARK_BG + ";")
                        .replaceAll("(?i)-fx-background-color:\\s*white;?", "-fx-background-color: " + DARK_BG + ";")
                        .replaceAll("(?i)-fx-background-color:\\s*#fff[a-fA-F0-9]*;?",
                                "-fx-background-color: " + DARK_BG + ";")
                        .replaceAll("(?i)-fx-background:\\s*white;?", "-fx-background: " + DARK_BG + ";")
                        .replaceAll("(?i)-fx-background:\\s*#[c-fC-F][0-9a-fA-F]{5};?",
                                "-fx-background: " + DARK_BG + ";")
                        .replaceAll("(?i)-fx-text-fill:\\s*#666;?", "-fx-text-fill: " + DARK_TEXT + ";")
                        .replaceAll("(?i)-fx-text-fill:\\s*#999;?", "-fx-text-fill: #888;")
                        .replaceAll("(?i)-fx-text-fill:\\s*#333;?", "-fx-text-fill: " + DARK_TEXT + ";")
                        .replaceAll("(?i)-fx-text-fill:\\s*#2c3e50;?", "-fx-text-fill: " + DARK_TEXT + ";")
                        .replaceAll("(?i)-fx-border-color:\\s*#[eEdD][0-9a-fA-F]{5};?", "-fx-border-color: #555555;");

                if (!newStyle.equals(currentStyle)) {
                    region.setStyle(newStyle);
                }
            }
            if (!region.getStyleClass().contains("dark-mode")) {
                region.getStyleClass().add("dark-mode");
            }
        }

        if (node instanceof ScrollPane) {
            ScrollPane sp = (ScrollPane) node;
            sp.setStyle("-fx-background: " + DARK_BG + "; -fx-background-color: " + DARK_BG + ";");
            
            if (sp.getContent() != null) {
                applyDarkStyleToNode(sp.getContent());
            }
        }
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                
                if (child instanceof javafx.scene.layout.Pane ||
                        child instanceof javafx.scene.layout.VBox ||
                        child instanceof javafx.scene.layout.HBox ||
                        child instanceof javafx.scene.layout.BorderPane ||
                        child instanceof javafx.scene.layout.GridPane ||
                        child instanceof javafx.scene.layout.StackPane ||
                        child instanceof javafx.scene.layout.FlowPane ||
                        child instanceof ScrollPane) {
                    applyDarkStyleToNode(child);
                }
            }
        }
    }

    /**
     * Remove dark styling from a node
     */
    private void removeDarkStyleFromNode(Node node) {
        if (node == null)
            return;

        if (node instanceof Region) {
            Region region = (Region) node;
            region.getStyleClass().remove("dark-mode");
        }
    }

    
    public void styleAlert(Alert alert) {
        if (alert == null || !darkMode || DARK_THEME_CSS == null)
            return;

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(DARK_THEME_CSS);
        dialogPane.getStyleClass().add("dark-dialog");
        dialogPane.setStyle("-fx-background-color: " + DARK_CARD_BG + ";");
    }

   
    public String getBackgroundStyle() {
        return darkMode ? "-fx-background-color: " + DARK_BG + ";" : "-fx-background-color: " + LIGHT_GRADIENT + ";";
    }

    
    public String getCardBackgroundStyle() {
        return darkMode ? "-fx-background-color: " + DARK_CARD_BG + ";" : "-fx-background-color: white;";
    }

    public String getTextStyle() {
        return darkMode ? "-fx-text-fill: " + DARK_TEXT + ";" : "-fx-text-fill: #333;";
    }

    public String getSecondaryTextStyle() {
        return darkMode ? "-fx-text-fill: #888;" : "-fx-text-fill: #666;";
    }
}
