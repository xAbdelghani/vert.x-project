package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface AttestationService {

  // Generate attestations from JSON input
  Future<JsonObject> generateAttestations(JsonObject requestData);

  // Get attestation by ID
  Future<JsonObject> getAttestation(Long id);

  // Get attestation by reference
  Future<JsonObject> getAttestationByReference(String reference);

  // Get company attestations
  Future<JsonArray> getCompanyAttestations(Long compagnieId);

  // Get vehicle attestations
  Future<JsonArray> getVehicleAttestations(Long vehiculeId);

  // Cancel attestation
  Future<JsonObject> cancelAttestation(Long id, String reason);

  // Get active attestations for a vehicle
  Future<JsonArray> getActiveVehicleAttestations(Long vehiculeId);

  // Check if vehicle has active attestation
  Future<Boolean> hasActiveAttestation(Long vehiculeId);

  // Expire attestations (for scheduled job)
  Future<Integer> expireAttestations();

  // Get attestation PDF
  Future<byte[]> getAttestationPDF(Long id);

  // Verify attestation by QR code
  Future<JsonObject> verifyAttestation(String reference);
}
