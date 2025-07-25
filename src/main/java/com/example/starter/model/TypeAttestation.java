package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class TypeAttestation {
  private Long id;
  private String libelle;
  private BigDecimal prixUnitaire;
  private String devise;
}
