package com.example.starter.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class UpdateAgenceRequest {

  private String noma;
  private String adressea;
  private String telephonea;
  private LocalDate dateDebuta;
  private LocalDate dateFina;
  private Long compagnieId;

}
