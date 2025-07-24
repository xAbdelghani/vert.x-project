package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SoldePrepaye {

  private Long id;
  private Long compagnieId;
  private Long factureId;
  private Long offreId;
  private LocalDate dateAbonnement;
  private BigDecimal solde;              // Monetary balance
  private Integer soldeAttestation;      // Number of attestations (if applicable)
  private String type;
  private String statut;
  private String devise;

  // Related entities
  private Compagnie compagnie;
  private Offre offre;

}
