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
    private Long chat_id;
    private String pain_date;
    private String comment;
}
