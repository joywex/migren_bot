package com.example.migren.migrbot.config;

import com.example.migren.migrbot.service.MigrenBotService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
            if (!migrenBotService.hasUser(update.getMessage())) {
                execute(this.migrenBotService.firstMsg(update));
            }
            execute(this.migrenBotService.sendMessage(update));
            scheduleMessageDeletion();
//            deleteAllMessages();
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void scheduleMessageDeletion() {
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
                deleteAllMessages();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        });
    }

    public void deleteAllMessages() {
        if (!migrenBotService.getMsgIdsList().isEmpty()) {
            for (Integer messageId : migrenBotService.getMsgIdsList()) {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(migrenBotService.getChatIdForDeleteMsg());
                deleteMessage.setMessageId(messageId);

                try {
                    execute(deleteMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace(); // Обработка исключений
                }
            }
            migrenBotService.msgIdsList.clear(); // Очистка списка после удаления всех сообщений
        }
    }

//    public void deleteAllMessages() {
//        if (!migrenBotService.msgIdsList.isEmpty()) {
//            for (Integer messageId : migrenBotService.msgIdsList) {
//                DeleteMessage deleteMessage = new DeleteMessage();
//                deleteMessage.setChatId(migrenBotService.getChatIdForDeleteMsg());
//                deleteMessage.setMessageId(messageId);
//
//                try {
//                    execute(deleteMessage);
//                } catch (TelegramApiException e) {
//                    e.printStackTrace(); // Обработка исключений
//                }
//            }
//            migrenBotService.msgIdsList.clear(); // Очистка списка после удаления всех сообщений
//        }
//    }

//    public void replySheduled(String chatId, String message) {
//        try {
//            SendMessage sendMessage = new SendMessage();
//            sendMessage.setChatId(chatId);
//            sendMessage.setText(message);
////            sendMessage.setParseMode("MarkdownV2");
//            execute(sendMessage);
//
//        } catch (TelegramApiException e) {
//            System.out.println(e.getMessage());
//        }
//    }




}
