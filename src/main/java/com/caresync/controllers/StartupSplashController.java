package com.caresync.controllers;

import com.caresync.Main;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class StartupSplashController {
    @FXML private VBox splashContent;
    @FXML private Circle splashRingOuter;
    @FXML private Circle splashRingMiddle;
    @FXML private ImageView splashLogo;
    @FXML private Node splashCard;
    @FXML private ProgressBar splashProgress;
    @FXML private Label splashStatusLabel;

    @FXML
    private void initialize() {
        playIntro();
        playProgress();
        PauseTransition delay = new PauseTransition(Duration.seconds(2.8));
        delay.setOnFinished(event -> openLogin());
        delay.play();
    }

    private void playIntro() {
        RotateTransition outer = rotate(splashRingOuter, 360, 1.8);
        RotateTransition middle = rotate(splashRingMiddle, -360, 1.25);
        RotateTransition logo = rotate(splashLogo, 360, 2.6);

        splashContent.setOpacity(0);
        splashContent.setTranslateY(18);
        splashContent.setScaleX(0.96);
        splashContent.setScaleY(0.96);

        FadeTransition fade = new FadeTransition(Duration.millis(520), splashContent);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(620), splashContent);
        slide.setFromY(18);
        slide.setToY(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(620), splashContent);
        scale.setFromX(0.96);
        scale.setFromY(0.96);
        scale.setToX(1);
        scale.setToY(1);

        ScaleTransition cardPulse = new ScaleTransition(Duration.seconds(0.9), splashCard);
        cardPulse.setFromX(1);
        cardPulse.setFromY(1);
        cardPulse.setToX(1.025);
        cardPulse.setToY(1.025);
        cardPulse.setAutoReverse(true);
        cardPulse.setCycleCount(ScaleTransition.INDEFINITE);

        new ParallelTransition(outer, middle, logo, fade, slide, scale, cardPulse).play();
    }

    private void playProgress() {
        splashProgress.setProgress(0);
        Timeline progress = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(splashProgress.progressProperty(), 0, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(700), event -> splashStatusLabel.setText("Loading secure modules..."),
                        new KeyValue(splashProgress.progressProperty(), 0.38, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(1500), event -> splashStatusLabel.setText("Checking clinic database settings..."),
                        new KeyValue(splashProgress.progressProperty(), 0.72, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(2400), event -> splashStatusLabel.setText("Opening CareSync login..."),
                        new KeyValue(splashProgress.progressProperty(), 1.0, Interpolator.EASE_OUT))
        );
        progress.play();
    }

    private RotateTransition rotate(Node node, double angle, double seconds) {
        RotateTransition transition = new RotateTransition(Duration.seconds(seconds), node);
        transition.setByAngle(angle);
        transition.setCycleCount(RotateTransition.INDEFINITE);
        transition.setInterpolator(Interpolator.LINEAR);
        return transition;
    }

    private void openLogin() {
        if (Main.getPrimaryStage() == null) {
            return;
        }
        try {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(360), splashContent);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(event -> {
                try {
                    Main.setRoot("login", 1080, 720);
                } catch (Exception ex) {
                    splashStatusLabel.setText("Could not open login: " + ex.getMessage());
                }
            });
            fadeOut.play();
        } catch (Exception ex) {
            splashStatusLabel.setText("Startup animation failed: " + ex.getMessage());
        }
    }
}
