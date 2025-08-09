package com.example.starter.repository;

import com.example.starter.model.Attestation;
import io.vertx.core.Future;

import java.time.LocalDate;
import java.util.List;

public interface AttestationRepository {

  Future<Long> save(Attestation attestation);
  Future<Attestation> findById(Long id);
  Future<Attestation> findByReference(String reference);
  Future<List<Attestation>> findByCompagnieId(Long compagnieId);
  Future<List<Attestation>> findByVehiculeId(Long vehiculeId);
  Future<List<Attestation>> findActiveByVehiculeId(Long vehiculeId);
  Future<List<Attestation>> findExpiringAttestations(LocalDate date);
  Future<Void> updateStatus(Long id, String status);
  Future<Void> addStatusHistory(Long attestationId, String status, String reason);
  Future<Void> updatePDFInfo(Long id, String fileName, String filePath, byte[] pdfData);
  Future<byte[]> getPDFData(Long id);

}
