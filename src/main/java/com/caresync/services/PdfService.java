package com.caresync.services;

import com.caresync.dao.SettingsDAO;
import com.caresync.models.MedicalHistory;
import com.caresync.models.Patient;
import com.caresync.models.Prescription;
import com.caresync.models.PrescriptionItem;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class PdfService {
    private static final Color TEAL = new Color(17, 128, 135);
    private static final Color TEAL_DARK = new Color(18, 86, 92);
    private static final Color NAVY = new Color(20, 38, 62);
    private static final Color MUTED = new Color(89, 109, 128);
    private static final Color LINE = new Color(211, 229, 232);
    private static final Color SURFACE = new Color(245, 250, 250);
    private static final Color WARNING_SURFACE = new Color(255, 248, 231);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    private final SettingsDAO settingsDAO = new SettingsDAO();

    public File generatePrescription(Patient patient, MedicalHistory history, Prescription prescription) throws Exception {
        return generateDocument(patient, history, prescription, loadSettings(), DocumentType.COMPLETE_MEDICAL_REPORT);
    }

    public File generatePrescription(Patient patient, MedicalHistory history, Prescription prescription, Map<String, String> settings) throws Exception {
        return generateDocument(patient, history, prescription, settings, DocumentType.COMPLETE_MEDICAL_REPORT);
    }

    public File generateMedicalReport(Patient patient, MedicalHistory history) throws Exception {
        return generateDocument(patient, history, null, loadSettings(), DocumentType.MEDICAL_REPORT_ONLY);
    }

    public File generatePrescriptionOnly(Patient patient, MedicalHistory history, Prescription prescription) throws Exception {
        return generateDocument(patient, history, prescription, loadSettings(), DocumentType.PRESCRIPTION_ONLY);
    }

    private File generateDocument(Patient patient, MedicalHistory history, Prescription prescription,
                                  Map<String, String> settings, DocumentType type) throws Exception {
        File directory = new File("generated-prescriptions");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create generated-prescriptions directory.");
        }
        File file = new File(directory, type.filePrefix + "_" + sanitize(patient.getFullName()) + "_" + System.currentTimeMillis() + ".pdf");

        Document document = new Document(PageSize.A4, 42, 42, 44, 48);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setPageEvent(new ReportPageEvent(settings.getOrDefault("clinic_name", "CareSync Clinic")));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.WHITE);
        Font clinicFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(225, 244, 245));
        Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, NAVY);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, MUTED);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(45, 58, 72));
        Font strongFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, NAVY);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

        document.add(header(settings, titleFont, clinicFont, type.documentLabel));
        document.add(summaryStrip(patient, history, strongFont, smallFont));
        document.add(twoColumnSection(
                "Patient Details",
                detailsTable(new String[][]{
                        {"Patient", display(patient.getFullName(), "Patient #" + patient.getId())},
                        {"Gender", patient.getGenderLabel()},
                        {"Date of birth", patient.getDateOfBirth() == null ? "Not specified" : DATE_FORMAT.format(patient.getDateOfBirth())},
                        {"Age", ageLabel(patient)},
                        {"Phone", display(patient.getPhone(), "Not recorded")},
                        {"Email", display(patient.getEmail(), "Not recorded")},
                        {"Address", display(patient.getAddress(), "Not recorded")},
                        {"Emergency contact", display(patient.getEmergencyContact(), "Not recorded")}
                }, labelFont, bodyFont),
                "Clinical Details",
                detailsTable(new String[][]{
                        {"Record", history.getId() > 0 ? "MR-" + history.getId() : "Draft"},
                        {"Doctor", display(history.getDoctorName(), "Not recorded")},
                        {"Created", dateTimeLabel(history.getCreatedAt())},
                        {"Generated", DATE_TIME_FORMAT.format(LocalDateTime.now())},
                        {"Blood group", display(patient.getBloodGroup(), "Not recorded")},
                        {"Allergies", display(patient.getAllergies(), "No known allergies")}
                }, labelFont, bodyFont)
        ));

        if (type.includeClinicalSections) {
            document.add(sectionCard("Diagnosis", display(history.getDiagnosis(), "No diagnosis recorded."), headingFont, bodyFont, SURFACE));
            document.add(sectionCard("Treatment Notes", display(history.getTreatmentNotes(), "No treatment notes recorded."), headingFont, bodyFont, Color.WHITE));
        }

        if (type.includePrescriptionSection) {
            addPrescriptionSection(document, prescription, headingFont, strongFont, bodyFont);
        }
        document.add(signatureBlock(history, strongFont, smallFont));

        document.close();
        return file;
    }

    private Map<String, String> loadSettings() {
        try {
            return settingsDAO.findAll();
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private void addPrescriptionSection(Document document, Prescription prescription, Font headingFont,
                                        Font strongFont, Font bodyFont) throws Exception {
        document.add(sectionTitle("Prescription", headingFont));
        if (prescription == null || prescription.getItems().isEmpty()) {
            document.add(emptyBox("No medication items were recorded.", bodyFont));
        } else {
            PdfPTable table = new PdfPTable(new float[]{3f, 2f, 2f, 2f, 3f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(12);
            addHeader(table, "Medicine", strongFont);
            addHeader(table, "Dosage", strongFont);
            addHeader(table, "Frequency", strongFont);
            addHeader(table, "Duration", strongFont);
            addHeader(table, "Notes", strongFont);
            for (PrescriptionItem item : prescription.getItems()) {
                addCell(table, item.getMedicineName(), bodyFont);
                addCell(table, item.getDosage(), bodyFont);
                addCell(table, item.getFrequency(), bodyFont);
                addCell(table, item.getDuration(), bodyFont);
                addCell(table, item.getNotes(), bodyFont);
            }
            document.add(table);
        }
        String instructions = prescription == null ? null : prescription.getInstructions();
        document.add(sectionCard("Patient Instructions", display(instructions, "No additional instructions recorded."), headingFont, bodyFont, WARNING_SURFACE));
    }

    private PdfPTable header(Map<String, String> settings, Font titleFont, Font clinicFont, String documentLabel) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{0.8f, 3.4f, 1.35f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(14);

        PdfPCell logoCell = noBorderCell();
        Image logo = loadLogo();
        if (logo != null) {
            logo.scaleToFit(48, 48);
            logoCell.addElement(logo);
        }
        logoCell.setBackgroundColor(TEAL_DARK);
        logoCell.setPadding(12);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(logoCell);

        PdfPCell clinicCell = noBorderCell();
        clinicCell.setBackgroundColor(TEAL_DARK);
        clinicCell.setPadding(12);
        Paragraph title = new Paragraph(display(settings.get("clinic_name"), "CareSync Clinic"), titleFont);
        title.setSpacingAfter(4);
        clinicCell.addElement(title);
        clinicCell.addElement(new Paragraph(clinicLine(settings), clinicFont));
        table.addCell(clinicCell);

        PdfPCell typeCell = noBorderCell();
        typeCell.setBackgroundColor(TEAL_DARK);
        typeCell.setPadding(12);
        typeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph type = new Paragraph(documentLabel, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE));
        type.setAlignment(Element.ALIGN_RIGHT);
        typeCell.addElement(type);
        Paragraph generated = new Paragraph("Generated " + DATE_FORMAT.format(LocalDate.now()), clinicFont);
        generated.setAlignment(Element.ALIGN_RIGHT);
        typeCell.addElement(generated);
        table.addCell(typeCell);

        return table;
    }

    private PdfPTable summaryStrip(Patient patient, MedicalHistory history, Font strongFont, Font smallFont) {
        PdfPTable table = new PdfPTable(new float[]{1.6f, 1.2f, 1.2f, 1.2f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(14);
        addSummaryCell(table, "Patient", display(patient.getFullName(), "Patient #" + patient.getId()), strongFont, smallFont);
        addSummaryCell(table, "Doctor", display(history.getDoctorName(), "Not recorded"), strongFont, smallFont);
        addSummaryCell(table, "Diagnosis", display(history.getDiagnosis(), "Not recorded"), strongFont, smallFont);
        addSummaryCell(table, "Record date", dateTimeLabel(history.getCreatedAt()), strongFont, smallFont);
        return table;
    }

    private PdfPTable twoColumnSection(String leftTitle, PdfPTable leftContent, String rightTitle, PdfPTable rightContent) {
        PdfPTable wrapper = new PdfPTable(new float[]{1f, 1f});
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingAfter(12);
        wrapper.addCell(panelCell(leftTitle, leftContent));
        wrapper.addCell(panelCell(rightTitle, rightContent));
        return wrapper;
    }

    private PdfPCell panelCell(String title, Element content) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(LINE);
        cell.setBorderWidth(0.8f);
        cell.setPadding(10);
        cell.setBackgroundColor(Color.WHITE);
        Paragraph heading = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, NAVY));
        heading.setSpacingAfter(6);
        cell.addElement(heading);
        cell.addElement(content);
        return cell;
    }

    private PdfPTable detailsTable(String[][] rows, Font labelFont, Font bodyFont) {
        PdfPTable table = new PdfPTable(new float[]{1f, 2f});
        table.setWidthPercentage(100);
        for (String[] row : rows) {
            PdfPCell labelCell = detailCell(row[0].toUpperCase(), labelFont);
            PdfPCell valueCell = detailCell(row[1], bodyFont);
            table.addCell(labelCell);
            table.addCell(valueCell);
        }
        return table;
    }

    private PdfPCell detailCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(231, 240, 242));
        cell.setPadding(5);
        return cell;
    }

    private Paragraph sectionTitle(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(4);
        paragraph.setSpacingAfter(7);
        return paragraph;
    }

    private PdfPTable sectionCard(String title, String body, Font headingFont, Font bodyFont, Color background) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(12);
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(LINE);
        cell.setBorderWidth(0.8f);
        cell.setPadding(10);
        cell.setBackgroundColor(background);
        Paragraph heading = new Paragraph(title, headingFont);
        heading.setSpacingAfter(5);
        Paragraph content = new Paragraph(body, bodyFont);
        content.setLeading(14);
        cell.addElement(heading);
        cell.addElement(content);
        table.addCell(cell);
        return table;
    }

    private PdfPTable emptyBox(String text, Font font) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(12);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(LINE);
        cell.setPadding(10);
        cell.setBackgroundColor(SURFACE);
        table.addCell(cell);
        return table;
    }

    private PdfPTable signatureBlock(MedicalHistory history, Font strongFont, Font smallFont) {
        PdfPTable table = new PdfPTable(new float[]{1.1f, 1f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(12);

        PdfPCell preparedCell = noBorderCell();
        preparedCell.addElement(new Paragraph("Prepared by", smallFont));
        preparedCell.addElement(new Paragraph(display(history.getDoctorName(), "Attending clinician"), strongFont));
        table.addCell(preparedCell);

        PdfPCell signatureCell = noBorderCell();
        signatureCell.addElement(new Paragraph("\nDoctor signature: ______________________________", strongFont));
        table.addCell(signatureCell);

        return table;
    }

    private void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(226, 245, 246));
        cell.setBorderColor(LINE);
        cell.setPadding(7);
        cell.setUseAscender(true);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(display(text, "-"), font));
        cell.setBorderColor(LINE);
        cell.setPadding(7);
        cell.setUseAscender(true);
        table.addCell(cell);
    }

    private void addSummaryCell(PdfPTable table, String label, String value, Font valueFont, Font labelFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(LINE);
        cell.setPadding(9);
        cell.setBackgroundColor(SURFACE);
        cell.addElement(new Paragraph(label.toUpperCase(), labelFont));
        Paragraph content = new Paragraph(value, valueFont);
        content.setSpacingBefore(2);
        cell.addElement(content);
        table.addCell(cell);
    }

    private PdfPCell noBorderCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private Image loadLogo() {
        try {
            URL logoUrl = getClass().getResource("/images/caresync-logo-mark.png");
            return logoUrl == null ? null : Image.getInstance(logoUrl);
        } catch (Exception ex) {
            return null;
        }
    }

    private String clinicLine(Map<String, String> settings) {
        String address = display(settings.get("clinic_address"), "");
        String phone = display(settings.get("clinic_phone"), "");
        if (!address.isBlank() && !phone.isBlank()) {
            return address + "\n" + phone;
        }
        return address + phone;
    }

    private String ageLabel(Patient patient) {
        if (patient.getDateOfBirth() == null) {
            return "Not specified";
        }
        int years = Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
        return years >= 0 ? years + " years" : "Not specified";
    }

    private String dateTimeLabel(LocalDateTime dateTime) {
        return dateTime == null ? "Not recorded" : DATE_TIME_FORMAT.format(dateTime);
    }

    private String display(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String sanitize(String value) {
        String fallback = value == null || value.isBlank() ? "patient" : value;
        return fallback.replaceAll("[^A-Za-z0-9]+", "_");
    }

    private enum DocumentType {
        COMPLETE_MEDICAL_REPORT("medical_report", "MEDICAL REPORT", true, true),
        MEDICAL_REPORT_ONLY("medical_report_only", "MEDICAL REPORT", true, false),
        PRESCRIPTION_ONLY("prescription_only", "PRESCRIPTION", false, true);

        private final String filePrefix;
        private final String documentLabel;
        private final boolean includeClinicalSections;
        private final boolean includePrescriptionSection;

        DocumentType(String filePrefix, String documentLabel, boolean includeClinicalSections, boolean includePrescriptionSection) {
            this.filePrefix = filePrefix;
            this.documentLabel = documentLabel;
            this.includeClinicalSections = includeClinicalSections;
            this.includePrescriptionSection = includePrescriptionSection;
        }
    }

    private static class ReportPageEvent extends PdfPageEventHelper {
        private final String clinicName;
        private final Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

        ReportPageEvent(String clinicName) {
            this.clinicName = clinicName == null || clinicName.isBlank() ? "CareSync Clinic" : clinicName;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle page = document.getPageSize();
            canvas.saveState();
            canvas.setColorStroke(new Color(223, 238, 241));
            canvas.setLineWidth(0.8f);
            canvas.rectangle(page.getLeft(26), page.getBottom(26), page.getWidth() - 52, page.getHeight() - 52);
            canvas.stroke();
            canvas.restoreState();

            String footer = clinicName + " | Confidential medical document | Page " + writer.getPageNumber();
            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_CENTER,
                    new Phrase(footer, footerFont),
                    (page.getLeft() + page.getRight()) / 2,
                    page.getBottom(18),
                    0
            );
        }
    }
}
