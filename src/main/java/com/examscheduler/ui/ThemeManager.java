package com.examscheduler.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

import java.util.HashSet;
import java.util.Set;

/**
 * ThemeManager - Singleton Pattern
 * Manages dark/light theme for the entire application.
 * All scenes should be registered with this manager.
 */
public class ThemeManager {

    private static ThemeManager instance;
    private boolean darkMode = false;
    private final Set<Scene> registeredScenes = new HashSet<>();

    private static final String DARK_THEME_CSS = ThemeManager.class.getResource("/com/examscheduler/ui/dark-theme.css")
            .toExternalForm();

    // Dark mode colors
    private static final String DARK_BG = "#1e1e1e";
    private static final String DARK_CARD_BG = "#2d2d2d";
    private static final String DARK_TEXT = "#e0e0e0";

    // Light mode colors (original)
    private static final String LIGHT_GRADIENT = "linear-gradient(to bottom right, #ffffff, #f0f4f8)";

    private ThemeManager() {
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /**
     * Register a scene to be managed by ThemeManager.
     * Automatically applies current theme.
     */
    public void registerScene(Scene scene) {
        if (scene != null) {
            registeredScenes.add(scene);
            applyTheme(scene);
        }
    }

    /**
     * Unregister a scene (call when window is closed).
     */
    public void unregisterScene(Scene scene) {
        registeredScenes.remove(scene);
    }

    /**
     * Toggle between dark and light mode.
     */
    public void toggleDarkMode() {
        setDarkMode(!darkMode);
    }

    /**
     * Set dark mode on or off.
     */
    public void setDarkMode(boolean enabled) {
        this.darkMode = enabled;
        // Apply to all registered scenes
        for (Scene scene : registeredScenes) {
            applyTheme(scene);
        }
    }

    /**
     * Check if dark mode is currently enabled.
     */
    public boolean isDarkMode() {
        return darkMode;
    }

    /**
     * Apply current theme to a specific scene.
     */
    private void applyTheme(Scene scene) {
        if (scene == null)
            return;

        if (darkMode) {
            if (!scene.getStylesheets().contains(DARK_THEME_CSS)) {
                scene.getStylesheets().add(DARK_THEME_CSS);
            }
            // Apply dark styling to root
            applyDarkStyleToNode(scene.getRoot());
        } else {
            scene.getStylesheets().remove(DARK_THEME_CSS);
            // Remove dark styling
            removeDarkStyleFromNode(scene.getRoot());
        }
    }

    /**
     * Public method to manually apply theme to a node (useful for dynamic content).
     */
    public void styleNode(Node node) {
        if (darkMode) {
            applyDarkStyleToNode(node);
        } else {
            removeDarkStyleFromNode(node);
        }
    }

    /**
     * Apply dark background to a node, overriding inline styles
     */
    private void applyDarkStyleToNode(Node node) {
        if (node == null)
            return;

        if (node instanceof Region) {
            Region region = (Region) node;
            String currentStyle = region.getStyle();

            // Replace white/light backgrounds with dark
            if (currentStyle != null && !currentStyle.isEmpty()) {
                String newStyle = currentStyle
                        // Semantic Color Mappings (Calendar Cells)
                        .replaceAll("(?i)-fx-background-color:\\s*#d4edda;?", "-fx-background-color: #1e4620;") // Green
                        .replaceAll("(?i)-fx-background-color:\\s*#d1ecf1;?", "-fx-background-color: #1a3a5e;") // Blue
                        .replaceAll("(?i)-fx-background-color:\\s*#fff3cd;?", "-fx-background-color: #554400;") // Yellow
                        // Generic Light Backgrounds
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

            // Add dark-mode class if not present
            if (!region.getStyleClass().contains("dark-mode")) {
                region.getStyleClass().add("dark-mode");
            }
        }

        if (node instanceof ScrollPane) {
            ScrollPane sp = (ScrollPane) node;
            sp.setStyle("-fx-background: " + DARK_BG + "; -fx-background-color: " + DARK_BG + ";");
            // Also traverse ScrollPane content
            if (sp.getContent() != null) {
                applyDarkStyleToNode(sp.getContent());
            }
        }

        // Recursively apply to ALL children
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                // Apply to all Region types (VBox, HBox, etc.) but skip leaf controls like
                // Button, Label
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

        // Note: We cannot easily restore original inline styles after they've been
        // replaced.
        // The app will need to be restarted for full light mode restoration.
    }

    /**
     * Style an Alert dialog with the current theme.
     * Call this before showing the alert.
     */
    public void styleAlert(Alert alert) {
        if (alert == null)
            return;

        DialogPane dialogPane = alert.getDialogPane();
        if (darkMode) {
            dialogPane.getStylesheets().add(DARK_THEME_CSS);
            dialogPane.getStyleClass().add("dark-dialog");
            dialogPane.setStyle("-fx-background-color: " + DARK_CARD_BG + ";");
        }
    }

    /**
     * Get appropriate background style for containers based on current theme.
     */
    public String getBackgroundStyle() {
        return darkMode ? "-fx-background-color: " + DARK_BG + ";" : "-fx-background-color: " + LIGHT_GRADIENT + ";";
    }

    /**
     * Get appropriate card/panel background style.
     */
    public String getCardBackgroundStyle() {
        return darkMode ? "-fx-background-color: " + DARK_CARD_BG + ";" : "-fx-background-color: white;";
    }

    /**
     * Get appropriate text color for labels.
     */
    public String getTextStyle() {
        return darkMode ? "-fx-text-fill: " + DARK_TEXT + ";" : "-fx-text-fill: #333;";
    }

    /**
     * Get appropriate secondary text color.
     */
    public String getSecondaryTextStyle() {
        return darkMode ? "-fx-text-fill: #888;" : "-fx-text-fill: #666;";
    }
}
