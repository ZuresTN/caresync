package com.caresync.utils;

import com.caresync.models.AppointmentStatus;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class UiStyleUtil {
    private UiStyleUtil() {
    }

    public static <T> void applyAppointmentStatusCell(TableColumn<T, AppointmentStatus> column) {
        column.setCellFactory(ignored -> new TableCell<T, AppointmentStatus>() {
            @Override
            protected void updateItem(AppointmentStatus item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(style -> style.startsWith("status-cell-"));
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.name());
                getStyleClass().add("status-cell-" + item.name().toLowerCase());
            }
        });
    }

    public static <T> void applyTextBadgeCell(TableColumn<T, String> column, String type) {
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(style -> style.startsWith("badge-cell-"));
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                getStyleClass().add("badge-cell-" + type);
            }
        });
    }

    public static void applyEmptyState(TableView<?> table, String message) {
        Label label = new Label(message);
        label.getStyleClass().add("empty-state");
        table.setPlaceholder(label);
    }
}
