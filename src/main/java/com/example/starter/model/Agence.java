package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
public class Agence {

  private Long id;
  private String noma;
  private String adressea;
  private String telephonea;
  private LocalDate dateDebuta;
  private LocalDate dateFina;
  private String status;
  private Long compagnieId;

  // For joins
  private String compagnieNom;

  private String compagnieRaisonSocial;

  public Agence() {}

}
