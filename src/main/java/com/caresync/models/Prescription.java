package com.caresync.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Prescription {
    private int id;
    private int medicalHistoryId;
    private String instructions;
    private LocalDateTime createdAt;
    private List<PrescriptionItem> items = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMedicalHistoryId() {
        return medicalHistoryId;
    }

    public void setMedicalHistoryId(int medicalHistoryId) {
        this.medicalHistoryId = medicalHistoryId;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<PrescriptionItem> getItems() {
        return items;
    }

    public void setItems(List<PrescriptionItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }
}
