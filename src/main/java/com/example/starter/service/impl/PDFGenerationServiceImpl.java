package com.example.starter.service.impl;

import com.example.starter.model.Attestation;
import com.example.starter.repository.CompagnieRepository;
import com.example.starter.repository.ModeleVehiculeRepository;
import com.example.starter.service.PDFGenerationService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;



public class PDFGenerationServiceImpl implements PDFGenerationService {

  private final CompagnieRepository compagnieRepository;
  private final ModeleVehiculeRepository modeleVehiculeRepository;

  private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
  private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
  private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
  private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

  public PDFGenerationServiceImpl(CompagnieRepository compagnieRepository,
                                  ModeleVehiculeRepository modeleVehiculeRepository) {
    this.compagnieRepository = compagnieRepository;
    this.modeleVehiculeRepository = modeleVehiculeRepository;
  }

  @Override
  public Future<byte[]> generateAttestationPDF(Attestation attestation, JsonObject vehicule,
                                               JsonObject typeAttestation, Long compagnieId) {

    return compagnieRepository.findById(compagnieId)
      .compose(compagnie -> {
        Long modelId = vehicule.getLong("model_id");
        return modeleVehiculeRepository.findById(modelId)
          .map(modele -> {
            try {
              ByteArrayOutputStream baos = new ByteArrayOutputStream();
              Document document = new Document(PageSize.A4);
              PdfWriter writer = PdfWriter.getInstance(document, baos);

              document.open();

              // Add header
              addHeader(document, typeAttestation.getString("libelle"));

              // Add company info
              addCompanySection(document, JsonObject.mapFrom(compagnie));

              // Add attestation info
              addAttestationSection(document, attestation);

              // Add vehicle info
              addVehicleSection(document, vehicule, JsonObject.mapFrom(modele));

              // Add validity section
              addValiditySection(document, attestation);

              // Add QR code
              addQRCode(document, attestation.getQrCode());

              // Add footer
              addFooter(document, attestation.getReferenceFlotte());

              document.close();

              return baos.toByteArray();

            } catch (Exception e) {
              throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
            }
          });
      });
  }

  private void addHeader(Document document, String typeLibelle) throws DocumentException {
    Paragraph header = new Paragraph("ATTESTATION " + typeLibelle.toUpperCase(), TITLE_FONT);
    header.setAlignment(Element.ALIGN_CENTER);
    header.setSpacingAfter(20);
    document.add(header);

    // Add line separator
    LineSeparator line = new LineSeparator();
    document.add(new Chunk(line));
    document.add(new Paragraph(" "));
  }

  private void addCompanySection(Document document, JsonObject compagnie) throws DocumentException {
    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);
    table.setSpacingBefore(10);
    table.setSpacingAfter(10);

    // Section title
    PdfPCell titleCell = new PdfPCell(new Phrase("INFORMATIONS ENTREPRISE", HEADER_FONT));
    titleCell.setColspan(2);
    titleCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
    titleCell.setPadding(5);
    table.addCell(titleCell);

    // Company details
    addTableRow(table, "Raison sociale:", compagnie.getString("raison_social"));
    addTableRow(table, "Adresse:", compagnie.getString("adresse", ""));
    addTableRow(table, "Téléphone:", compagnie.getString("telephone", ""));
    addTableRow(table, "Email:", compagnie.getString("email", ""));

