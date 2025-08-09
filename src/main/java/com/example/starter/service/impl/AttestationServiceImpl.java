package com.example.starter.service.impl;

import com.example.starter.model.Attestation;
import com.example.starter.repository.*;
import com.example.starter.service.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class AttestationServiceImpl implements AttestationService {

  private final Pool pgPool;
  private final AttestationRepository attestationRepository;
  private final VehiculeRepository vehiculeRepository;
  private final CompagnieRepository compagnieRepository;
  private final TypeAttestationRepository typeAttestationRepository;
  private final PrepayeService prepayeService;
  private final SubscriptionService subscriptionService;
  private final TypeAttestationService typeAttestationService;
  private final PDFGenerationService pdfGenerationService;
  private final TransactionRepository transactionRepository;

  private static final AtomicInteger sequenceGenerator = new AtomicInteger(0);

  public AttestationServiceImpl(
    Pool pgPool,
    AttestationRepository attestationRepository,
    VehiculeRepository vehiculeRepository,
    CompagnieRepository compagnieRepository,
    TypeAttestationRepository typeAttestationRepository,
    PrepayeService prepayeService,
    SubscriptionService subscriptionService,
    TypeAttestationService typeAttestationService,
    PDFGenerationService pdfGenerationService,
    TransactionRepository transactionRepository
  ) {
    this.pgPool = pgPool;
    this.attestationRepository = attestationRepository;
    this.vehiculeRepository = vehiculeRepository;
    this.compagnieRepository = compagnieRepository;
    this.typeAttestationRepository = typeAttestationRepository;
    this.prepayeService = prepayeService;
    this.subscriptionService = subscriptionService;
    this.typeAttestationService = typeAttestationService;
    this.pdfGenerationService = pdfGenerationService;
    this.transactionRepository = transactionRepository;
  }

  @Override
  public Future<JsonObject> generateAttestations(JsonObject requestData) {

    Long compagnieId = requestData.getLong("compagnie_id");
    JsonArray attestationsArray = requestData.getJsonArray("attestations");

    if (compagnieId == null || attestationsArray == null || attestationsArray.isEmpty()) {
      return Future.failedFuture("Company ID and attestations list are required");
    }

    // Start transaction
    return pgPool.withTransaction(client -> {
        return validateCompanyPaymentStatus(compagnieId)
          .compose(paymentInfo -> {
            // Validate all attestations
            List<Future<JsonObject>> validationFutures = new ArrayList<>();

            for (int i = 0; i < attestationsArray.size(); i++) {
              JsonObject attestation = attestationsArray.getJsonObject(i);
              validationFutures.add(validateAttestationRequest(compagnieId, attestation));
            }

            return CompositeFuture.all(validationFutures.stream().map(f -> (Future)f).collect(Collectors.toList()))
              .compose(validationResults -> {
                // Calculate total cost
                BigDecimal totalCost = calculateTotalCost(validationResults.list());
                String paymentType = paymentInfo.getString("payment_type");
                String devise = paymentInfo.getString("devise", "MAD");

                // Process payment based on type
                return processPayment(compagnieId, totalCost, paymentType, devise, attestationsArray.size())
                  .compose(paymentResult -> {
                    // Generate attestations
                    List<Future<JsonObject>> generationFutures = new ArrayList<>();

                    for (int i = 0; i < validationResults.size(); i++) {
                      JsonObject validatedData = validationResults.resultAt(i);
                      JsonObject originalRequest = attestationsArray.getJsonObject(i);

                      Future<JsonObject> genFuture = generateSingleAttestation(
                        compagnieId,
                        validatedData,
                        originalRequest,
                        paymentType,
                        devise
                      );

                      generationFutures.add(genFuture);
                    }

                    return CompositeFuture.all(generationFutures.stream().map(f -> (Future)f).collect(Collectors.toList()));
                  })
                  .map(generatedResults -> {
                    JsonObject response = new JsonObject()
                      .put("success", true)
                      .put("total_cost", totalCost)
                      .put("devise", devise)
                      .put("payment_type", paymentType)
                      .put("attestations", new JsonArray(generatedResults.list()));

                    return response;
                  });
              });
          });
      })
      .recover(error -> {
        return Future.succeededFuture(
          new JsonObject()
            .put("success", false)
            .put("error", error.getMessage())
        );
      });
  }

  private Future<JsonObject> validateCompanyPaymentStatus(Long compagnieId) {
    // Check PREPAYE first
    return prepayeService.getPrepayeBalance(compagnieId)
      .compose(balance -> {
        if (balance != null && balance.containsKey("solde")) {
          BigDecimal balanceAmount = balance.getString("solde") != null
            ? new BigDecimal(balance.getString("solde"))
            : BigDecimal.ZERO;

          return Future.succeededFuture(
            new JsonObject()
              .put("payment_type", "PREPAYE")
              .put("balance", balanceAmount)
              .put("devise", balance.getString("devise", "MAD"))
          );
        }

        // Check subscription
        return subscriptionService.getCompanySubscription(compagnieId)
          .compose(subscription -> {
            if (subscription == null || !subscription.containsKey("type")) {
              return Future.failedFuture("Company has no active payment method");
            }

            String type = subscription.getString("type");
            if ("AVANCE".equals(type) || "CAUTION".equals(type)) {
              return Future.succeededFuture(
                new JsonObject()
                  .put("payment_type", type)
                  .put("subscription", subscription)
                  .put("devise", subscription.getString("devise", "MAD"))
              );
            }

            return Future.failedFuture("Invalid subscription type");
          });
      });
  }

  private Future<JsonObject> validateAttestationRequest(Long compagnieId, JsonObject attestation) {
    Long vehiculeId = attestation.getLong("vehicule_id");
    Long typeAttestationId = attestation.getLong("type_attestation_id");

    if (vehiculeId == null || typeAttestationId == null) {
      return Future.failedFuture("Vehicle ID and attestation type ID are required");
    }

    // Check authorization
    return typeAttestationService.isCompanyAuthorized(compagnieId, typeAttestationId)
      .compose(authorized -> {
        if (!authorized) {
          return Future.failedFuture("Company not authorized for attestation type " + typeAttestationId);
        }

        // Check vehicle exists and belongs to company
        return vehiculeRepository.findById(vehiculeId)
          .compose(vehicule -> {
            if (vehicule == null) {
              return Future.failedFuture("Vehicle not found");
            }

            if (!vehicule.getCompagnieId().equals(compagnieId)) {
              return Future.failedFuture("Vehicle does not belong to company");
            }

            // Check for active attestations
            return hasActiveAttestation(vehiculeId)
              .compose(hasActive -> {
                if (hasActive) {
                  return Future.failedFuture("Vehicle has active attestation");
                }

                // Get attestation type details
                return typeAttestationRepository.findById(typeAttestationId)
                  .map(typeAttestation -> {
                    return new JsonObject()
                      .put("vehicule", JsonObject.mapFrom(vehicule))
                      .put("type_attestation", JsonObject.mapFrom(typeAttestation))
                      .put("prix", typeAttestation.getPrixUnitaire())
                      .put("devise", typeAttestation.getDevise());
                  });
              });
          });
      });
  }

  private BigDecimal calculateTotalCost(List<JsonObject> validatedAttestations) {
    return validatedAttestations.stream()
      .map(att -> {
        String prixStr = att.getString("prix");
        return prixStr != null ? new BigDecimal(prixStr) : BigDecimal.ZERO;
      })
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private Future<JsonObject> processPayment(Long compagnieId, BigDecimal amount,
                                            String paymentType, String devise, int count) {
    String description = "Génération de " + count + " attestation(s) - " + devise;

    switch (paymentType) {
      case "PREPAYE":
        return prepayeService.hasSufficientBalance(compagnieId, amount)
          .compose(sufficient -> {
            if (!sufficient) {
              return Future.failedFuture("Insufficient balance");
            }
            return prepayeService.deductCredit(compagnieId, amount, description);
          });

      case "AVANCE":
        return subscriptionService.canUseCredit(compagnieId, amount)
          .compose(canUse -> {
            if (!canUse) {
              return Future.failedFuture("Credit limit exceeded");
            }
            return subscriptionService.useCredit(compagnieId, amount, description);
          });

      case "CAUTION":
        return subscriptionService.canUseDeposit(compagnieId, amount)
          .compose(canUse -> {
            if (!canUse) {
              return Future.failedFuture("Insufficient deposit");
            }
            return subscriptionService.useDeposit(compagnieId, amount, description);
          });

      default:
        return Future.failedFuture("Unknown payment type: " + paymentType);
    }
  }

  private Future<JsonObject> generateSingleAttestation(Long compagnieId, JsonObject validatedData,
                                                       JsonObject originalRequest, String paymentType,
                                                       String devise) {
    Attestation attestation = new Attestation();

    // Extract data
    JsonObject vehicule = validatedData.getJsonObject("vehicule");
    JsonObject typeAttestation = validatedData.getJsonObject("type_attestation");

    // Generate reference
    String reference = generateReference(compagnieId);

    // Set attestation fields
    attestation.setCompagnieId(compagnieId);
    attestation.setVehiculeId(vehicule.getLong("id"));
    attestation.setTypeAttestationId(typeAttestation.getLong("id"));
    attestation.setReferenceFlotte(reference);
    attestation.setDateGeneration(LocalDate.now());
    attestation.setDateDebut(LocalDate.parse(originalRequest.getString("date_debut")));
    attestation.setDateFin(LocalDate.parse(originalRequest.getString("date_fin")));

    // Build additional attributes
    JsonObject attributes = new JsonObject()
      .put("payment_method", paymentType)
      .put("amount_charged", new BigDecimal(validatedData.getString("prix")))
      .put("currency", devise)
      .put("vehicle_details", new JsonObject()
        .put("immatriculation", vehicule.getString("immatriculation"))
        .put("model_id", vehicule.getLong("model_id")))
      .put("generated_at", LocalDate.now().toString());

    if (originalRequest.containsKey("attributs_supplementaires")) {
      attributes.put("custom_fields", originalRequest.getJsonObject("attributs_supplementaires"));
    }

    attestation.setResteAttributsJson(attributes.encode());

    // Generate QR code
    String qrContent = "https://attestation.ma/verify/" + reference;
    attestation.setQrCode(qrContent);

    // Save attestation
    return attestationRepository.save(attestation)
      .compose(attestationId -> {
        attestation.setId(attestationId);

        // Generate PDF
        return pdfGenerationService.generateAttestationPDF(attestation, vehicule, typeAttestation, compagnieId)
          .compose(pdfData -> {
            // Update attestation with PDF info
            String fileName = reference + "_" + LocalDate.now() + ".pdf";
            attestation.setNomFichier(fileName);
            attestation.setCheminDepotAttestationGeneree("/attestations/" + fileName);

            return attestationRepository.updatePDFInfo(attestationId, fileName,
                attestation.getCheminDepotAttestationGeneree(), pdfData)
              .map(v -> new JsonObject()
                .put("id", attestationId)
                .put("reference", reference)
                .put("vehicule_id", vehicule.getLong("id"))
                .put("type", typeAttestation.getString("libelle"))
                .put("date_debut", attestation.getDateDebut().toString())
                .put("date_fin", attestation.getDateFin().toString())
                .put("pdf_url", "/api/attestations/" + attestationId + "/pdf")
                .put("qr_code", qrContent));
          });
      });
  }

  private String generateReference(Long compagnieId) {
    int sequence = sequenceGenerator.incrementAndGet();
    String year = String.valueOf(LocalDate.now().getYear());
    String companyCode = String.format("CMP%03d", compagnieId);
    String sequenceStr = String.format("%06d", sequence);

    return String.format("ATT-%s-%s-%s", year, companyCode, sequenceStr);
  }

  @Override
  public Future<Boolean> hasActiveAttestation(Long vehiculeId) {
    return attestationRepository.findActiveByVehiculeId(vehiculeId)
      .map(attestations -> !attestations.isEmpty());
  }

  @Override
  public Future<JsonObject> getAttestation(Long id) {
    return attestationRepository.findById(id)
      .map(attestation -> {
        if (attestation == null) {
          throw new RuntimeException("Attestation not found");
        }
        return attestationToJson(attestation);
      });
  }

  @Override
  public Future<JsonObject> getAttestationByReference(String reference) {
    return attestationRepository.findByReference(reference)
      .map(attestation -> {
        if (attestation == null) {
          throw new RuntimeException("Attestation not found");
        }
        return attestationToJson(attestation);
      });
  }

  @Override
  public Future<JsonArray> getCompanyAttestations(Long compagnieId) {
    return attestationRepository.findByCompagnieId(compagnieId)
      .map(attestations -> {
        JsonArray array = new JsonArray();
        attestations.forEach(att -> array.add(attestationToJson(att)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getVehicleAttestations(Long vehiculeId) {
    return attestationRepository.findByVehiculeId(vehiculeId)
      .map(attestations -> {
        JsonArray array = new JsonArray();
        attestations.forEach(att -> array.add(attestationToJson(att)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getActiveVehicleAttestations(Long vehiculeId) {
    return attestationRepository.findActiveByVehiculeId(vehiculeId)
      .map(attestations -> {
        JsonArray array = new JsonArray();
        attestations.forEach(att -> array.add(attestationToJson(att)));
        return array;
      });
  }

  @Override
  public Future<JsonObject> cancelAttestation(Long id, String reason) {
    return attestationRepository.findById(id)
      .compose(attestation -> {
        if (attestation == null) {
          return Future.failedFuture("Attestation not found");
        }

        if (!"EN_COURS".equals(attestation.getStatut())) {
          return Future.failedFuture("Can only cancel active attestations");
        }

        // Update status
        return attestationRepository.updateStatus(id, "ANNULE")
          .compose(v -> {
            // Record in history
            return attestationRepository.addStatusHistory(id, "ANNULE", reason)
              .map(v2 -> new JsonObject()
                .put("success", true)
                .put("message", "Attestation cancelled successfully"));
          });
      });
  }

  @Override
  public Future<Integer> expireAttestations() {
    LocalDate today = LocalDate.now();
    return attestationRepository.findExpiringAttestations(today)
      .compose(expiring -> {
        List<Future<Void>> updateFutures = new ArrayList<>();

        for (Attestation att : expiring) {
          Future<Void> updateFuture = attestationRepository.updateStatus(att.getId(), "TERMINE")
            .compose(v -> attestationRepository.addStatusHistory(att.getId(), "TERMINE", "Expiration automatique"));

          updateFutures.add(updateFuture);
        }

        return CompositeFuture.all(updateFutures.stream().map(f -> (Future)f).collect(Collectors.toList()))
          .map(v -> updateFutures.size());
      });
  }

  @Override
  public Future<byte[]> getAttestationPDF(Long id) {
    return attestationRepository.getPDFData(id);
  }

  @Override
  public Future<JsonObject> verifyAttestation(String reference) {
    return attestationRepository.findByReference(reference)
      .map(attestation -> {
        if (attestation == null) {
          return new JsonObject()
            .put("valid", false)
            .put("message", "Attestation not found");
        }

        LocalDate today = LocalDate.now();
        boolean isValid = "EN_COURS".equals(attestation.getStatut()) &&
          !today.isBefore(attestation.getDateDebut()) &&
          !today.isAfter(attestation.getDateFin());

        JsonObject result = new JsonObject()
          .put("valid", isValid)
          .put("reference", attestation.getReferenceFlotte())
          .put("status", attestation.getStatut())
          .put("date_debut", attestation.getDateDebut().toString())
          .put("date_fin", attestation.getDateFin().toString());

        if (attestation.getVehicule() != null) {
          result.put("vehicule_immatriculation", attestation.getVehicule().getImmatriculation());
        }

        if (attestation.getTypeAttestation() != null) {
          result.put("type", attestation.getTypeAttestation().getLibelle());
        }

        return result;
      });
  }

  private JsonObject attestationToJson(Attestation attestation) {
    JsonObject json = new JsonObject()
      .put("id", attestation.getId())
      .put("reference", attestation.getReferenceFlotte())
      .put("compagnie_id", attestation.getCompagnieId())
      .put("vehicule_id", attestation.getVehiculeId())
      .put("type_attestation_id", attestation.getTypeAttestationId())
      .put("date_generation", attestation.getDateGeneration().toString())
      .put("date_debut", attestation.getDateDebut().toString())
      .put("date_fin", attestation.getDateFin().toString())
      .put("statut", attestation.getStatut())
      .put("qr_code", attestation.getQrCode());

    if (attestation.getNomFichier() != null) {
      json.put("pdf_url", "/api/attestations/" + attestation.getId() + "/pdf");
    }

    if (attestation.getResteAttributsJson() != null) {
      json.put("attributes", new JsonObject(attestation.getResteAttributsJson()));
    }

    // Add related data if loaded
    if (attestation.getVehicule() != null) {
      json.put("vehicule_immatriculation", attestation.getVehicule().getImmatriculation());
    }

    if (attestation.getTypeAttestation() != null) {
      json.put("type_libelle", attestation.getTypeAttestation().getLibelle())
        .put("prix", attestation.getTypeAttestation().getPrixUnitaire());
    }

    return json;
  }
}
