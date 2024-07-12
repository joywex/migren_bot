package com.example.migren.migrbot.entity;


import jakarta.persistence.*;
import lombok.Data;

@Table(name = "tablets")
@Entity
@Data
public class TabletsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long surveyId;
    private String nameTablets;
    private boolean help;
}
