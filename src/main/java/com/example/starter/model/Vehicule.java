package com.example.starter.model;

import java.time.LocalDate;

public class Vehicule {
  private Long id;
  private String immatriculation;
  private LocalDate dateImmatriculation;
  private Long modelId;
  private Long compagnieId;  // THIS FIELD MUST EXIST

  // Related entities
  private ModeleVehicule modeleVehicule;
  private Compagnie compagnie;
  private Integer attestationCount;

  // Getters and setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getImmatriculation() {
    return immatriculation;
  }

  public void setImmatriculation(String immatriculation) {
    this.immatriculation = immatriculation;
  }

  public LocalDate getDateImmatriculation() {
    return dateImmatriculation;
  }

  public void setDateImmatriculation(LocalDate dateImmatriculation) {
    this.dateImmatriculation = dateImmatriculation;
  }

  public Long getModelId() {
    return modelId;
  }

  public void setModelId(Long modelId) {
    this.modelId = modelId;
  }

  public Long getCompagnieId() {
    return compagnieId;
  }

  public void setCompagnieId(Long compagnieId) {
    this.compagnieId = compagnieId;
  }

  public ModeleVehicule getModeleVehicule() {
    return modeleVehicule;
  }

  public void setModeleVehicule(ModeleVehicule modeleVehicule) {
    this.modeleVehicule = modeleVehicule;
  }

  public Compagnie getCompagnie() {
    return compagnie;
  }

  public void setCompagnie(Compagnie compagnie) {
    this.compagnie = compagnie;
  }

  public Integer getAttestationCount() {
    return attestationCount;
  }

  public void setAttestationCount(Integer attestationCount) {
    this.attestationCount = attestationCount;
  }
}
