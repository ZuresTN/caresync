package com.caresync.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class LoadingController {
    @FXML private Circle ringOuter;
    @FXML private Circle ringInner;
    @FXML private Node loadingContent;

    @FXML
    private void initialize() {
        RotateTransition outer = new RotateTransition(Duration.seconds(1.6), ringOuter);
        outer.setByAngle(360);
        outer.setCycleCount(RotateTransition.INDEFINITE);

        RotateTransition inner = new RotateTransition(Duration.seconds(1.1), ringInner);
        inner.setByAngle(-360);
        inner.setCycleCount(RotateTransition.INDEFINITE);

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(0.9), loadingContent);
        pulse.setFromX(0.98);
        pulse.setFromY(0.98);
        pulse.setToX(1.02);
        pulse.setToY(1.02);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(ScaleTransition.INDEFINITE);

        FadeTransition fade = new FadeTransition(Duration.millis(420), loadingContent);
        fade.setFromValue(0);
        fade.setToValue(1);

        new ParallelTransition(outer, inner, pulse, fade).play();
    }
}
