package com.example.migren.migrbot.repository;

import com.example.migren.migrbot.entity.SurveyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyRepository extends JpaRepository<SurveyEntity,Long> {
}
