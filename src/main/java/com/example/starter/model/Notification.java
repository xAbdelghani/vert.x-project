package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
public class Notification {

  private Long id;
  private Long companyId;
  private String message;
  private String description;
  private LocalDateTime timestamp;
  private Boolean read;
  private String type; // AUTHORIZATION_REQUEST, SYSTEM, INFO, etc.
  private String metadata; // JSON string for additional data

}
