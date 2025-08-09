package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModeleVehicule {

  private Long id;
  private String designation;
  private String type;        // VOITURE, CAMION, MOTO, etc.
  private String marque;      // RENAULT, PEUGEOT, etc.
  private Integer annee;
  private Double puissanceFiscale;
  private String carburant;   // ESSENCE, DIESEL, HYBRIDE, ELECTRIQUE

  // For UI display
  private Integer vehicleCount; // Number of vehicles using this model

}
