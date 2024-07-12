package com.example.migren.migrbot.service;

import com.example.migren.migrbot.entity.SurveyEntity;
import com.example.migren.migrbot.entity.TabletsEntity;
import com.example.migren.migrbot.entity.UsersEntity;
import com.example.migren.migrbot.repository.SurveyRepository;
import com.example.migren.migrbot.repository.TabletsRepository;
import com.example.migren.migrbot.repository.UsersRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.swing.text.DateFormatter;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class MigrenBotService {

    private final SurveyRepository surveyRepository;
    private final UsersRepository usersRepository;
    private final TabletsRepository tabletsRepository;
    private String datePain;

    public MigrenBotService(SurveyRepository surveyRepository, UsersRepository usersRepository, TabletsRepository tabletsRepository) {
        this.surveyRepository = surveyRepository;
        this.usersRepository = usersRepository;
        this.tabletsRepository = tabletsRepository;
    }

    public SendMessage sendMessage(Update update) {
        SendMessage sendMessage = new SendMessage();
        if (update.getMessage() == null) {
            sendMessage = getCallBackDataPain(update);
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

    private String firstMsg() {
        return "Добро пожаловать!";
    }

    private SendMessage painChoice(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(message.getChatId()));
        sendMessage.setText("У вас болела голова?");

        createKeyboard();
        sendMessage.setReplyMarkup(createKeyboard());
        return sendMessage;
    }

    private SendMessage tabletsChoice(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(message.getChatId()));
        sendMessage.setText("Принимали ли Вы лекартство?");

        createKeyboard();
        sendMessage.setReplyMarkup(createKeyboard());
        return sendMessage;
    }

    private void createUser(Message message) {
        if (usersRepository.hasUserByChatId(message.getChatId()).isEmpty()) {
            UsersEntity usersEntity = new UsersEntity();
            usersEntity.setChatId(message.getChatId());
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

    private SendMessage getCallBackDataTablets(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "1":
                TabletsEntity tabletsEntity = new TabletsEntity();
                tabletsEntity.setSurveyId(surveyRepository.findIdByPainDate(datePain));

        }

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
                datePain = formattedDate;
                surveyRepository.save(surveyEntity);
                sendMessage.setText("Запись успешно добавлена.");
                break;
            case "0":
                sendMessage.setText("Отлично, рад за Вас!");
                break;
        }
        return sendMessage;
    }

}
