package com.example.starter.service;

import com.example.starter.model.Attestation;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface PDFGenerationService {
  Future<byte[]> generateAttestationPDF(Attestation attestation, JsonObject vehicule,
                                        JsonObject typeAttestation, Long compagnieId);
}
