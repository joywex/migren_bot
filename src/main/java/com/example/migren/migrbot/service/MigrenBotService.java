package com.example.migren.migrbot.service;

import com.example.migren.migrbot.entity.SurveyEntity;
import com.example.migren.migrbot.repository.SurveyRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class MigrenBotService {

    private final SurveyRepository surveyRepository;

    public MigrenBotService(SurveyRepository surveyRepository) {
        this.surveyRepository = surveyRepository;
    }

    public SendMessage sendMessage(Message message) {
        SendMessage sendMessage = new SendMessage();

        switch (message.getText().toLowerCase()) {
            case "/start":
//                sendMessage.setText(firstMsg());
                sendMessage = painChoice(message);

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
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> msgButtons = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText("da");
        inlineKeyboardButton.setCallbackData("0");
        row.add(inlineKeyboardButton);
        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
        inlineKeyboardButton1.setText("net");
        inlineKeyboardButton1.setCallbackData("1");
        row.add(inlineKeyboardButton1);
        msgButtons.add(row);
        inlineKeyboardMarkup.setKeyboard(msgButtons);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
//        SurveyEntity surveyEntity = new SurveyEntity();
//        surveyEntity.setChat_id(message.getChatId());
        return sendMessage;
    }

}
