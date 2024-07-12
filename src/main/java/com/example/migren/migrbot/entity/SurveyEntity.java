package com.example.migren.migrbot.entity;


import jakarta.persistence.*;
import lombok.Data;

@Table(name = "survey")
@Entity
@Data
public class SurveyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long chatId;
    private String painDate;
    private String comment;
}
