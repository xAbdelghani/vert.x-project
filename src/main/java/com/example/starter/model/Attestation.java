package com.example.starter.model;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;


@Getter
@Setter
public class Attestation {

  private Long id;
  private Long compagnieId;
  private Long vehiculeId;
  private Long typeAttestationId;
  private Long factureId;
  private String referenceFlotte;
  private LocalDate dateGeneration;
  private LocalDate dateDebut;
  private LocalDate dateFin;
  private String qrCode;
  private String nomFichier;
  private String cheminDepotAttestationGeneree;
  private String resteAttributsJson;
  private byte[] fichierData;
  private String statut = "EN_COURS";
  // Related entities
  private Compagnie compagnie;
  private Vehicule vehicule;
  private TypeAttestation typeAttestation;

}
