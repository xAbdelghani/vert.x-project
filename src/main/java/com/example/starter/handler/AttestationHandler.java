package com.example.starter.handler;


import com.example.starter.service.AttestationService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.io.ByteArrayOutputStream;

public class AttestationHandler {

  private final AttestationService attestationService;

  public AttestationHandler(AttestationService attestationService) {
    this.attestationService = attestationService;
  }

  // POST /api/attestations/generate
  public void generateAttestations(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    attestationService.generateAttestations(body)
      .onSuccess(result -> {
        int statusCode = result.getBoolean("success") ? 201 : 400;
        ctx.response()
          .setStatusCode(statusCode)
          .putHeader("content-type", "application/json")
          .end(result.encode());
      })
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject()
          .put("success", false)
          .put("error", err.getMessage()).encode()));
  }

  // GET /api/attestations/:id
  public void getAttestation(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    attestationService.getAttestation(id)
      .onSuccess(attestation -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(attestation.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/attestations/reference/:reference
  public void getAttestationByReference(RoutingContext ctx) {
    String reference = ctx.pathParam("reference");

    attestationService.getAttestationByReference(reference)
      .onSuccess(attestation -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(attestation.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/companies/:id/attestations
  public void getCompanyAttestations(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));

    attestationService.getCompanyAttestations(compagnieId)
      .onSuccess(attestations -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(attestations.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/vehicles/:id/attestations
  public void getVehicleAttestations(RoutingContext ctx) {
    Long vehiculeId = Long.parseLong(ctx.pathParam("id"));

    attestationService.getVehicleAttestations(vehiculeId)
      .onSuccess(attestations -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(attestations.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // PUT /api/attestations/:id/cancel
  public void cancelAttestation(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();
    String reason = body.getString("reason", "Annulation manuelle");

    attestationService.cancelAttestation(id, reason)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/attestations/:id/pdf
  public void getAttestationPDF(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    // Get attestation details first
    attestationService.getAttestation(id)
      .compose(attestationJson -> {
        // Regenerate PDF from attestation data
        Long vehiculeId = attestationJson.getLong("vehicule_id");
        Long typeAttestationId = attestationJson.getLong("type_attestation_id");
        Long compagnieId = attestationJson.getLong("compagnie_id");

        // Create a simple PDF with the attestation data
        return generateSimplePDF(attestationJson);
      })
      .onSuccess(pdfData -> {
        ctx.response()
          .putHeader("content-type", "application/pdf")
          .putHeader("content-disposition", "inline; filename=attestation_" + id + ".pdf")
          .end(io.vertx.core.buffer.Buffer.buffer(pdfData));
      })
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", "Attestation not found or PDF generation failed").encode()));
  }

  private Future<byte[]> generateSimplePDF(JsonObject attestation) {
    return Future.future(promise -> {
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);

        document.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("ATTESTATION", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        // Content
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
        document.add(new Paragraph("Référence: " + attestation.getString("reference"), normalFont));
        document.add(new Paragraph("Type: " + attestation.getString("type_libelle", "Attestation"), normalFont));
        document.add(new Paragraph("Véhicule: " + attestation.getString("vehicule_immatriculation", "N/A"), normalFont));
        document.add(new Paragraph("Date début: " + attestation.getString("date_debut"), normalFont));
        document.add(new Paragraph("Date fin: " + attestation.getString("date_fin"), normalFont));
        document.add(new Paragraph("Statut: " + attestation.getString("statut", "EN_COURS"), normalFont));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("QR Code: " + attestation.getString("qr_code"), normalFont));

        document.close();

        promise.complete(baos.toByteArray());
      } catch (Exception e) {
        promise.fail(e);
      }
    });
  }

  // GET /api/attestations/verify/:reference
  public void verifyAttestation(RoutingContext ctx) {
    String reference = ctx.pathParam("reference");

    attestationService.verifyAttestation(reference)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // POST /api/attestations/expire - Called by scheduled job
  public void expireAttestations(RoutingContext ctx) {
    attestationService.expireAttestations()
      .onSuccess(count -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(new JsonObject()
          .put("success", true)
          .put("expired_count", count)
          .put("message", count + " attestations expired").encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }
}
