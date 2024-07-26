package com.example.migren.migrbot.config;

import com.example.migren.migrbot.States.UserQuestionState;
import com.example.migren.migrbot.repository.UsersRepository;
import com.example.migren.migrbot.service.MigrenBotService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class MigrenBotSetInfo extends TelegramLongPollingBot {

    private final MigrenBotConfig migrenBotConfig;
    private final MigrenBotService migrenBotService;
    private final UsersRepository usersRepository;

    private MigrenBotSetInfo(MigrenBotConfig migrenBotConfig, MigrenBotService migrenBotService, UsersRepository usersRepository) {
        this.migrenBotConfig = migrenBotConfig;
        this.migrenBotService = migrenBotService;
        this.usersRepository = usersRepository;
        initCommands();
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
            editMsgs();
            if (!migrenBotService.getMsgIdsDeleteList().isEmpty()) {
                scheduleMessageDeletion();
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void editMsgs() {
        List<Integer> msgIds = migrenBotService.getMsgIdsList();

        if (!msgIds.isEmpty()) {
            for (Map.Entry<Long, UserQuestionState> entry : migrenBotService.getUserQuestion().entrySet()) {
                long chatId = entry.getKey();
                UserQuestionState state = entry.getValue();
                String question = state.getLastQuestion();

                for (Integer messageId : msgIds) {
                    EditMessageText editMessageText = new EditMessageText();
                    editMessageText.setChatId(String.valueOf(chatId));
                    editMessageText.setMessageId(messageId);
                    editMessageText.setParseMode("Markdown");

                    switch (question) {
                        case "Голова болела":
                            editMessageText.setText("У вас сегодня болела голова?\n\n\n*Записал ответ:*\nБыла головная боль \uD83E\uDD74");
                            break;
                        case "Голова не болела":
                            editMessageText.setText("У вас сегодня болела голова?\n\n\n*Записал ответ:*\nГоловной боли не было \uD83D\uDD25");
                            break;
                        case "Принимал лекарство":
                            editMessageText.setText("Принимали ли Вы лекарство?\n\n\n*Записал ответ:*\nПринимал(а) лекарство \uD83D\uDC4D\uD83C\uDFFB");
                            break;
                        case "Не принимал лекарство":
                            editMessageText.setText("Принимали ли Вы лекарство?\n\n\n*Записал ответ:*\nНе принимал(а) лекарство \uD83D\uDC4E\uD83C\uDFFB");
                            break;
                        case "Лекарство помогло":
                            editMessageText.setText("Лекарство помогло от головной боли?\n\n\n*Записал ответ:*\nПомогло \uD83D\uDC4D\uD83C\uDFFB");
                            break;
                        case "Лекарство не помогло":
                            editMessageText.setText("Лекарство помогло от головной боли?\n\n\n*Записал ответ:*\nНе помогло \uD83D\uDC4E\uD83C\uDFFB");
                            break;
                        default:
                            editMessageText.setText("Неизвестный вопрос.");
                            break;
                    }

                    try {
                        execute(editMessageText);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
            migrenBotService.getMsgIdsList().clear();
        }
    }

//    public void editMsgs() {
//        for (Map.Entry<Long, UserQuestionState> entry : migrenBotService.getUserQuestion().entrySet()) {
//            long chatId = entry.getKey();
//            UserQuestionState state = entry.getValue();
//
//            List<Integer> msgIds = migrenBotService.getMsgIdsList();
//            if (!msgIds.isEmpty()) {
//                String question = state.getLastQuestion();
//                for (Integer messageId : msgIds) {
//                    EditMessageText editMessageText = new EditMessageText();
//                    editMessageText.setChatId(String.valueOf(chatId));
//                    editMessageText.setMessageId(messageId);
//
//                    switch (question) {
//                        case "Голова болела":
//                            editMessageText.setText("У вас сегодня болела голова?\n\n\nЗаписал ответ: была головная боль \uD83E\uDD74");
//                            break;
//                        case "Голова не болела":
//                            editMessageText.setText("У вас сегодня болела голова?\n\n\nЗаписал ответ: головной боли не было \uD83D\uDD25");
//                            break;
//                        case "Принимал лекарство":
//                            editMessageText.setText("Принимали ли Вы лекарство?\n\n\nЗаписал ответ: принимал лекарство \uD83D\uDC4D\uD83C\uDFFB");
//                            break;
//                        case "Не принимал лекарство":
//                            editMessageText.setText("Принимали ли Вы лекарство?\n\n\nЗаписал ответ: не принимал лекарство \uD83D\uDC4E\uD83C\uDFFB");
//                            break;
//                        case "Лекарство помогло":
//                            editMessageText.setText("Лекарство помогло от головной боли?\n\n\nЗаписал ответ: помогло \uD83D\uDC4D\uD83C\uDFFB");
//                            break;
//                        case "Лекарство не помогло":
//                            editMessageText.setText("Лекарство помогло от головной боли?\n\n\nЗаписал ответ: не помогло \uD83D\uDC4E\uD83C\uDFFB");
//                            break;
//                        default:
//                            editMessageText.setText("Неизвестный вопрос.");
//                            break;
//                    }
//                    try {
//                        execute(editMessageText);
//                    } catch (TelegramApiException e) {
//                        e.printStackTrace();
//                    }
//                }
//                msgIds.clear();
//            }
//        }
//    }

    private void initCommands() {
        List<BotCommand> botCommands = new ArrayList<>();
        botCommands.add(new BotCommand("/start", "Запуск бота"));
        botCommands.add(new BotCommand("/addnote", "Добавить запись о головной боли в дневник"));
        botCommands.add(new BotCommand("/mynotes", "Посмотреть записи в дневнике"));

        SetMyCommands setMyCommands = new SetMyCommands();
        setMyCommands.setCommands(botCommands);
        setMyCommands.setScope(new BotCommandScopeDefault());

        try {
            execute(setMyCommands);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
        List<Integer> msgIdsDeleteList = migrenBotService.getMsgIdsDeleteList();
        String chatId = migrenBotService.getChatIdForDeleteMsg();

        while (!msgIdsDeleteList.isEmpty()) {
            Integer messageId = msgIdsDeleteList.remove(0);

            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(messageId);

            try {
                execute(deleteMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace(); // Обработка исключений
            }
        }

        // Очищаем список после удаления всех сообщений
        migrenBotService.getMsgIdsDeleteList().clear();
    }

    public void replySheduled(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }


    }

}
