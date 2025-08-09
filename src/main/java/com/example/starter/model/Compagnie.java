package com.example.starter.model;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class Compagnie {

  private Long id;
  private String nom;
  private String raison_social;
  private String adresse;
  private String telephone;
  private String email;
  private String statut;
  private String password;
  private BigDecimal soldeCompagnie = BigDecimal.ZERO;
  // Relationships - loaded on demand, not automatically
  private List<Abonnement> abonnements;
  private List<Agence> agences;
  private List<SoldePrepaye> soldePrepayes;
  private List<Attestation> attestations;
  private List<AttestationAutoriser> attestationsAutorisees;
  private List<Facture> factures;
  private List<Contact> contacts;
  private List<RelationPointventeCompagnie> relationPointventeCompagnies;
  private List<Notification> notifications;


}
