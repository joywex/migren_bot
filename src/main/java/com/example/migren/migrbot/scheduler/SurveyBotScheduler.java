package com.example.migren.migrbot.scheduler;

import com.example.migren.migrbot.config.MigrenBotSetInfo;
import com.example.migren.migrbot.repository.SurveyRepository;
import com.example.migren.migrbot.repository.UsersRepository;
import com.example.migren.migrbot.service.MigrenBotService;
import com.example.migren.migrbot.utils.Utils;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
public class SurveyBotScheduler {

    private final MigrenBotService migrenBotService;
    private final MigrenBotSetInfo migrenBotSetInfo;
    private final UsersRepository usersRepository;
    private final SurveyRepository surveyRepository;

    public SurveyBotScheduler(MigrenBotService migrenBotService, MigrenBotSetInfo migrenBotSetInfo, UsersRepository usersRepository, SurveyRepository surveyRepository) {
        this.migrenBotService = migrenBotService;
        this.migrenBotSetInfo = migrenBotSetInfo;
        this.usersRepository = usersRepository;
        this.surveyRepository = surveyRepository;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void startChoice() {
        System.out.println("start scheduler");
        List<Long> getAllChatId = usersRepository.findAllChatId();

        for (Long chatId : getAllChatId) {
            if (!surveyRepository.existsByChatIdAndPainDate(chatId, Utils.getFormatDate())) {
                Update update = new Update();
                Message message = new Message();
                Chat chat = new Chat();
                chat.setId(chatId);
                message.setChat(chat);
                update.setMessage(message);
                migrenBotSetInfo.replySheduled(migrenBotService.painChoice(update, Utils.getFormatDate()));
            }
        }
    }
}
