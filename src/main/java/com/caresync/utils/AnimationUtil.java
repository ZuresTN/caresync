package com.caresync.utils;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public final class AnimationUtil {
    private static final Interpolator SMOOTH = Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0);

    private AnimationUtil() {
    }

    public static ParallelTransition pageEnter(Node page) {
        page.setOpacity(0);
        page.setTranslateY(28);
        page.setScaleX(0.982);
        page.setScaleY(0.982);

        FadeTransition fade = new FadeTransition(Duration.millis(420), page);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(SMOOTH);

        TranslateTransition slide = new TranslateTransition(Duration.millis(520), page);
        slide.setFromY(28);
        slide.setToY(0);
        slide.setInterpolator(SMOOTH);

        ScaleTransition scale = new ScaleTransition(Duration.millis(520), page);
        scale.setFromX(0.982);
        scale.setFromY(0.982);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(SMOOTH);

        return new ParallelTransition(fade, slide, scale);
    }

    public static ParallelTransition pageExit(Node page) {
        FadeTransition fade = new FadeTransition(Duration.millis(160), page);
        fade.setFromValue(page.getOpacity());
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition slide = new TranslateTransition(Duration.millis(180), page);
        slide.setFromY(page.getTranslateY());
        slide.setToY(-10);
        slide.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition scale = new ScaleTransition(Duration.millis(180), page);
        scale.setFromX(page.getScaleX());
        scale.setFromY(page.getScaleY());
        scale.setToX(0.992);
        scale.setToY(0.992);
        scale.setInterpolator(Interpolator.EASE_IN);

        return new ParallelTransition(fade, slide, scale);
    }

    public static void staggerIn(List<? extends Node> nodes) {
        SequentialTransition sequence = new SequentialTransition();
        for (Node node : nodes) {
            if (!node.isManaged()) {
                continue;
            }
            node.setOpacity(0);
            node.setTranslateX(-18);

            FadeTransition fade = new FadeTransition(Duration.millis(240), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(SMOOTH);

            TranslateTransition slide = new TranslateTransition(Duration.millis(300), node);
            slide.setFromX(-18);
            slide.setToX(0);
            slide.setInterpolator(SMOOTH);

            ParallelTransition item = new ParallelTransition(fade, slide);
            sequence.getChildren().add(item);
        }
        sequence.play();
    }

    public static void animateInteractiveControls(Parent root) {
        for (Node node : root.lookupAll(".button")) {
            if (node instanceof ButtonBase) {
                installButtonMotion(node);
            }
        }
        for (Node node : root.lookupAll(".text-field")) {
            installFocusMotion(node);
        }
        for (Node node : root.lookupAll(".password-field")) {
            installFocusMotion(node);
        }
        for (Node node : root.lookupAll(".text-area")) {
            installFocusMotion(node);
        }
        for (Node node : root.lookupAll(".combo-box")) {
            installFocusMotion(node);
        }
        for (Node node : root.lookupAll(".choice-box")) {
            installFocusMotion(node);
        }
        for (Node node : root.lookupAll(".date-picker")) {
            installFocusMotion(node);
        }
        for (Node node : root.lookupAll(".stat-card")) {
            installLiftMotion(node, 1.025, -6);
        }
        for (Node node : root.lookupAll(".panel")) {
            installLiftMotion(node, 1.008, -3);
        }
        for (Node node : root.lookupAll(".panel-quiet")) {
            installLiftMotion(node, 1.006, -2);
        }
        for (Node node : root.lookupAll(".sidebar-profile")) {
            installLiftMotion(node, 1.006, -2);
        }
        for (Node node : root.lookupAll(".brand-feature-row")) {
            installLiftMotion(node, 1.012, -3);
        }
        for (Node node : root.lookupAll(".login-help-strip")) {
            installLiftMotion(node, 1.006, -2);
        }
        for (Node node : root.lookupAll(".table-view")) {
            if (node instanceof TableView<?>) {
                softReveal(node);
            }
        }
        for (Node node : root.lookupAll(".calendar-cell")) {
            installLiftMotion(node, 1.012, -3);
        }
        for (Node node : root.lookupAll(".calendar-badge")) {
            installLiftMotion(node, 1.025, -2);
        }
    }

    public static void revealPageChildren(Parent root) {
        List<Node> nodes = new ArrayList<>();
        nodes.addAll(root.lookupAll(".stat-card"));
        nodes.addAll(root.lookupAll(".panel"));
        nodes.addAll(root.lookupAll(".panel-quiet"));
        nodes.addAll(root.lookupAll(".table-view"));
        revealInStagger(nodes);
    }

    public static void softReveal(Node node) {
        node.setOpacity(0);
        node.setTranslateY(10);
        FadeTransition fade = new FadeTransition(Duration.millis(330), node);
        fade.setToValue(1);
        fade.setInterpolator(SMOOTH);
        TranslateTransition slide = new TranslateTransition(Duration.millis(330), node);
        slide.setToY(0);
        slide.setInterpolator(SMOOTH);
        new ParallelTransition(fade, slide).play();
    }

    public static void revealInSequence(Node... nodes) {
        SequentialTransition sequence = new SequentialTransition();
        for (Node node : nodes) {
            if (node == null || !node.isManaged()) {
                continue;
            }
            node.setOpacity(0);
            node.setTranslateY(18);
            node.setScaleX(0.985);
            node.setScaleY(0.985);

            FadeTransition fade = new FadeTransition(Duration.millis(320), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(SMOOTH);

            TranslateTransition slide = new TranslateTransition(Duration.millis(380), node);
            slide.setFromY(18);
            slide.setToY(0);
            slide.setInterpolator(SMOOTH);

            ScaleTransition scale = new ScaleTransition(Duration.millis(380), node);
            scale.setFromX(0.985);
            scale.setFromY(0.985);
            scale.setToX(1);
            scale.setToY(1);
            scale.setInterpolator(SMOOTH);

            sequence.getChildren().add(new ParallelTransition(fade, slide, scale));
        }
        sequence.play();
    }

    public static void revealInStagger(List<? extends Node> nodes) {
        ParallelTransition all = new ParallelTransition();
        int index = 0;
        for (Node node : nodes) {
            if (node == null || !node.isManaged()) {
                continue;
            }
            node.setOpacity(0);
            node.setTranslateY(14);
            node.setScaleX(0.99);
            node.setScaleY(0.99);

            FadeTransition fade = new FadeTransition(Duration.millis(220), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(SMOOTH);

            TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
            slide.setFromY(14);
            slide.setToY(0);
            slide.setInterpolator(SMOOTH);

            ScaleTransition scale = new ScaleTransition(Duration.millis(260), node);
            scale.setFromX(0.99);
            scale.setFromY(0.99);
            scale.setToX(1);
            scale.setToY(1);
            scale.setInterpolator(SMOOTH);

            ParallelTransition item = new ParallelTransition(fade, slide, scale);
            item.setDelay(Duration.millis(Math.min(index, 18) * 24L));
            all.getChildren().add(item);
            index++;
        }
        all.play();
    }

    public static void revealFastGrid(List<? extends Node> nodes) {
        ParallelTransition all = new ParallelTransition();
        int index = 0;
        for (Node node : nodes) {
            if (node == null || !node.isManaged()) {
                continue;
            }
            node.setOpacity(0);
            node.setTranslateY(8);
            node.setScaleX(0.992);
            node.setScaleY(0.992);

            FadeTransition fade = new FadeTransition(Duration.millis(120), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(SMOOTH);

            TranslateTransition slide = new TranslateTransition(Duration.millis(150), node);
            slide.setFromY(8);
            slide.setToY(0);
            slide.setInterpolator(SMOOTH);

            ScaleTransition scale = new ScaleTransition(Duration.millis(150), node);
            scale.setFromX(0.992);
            scale.setFromY(0.992);
            scale.setToX(1);
            scale.setToY(1);
            scale.setInterpolator(SMOOTH);

            ParallelTransition item = new ParallelTransition(fade, slide, scale);
            item.setDelay(Duration.millis(Math.min(index, 24) * 8L));
            all.getChildren().add(item);
            index++;
        }
        all.play();
    }

    public static void animateProgress(ProgressBar progressBar, double target) {
        if (progressBar == null) {
            return;
        }
        double clamped = Math.min(1, Math.max(0, target));
        progressBar.setProgress(0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0)),
                new KeyFrame(Duration.millis(920), new KeyValue(progressBar.progressProperty(), clamped, SMOOTH))
        );
        timeline.play();
    }

    public static void countTo(Label label, int target) {
        IntegerProperty value = new SimpleIntegerProperty(0);
        label.textProperty().bind(value.asString());
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(value, 0)),
                new KeyFrame(Duration.millis(780), event -> label.textProperty().unbind(),
                        new KeyValue(value, Math.max(target, 0), SMOOTH))
        );
        timeline.play();
    }

    public static List<Node> visibleNavNodes(Node... nodes) {
        List<Node> visible = new ArrayList<>();
        for (Node node : nodes) {
            if (node != null && node.isManaged()) {
                visible.add(node);
            }
        }
        return visible;
    }

    private static void installButtonMotion(Node node) {
        if (Boolean.TRUE.equals(node.getProperties().get("motion-installed"))) {
            return;
        }
        node.getProperties().put("motion-installed", true);

        node.setOnMouseEntered(event -> playScale(node, 1.035, 120));
        node.setOnMouseExited(event -> playScale(node, 1.0, 140));
        node.setOnMousePressed(event -> playScale(node, 0.975, 70));
        node.setOnMouseReleased(event -> playScale(node, node.isHover() ? 1.035 : 1.0, 110));
    }

    private static void installFocusMotion(Node node) {
        if (!(node instanceof Control) || Boolean.TRUE.equals(node.getProperties().get("focus-motion-installed"))) {
            return;
        }
        node.getProperties().put("focus-motion-installed", true);
        node.focusedProperty().addListener((obs, old, focused) -> playScale(node, focused ? 1.012 : 1.0, focused ? 160 : 140));
    }

    private static void installLiftMotion(Node node, double hoverScale, double hoverY) {
        if (Boolean.TRUE.equals(node.getProperties().get("lift-installed"))) {
            return;
        }
        node.getProperties().put("lift-installed", true);

        node.setOnMouseEntered(event -> {
            playScale(node, hoverScale, 180);
            playTranslateY(node, hoverY, 180);
        });
        node.setOnMouseExited(event -> {
            playScale(node, 1.0, 200);
            playTranslateY(node, 0, 200);
        });
    }

    private static void playScale(Node node, double scale, int millis) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(millis), node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.setInterpolator(SMOOTH);
        transition.play();
    }

    private static void playTranslateY(Node node, double y, int millis) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(millis), node);
        transition.setToY(y);
        transition.setInterpolator(SMOOTH);
        transition.play();
    }
}
