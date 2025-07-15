package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StatutHistoriqueC {
  private Long id;
  private Long relationId;
  private Long idStatutc;
  private LocalDate dateDebutc;
  private LocalDate dateFinc;
  private String dateChangement;
  private String raison;
  private String statut;
  // Related entity
  private StatutC statutC;
}
