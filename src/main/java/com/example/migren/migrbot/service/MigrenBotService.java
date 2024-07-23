package com.example.migren.migrbot.service;

import com.example.migren.migrbot.entity.SurveyEntity;
import com.example.migren.migrbot.entity.TabletsEntity;
import com.example.migren.migrbot.entity.UsersEntity;
import com.example.migren.migrbot.repository.SurveyRepository;
import com.example.migren.migrbot.repository.TabletsRepository;
import com.example.migren.migrbot.repository.UsersRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class MigrenBotService {

    private final SurveyRepository surveyRepository;
    private final UsersRepository usersRepository;
    private final TabletsRepository tabletsRepository;

    public MigrenBotService(SurveyRepository surveyRepository, UsersRepository usersRepository, TabletsRepository tabletsRepository) {
        this.surveyRepository = surveyRepository;
        this.usersRepository = usersRepository;
        this.tabletsRepository = tabletsRepository;
    }

    public SendMessage sendMessage(Update update) {
        SendMessage sendMessage = new SendMessage();
        if (update.hasCallbackQuery()) {
            sendMessage = chooseCallbackData(update);
        } else {
            switch (update.getMessage().getText().toLowerCase()) {
                case "/start":
                    createUser(update.getMessage());
                    sendMessage = painChoice(update.getMessage());
                    break;
            }
        }
        return sendMessage;
    }

    public SendMessage firstMsg(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getMessage().getChatId()));
        sendMessage.setText("Добро пожаловать!");
        return sendMessage;
    }

    public boolean hasUser(Message message) {
        if (message == null) {
            return true;
        }
        return usersRepository.hasUserByChatId(message.getChatId()).isPresent();
    }

    private SendMessage chooseCallbackData(Update update) {
        SendMessage sendMessage = new SendMessage();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String newLastQuestion = "";

        switch (usersRepository.getLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId())) {
            case "":
                sendMessage = getCallBackDataPain(update);
                newLastQuestion = "Принимали ли Вы лекарство?";
                usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), newLastQuestion);
                break;
            case "Принимали ли Вы лекарство?":
                sendMessage = getCallBackDataTablets(update);
                newLastQuestion = "Лекарство помогло от головной боли?";
                usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), newLastQuestion);
                break;
        }
        return sendMessage;
    }

    private SendMessage painChoice(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(message.getChatId()));
        sendMessage.setText("У вас сегодня болела голова?");

        createKeyboard();
        sendMessage.setReplyMarkup(createKeyboard());
        return sendMessage;
    }

    private SendMessage tabletsChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        sendMessage.setText("Принимали ли Вы лекарство?");

        createKeyboard();
        sendMessage.setReplyMarkup(createKeyboard());
        return sendMessage;
    }

    private SendMessage helpChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getMessage().getChatId()));
        sendMessage.setText("Лекарство помогло от головной боли?");

        createKeyboard();
        sendMessage.setReplyMarkup(createKeyboard());
        return sendMessage;
    }

//    private SendMessage getCallBackDataHelp(Update update) {
//        SendMessage sendMessage = new SendMessage();
//        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
//        String callbackData = update.getCallbackQuery().getData();
//        long chatId = update.getCallbackQuery().getMessage().getChatId();
//
//        switch (callbackData) {
//            case "1":
//                TabletsEntity
//                break;
//            case "0":
//                sendMessage.setText("Запись успешно добавлена. До встречи завтра!");
//
//        }
//        return sendMessage;
//    }

    private SendMessage getCallBackDataTablets(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "1":
                TabletsEntity tabletsEntity = new TabletsEntity();
                LocalDateTime currentDate = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                String formattedDate = currentDate.format(formatter);
                tabletsEntity.setSurveyId(surveyRepository.findIdByPainDate(formattedDate));
                tabletsRepository.save(tabletsEntity);
                sendMessage.setText("Введите название лекарства:");
                break;
            case "0":
                sendMessage.setText("Запись успешно добавлена. До встречи завтра!");

        }
        return sendMessage;
    }

    private SendMessage getCallBackDataPain(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "1":
                SurveyEntity surveyEntity = new SurveyEntity();
                surveyEntity.setChatId(update.getCallbackQuery().getMessage().getChatId());
                LocalDateTime currentDate = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                String formattedDate = currentDate.format(formatter);
                surveyEntity.setPainDate(formattedDate);
                surveyRepository.save(surveyEntity);

                sendMessage = tabletsChoice(update);
                break;
            case "0":
                sendMessage.setText("Отлично, рад за Вас!");
                break;
        }
        return sendMessage;
    }

    private void createUser(Message message) {
        if (usersRepository.hasUserByChatId(message.getChatId()).isEmpty()) {
            UsersEntity usersEntity = new UsersEntity();
            usersEntity.setChatId(message.getChatId());
            usersEntity.setLastQuestion("");
            usersRepository.save(usersEntity);
        }
    }

    private InlineKeyboardMarkup createKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> msgButtons = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText("Да");
        inlineKeyboardButton.setCallbackData("1");
        row.add(inlineKeyboardButton);
        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
        inlineKeyboardButton1.setText("Нет");
        inlineKeyboardButton1.setCallbackData("0");
        row.add(inlineKeyboardButton1);
        msgButtons.add(row);
        inlineKeyboardMarkup.setKeyboard(msgButtons);
        return inlineKeyboardMarkup;
    }



}
