package com.example.starter.model;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class RelationPointventeCompagnie {
  private Long id;
  private Long pointventeId;
  private Long compagnieId;
  private LocalDate dateDebut;
  private LocalDate dateFin;
  private Boolean active;
  private String status;
  private String suspensionReason;

  // Related entities - loaded on demand
  private Pointvente pointvente;
  private Compagnie compagnie;
  private List<StatutHistoriqueC> historique;
}