    document.add(table);
  }

  private void addAttestationSection(Document document, Attestation attestation) throws DocumentException {
    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);
    table.setSpacingBefore(10);
    table.setSpacingAfter(10);

    // Section title
    PdfPCell titleCell = new PdfPCell(new Phrase("DETAILS ATTESTATION", HEADER_FONT));
    titleCell.setColspan(2);
    titleCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
    titleCell.setPadding(5);
    table.addCell(titleCell);

    // Attestation details
    addTableRow(table, "Référence:", attestation.getReferenceFlotte());
    addTableRow(table, "Date de génération:", formatDate(attestation.getDateGeneration()));
    addTableRow(table, "Statut:", "VALIDE");

    document.add(table);
  }

  private void addVehicleSection(Document document, JsonObject vehicule, JsonObject modele)
    throws DocumentException {
    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);
    table.setSpacingBefore(10);
    table.setSpacingAfter(10);

    // Section title
    PdfPCell titleCell = new PdfPCell(new Phrase("INFORMATIONS VEHICULE", HEADER_FONT));
    titleCell.setColspan(2);
    titleCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
    titleCell.setPadding(5);
    table.addCell(titleCell);

    // Vehicle details
    addTableRow(table, "Immatriculation:", vehicule.getString("immatriculation"));
    addTableRow(table, "Date d'immatriculation:",
      formatDate(LocalDate.parse(vehicule.getString("date_immatriculation", LocalDate.now().toString()))));

    if (modele != null) {
      addTableRow(table, "Marque:", modele.getString("marque", ""));
      addTableRow(table, "Modèle:", modele.getString("designation", ""));
      addTableRow(table, "Type:", modele.getString("type", ""));
      addTableRow(table, "Carburant:", modele.getString("carburant", ""));
    }

    document.add(table);
  }

  private void addValiditySection(Document document, Attestation attestation) throws DocumentException {
    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);
    table.setSpacingBefore(10);
    table.setSpacingAfter(10);

    // Section title
    PdfPCell titleCell = new PdfPCell(new Phrase("VALIDITE", HEADER_FONT));
    titleCell.setColspan(2);
    titleCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
    titleCell.setPadding(5);
    table.addCell(titleCell);

    // Validity details
    addTableRow(table, "Date de début:", formatDate(attestation.getDateDebut()));
    addTableRow(table, "Date de fin:", formatDate(attestation.getDateFin()));

    // Calculate days remaining
    long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), attestation.getDateFin());
    addTableRow(table, "Jours restants:", String.valueOf(Math.max(0, daysRemaining)));

    document.add(table);
  }

  private void addQRCode(Document document, String qrContent) throws Exception {
    // Generate QR code
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 200, 200);

    // Convert to image
    ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrBaos);

    // Add to PDF
    Image qrImage = Image.getInstance(qrBaos.toByteArray());
    qrImage.setAlignment(Element.ALIGN_CENTER);
    qrImage.setSpacingBefore(20);

    document.add(qrImage);

    // Add QR explanation
    Paragraph qrText = new Paragraph("Scanner ce code QR pour vérifier l'authenticité", SMALL_FONT);
    qrText.setAlignment(Element.ALIGN_CENTER);
    qrText.setSpacingAfter(10);
    document.add(qrText);
  }

  private void addFooter(Document document, String reference) throws DocumentException {
    document.add(new Paragraph(" "));
    LineSeparator line = new LineSeparator();
    document.add(new Chunk(line));

    Paragraph footer = new Paragraph();
    footer.setAlignment(Element.ALIGN_CENTER);
    footer.setSpacingBefore(10);

    Chunk chunk1 = new Chunk("Document généré le " + formatDate(LocalDate.now()) + " - ", SMALL_FONT);
    Chunk chunk2 = new Chunk("Référence: " + reference, SMALL_FONT);

    footer.add(chunk1);
    footer.add(chunk2);

    document.add(footer);

    // Add warning
    Paragraph warning = new Paragraph(
      "Ce document est strictement personnel et ne peut être cédé ou transmis à un tiers",
      new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC)
    );
    warning.setAlignment(Element.ALIGN_CENTER);
    warning.setSpacingBefore(5);
    document.add(warning);
  }

  private void addTableRow(PdfPTable table, String label, String value) {
    PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
    labelCell.setBorder(Rectangle.NO_BORDER);
    labelCell.setPadding(5);

    PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
    valueCell.setBorder(Rectangle.NO_BORDER);
    valueCell.setPadding(5);

    table.addCell(labelCell);
    table.addCell(valueCell);
  }

  private String formatDate(LocalDate date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
    return date.format(formatter);
  }
}
