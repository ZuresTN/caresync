package com.caresync.services;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public final class ToastService {
    private ToastService() {
    }

    public static void show(StackPane root, String message, boolean success) {
        if (root == null || message == null || message.isBlank()) {
            return;
        }
        Label toast = new Label(message);
        toast.getStyleClass().addAll("toast", success ? "toast-success" : "toast-error");
        StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_CENTER);
        root.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(160), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.seconds(3));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> root.getChildren().remove(toast));
        fadeIn.setOnFinished(event -> hold.play());
        hold.setOnFinished(event -> fadeOut.play());
        fadeIn.play();
    }
}
