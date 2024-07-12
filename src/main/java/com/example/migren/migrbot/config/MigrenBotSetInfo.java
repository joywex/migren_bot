package com.example.migren.migrbot.config;

import com.example.migren.migrbot.service.MigrenBotService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.print.attribute.standard.Media;
import java.util.Timer;
import java.util.TimerTask;

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
        SendMessage sendMessage = this.migrenBotService.sendMessage(update);
//            execute(this.migrenBotService.sendMessage(update));
        try {
            Message sentMessage = execute(sendMessage);// Отправка сообщения и сохранение отправленного сообщения
            int sentMessageId = sentMessage.getMessageId();

            // Пример удаления сообщения через 5 секунд
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    deleteBotMessage(update.getMessage().getChatId(), sentMessageId);
                }
            }, 3000);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }


    }

    public void deleteBotMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(String.valueOf(chatId));
        deleteMessage.setMessageId(messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }




}
