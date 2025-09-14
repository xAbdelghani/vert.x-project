package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttestationAutoriser {

  private Long compagnieId;

  private Long typeAttestationId;

  private Boolean flag;

  // For joins
  private Compagnie compagnie;

  private TypeAttestation typeAttestation;

}
