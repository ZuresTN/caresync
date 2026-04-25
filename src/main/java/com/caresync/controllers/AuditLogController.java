package com.caresync.controllers;

import com.caresync.dao.AuditLogDAO;
import com.caresync.models.AuditLog;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class AuditLogController {
    @FXML private TextField actionField;
    @FXML private TextField entityField;
    @FXML private TextField actorField;
    @FXML private DatePicker datePicker;
    @FXML private TableView<AuditLog> auditTable;
    @FXML private TableColumn<AuditLog, Object> createdColumn;
    @FXML private TableColumn<AuditLog, String> actorColumn;
    @FXML private TableColumn<AuditLog, String> actionColumn;
    @FXML private TableColumn<AuditLog, String> entityColumn;
    @FXML private TableColumn<AuditLog, String> summaryColumn;
    @FXML private Label statusLabel;

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @FXML
    private void initialize() {
        createdColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        actorColumn.setCellValueFactory(new PropertyValueFactory<>("actorName"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        entityColumn.setCellValueFactory(new PropertyValueFactory<>("entityType"));
        summaryColumn.setCellValueFactory(new PropertyValueFactory<>("summary"));
        actionField.textProperty().addListener((obs, old, value) -> refresh());
        entityField.textProperty().addListener((obs, old, value) -> refresh());
        actorField.textProperty().addListener((obs, old, value) -> refresh());
        datePicker.valueProperty().addListener((obs, old, value) -> refresh());
        refresh();
    }

    @FXML
    private void clearFilters() {
        actionField.clear();
        entityField.clear();
        actorField.clear();
        datePicker.setValue(null);
        refresh();
    }

    private void refresh() {
        try {
            auditTable.setItems(FXCollections.observableArrayList(
                    auditLogDAO.findAll(actionField.getText(), entityField.getText(), actorField.getText(), datePicker.getValue())
            ));
            statusLabel.setText("Showing latest matching audit events.");
        } catch (Exception ex) {
            statusLabel.setText("Could not load audit logs: " + ex.getMessage());
        }
    }
}
