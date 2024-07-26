package com.example.migren.migrbot.scheduler;

import com.example.migren.migrbot.config.MigrenBotSetInfo;
import com.example.migren.migrbot.service.MigrenBotService;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
public class SurveyBotScheduler {

    private final MigrenBotService migrenBotService;
    private final MigrenBotSetInfo migrenBotSetInfo;

    public SurveyBotScheduler(MigrenBotService migrenBotService, MigrenBotSetInfo migrenBotSetInfo) {
        this.migrenBotService = migrenBotService;
        this.migrenBotSetInfo = migrenBotSetInfo;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void startChoice() {
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(0L);
        message.setChat(chat);
        migrenBotSetInfo.replySheduled(migrenBotService.painChoice(message));

    }
}
