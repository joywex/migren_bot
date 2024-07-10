package com.example.migren.migrbot.config;

import com.example.migren.migrbot.service.MigrenBotService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.print.attribute.standard.Media;

@Component
public class MigrenBotSetInfo extends TelegramLongPollingBot {

    private final MigrenBotConfig migrenBotConfig;
    private final MigrenBotService migrenBotService;

    private MigrenBotSetInfo(MigrenBotConfig migrenBotConfig, MigrenBotService migrenBotService) {
        this.migrenBotConfig = migrenBotConfig;
        this.migrenBotService = migrenBotService;
    }

    @Override
    public String getBotUsername() {
        return this.migrenBotConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return this.migrenBotConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            execute(this.migrenBotService.sendMessage(update.getMessage()));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }


    }




}
