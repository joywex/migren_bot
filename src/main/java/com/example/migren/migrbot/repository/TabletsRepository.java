package com.example.migren.migrbot.repository;

import com.example.migren.migrbot.entity.SurveyEntity;
import com.example.migren.migrbot.entity.TabletsEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TabletsRepository extends JpaRepository<TabletsEntity, Long> {

    @Query(value = "SELECT exists (select 1 from tablets  where survey_id =:surveyId)", nativeQuery = true)
    boolean existsBySurveyId(@Param("surveyId") Long surveyId);

    @Modifying
    @Transactional
    @Query(value = "update TabletsEntity s set s.help =:newHelp where s.surveyId =:surveyId")
    void updateHelpBySurveyId(@Param("surveyId") Long surveyId, @Param("newHelp") boolean newHelp);


    @Modifying
    @Transactional
    @Query(value = "update TabletsEntity s set s.comment=:comment where s.surveyId=:surveyId")
    void updateCommentBySurveyId(@Param("surveyId") Long surveyId, @Param("comment") String comment);

}
