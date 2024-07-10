package com.example.migren.migrbot.repository;

import com.example.migren.migrbot.entity.SurveyEntity;
import com.example.migren.migrbot.entity.TabletsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TabletsRepository extends JpaRepository<TabletsEntity, Long> {
}
