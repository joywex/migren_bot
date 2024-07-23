package com.example.migren.migrbot.repository;

import com.example.migren.migrbot.entity.SurveyEntity;
import com.example.migren.migrbot.entity.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<SurveyEntity, Long> {

    @Query(value = "SELECT id FROM survey WHERE pain_date = :painDate", nativeQuery = true)
    Long findIdByPainDate(@Param("painDate") String painDate);
}
