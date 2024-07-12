package com.example.migren.migrbot.repository;

import com.example.migren.migrbot.entity.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, Long> {

    @Query(value = "SELECT * FROM users WHERE chat_id = :chatId", nativeQuery = true)
    Optional<UsersEntity> hasUserByChatId(@Param("chatId") Long chatId);
}
