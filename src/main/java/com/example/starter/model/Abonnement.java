package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class Abonnement {

  private Long id;
  private Long compagnieId;
  private Long typeabonnementId;
  private Long factureId;
  private LocalDate dateAbonnement;
  private LocalDate dateFin;
  private BigDecimal montant;
  private String type;
  private String statut;
  private String devise;

  // Related entities
  private Compagnie compagnie;
  private TypeAbonnement typeAbonnement;
}
