package com.example.migren.migrbot.repository;

import com.example.migren.migrbot.entity.UsersEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, Long> {

    @Query(value = "SELECT * FROM users WHERE chat_id = :chatId", nativeQuery = true)
    Optional<UsersEntity> hasUserByChatId(@Param("chatId") Long chatId);

    @Query(value = "SELECT chat_id FROM users", nativeQuery = true)
    List<Long> findAllChatId();

    @Query(value = "SELECT last_question FROM users WHERE chat_id =:chatId", nativeQuery = true)
    String getLastQuestionByChatId(@Param("chatId") Long chatId);

    @Modifying
    @Transactional
    @Query(value = "update UsersEntity s set s.lastQuestion =:newLastQuestion where s.chatId =:chatId")
    void updateLastQuestionByChatId(@Param("chatId") Long chatId, @Param("newLastQuestion") String lastQuestion);
}
