package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TypeAbonnement {

  private Long id;
  private String libelle;
  private String categorie; // AVANCE or CAUTION
  private Double duree;
  private String unite;
  private String description;
  private Boolean actif;

}
