package com.example.migren.migrbot.utils;

import com.example.migren.migrbot.entity.UsersEntity;
import com.example.migren.migrbot.repository.SurveyRepository;
import com.example.migren.migrbot.repository.UsersRepository;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class Utils {


    public String dateConverter(int day) {
        LocalDate currentDate = LocalDate.now();
        LocalDate newDate = currentDate.withDayOfMonth(day);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return newDate.format(formatter);
    }

    public Long getChatId(Update update) {
        Long chatId;
        if (update.getMessage() == null) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        } else {
            chatId = update.getMessage().getChatId();
        }
        return chatId;
    }

    public InlineKeyboardMarkup createKeyboard() {
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

    public InlineKeyboardMarkup createNoteKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> msgButtons = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton buttonToday = new InlineKeyboardButton();
        buttonToday.setText("Сегодняшний день");
        buttonToday.setCallbackData("0");
        row1.add(buttonToday);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton buttonYesterday = new InlineKeyboardButton();
        buttonYesterday.setText("Вчерашний день");
        buttonYesterday.setCallbackData("1");
        row2.add(buttonYesterday);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton buttonAnyPrevious = new InlineKeyboardButton();
        buttonAnyPrevious.setText("Любой предыдущий день");
        buttonAnyPrevious.setCallbackData("2");
        row3.add(buttonAnyPrevious);

        msgButtons.add(row1);
        msgButtons.add(row2);
        msgButtons.add(row3);

        inlineKeyboardMarkup.setKeyboard(msgButtons);

        return inlineKeyboardMarkup;
    }

    public String getFormatDate() {
        LocalDateTime currentDate = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return currentDate.format(formatter);
    }

    public String getPreviousDateFormat() {
        LocalDate currentDate = LocalDate.now();
        LocalDate previousDate = currentDate.minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return previousDate.format(formatter);
    }

    public SendMessage firstMsg(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getMessage().getChatId()));
        sendMessage.setText("Добро пожаловать в бота!\n\nТы можешь вести дневник головной боли, добавляя записи и комментарии каждый день.\n\nСписок доступных команд:" +
                "\n/add_note - создать запись в дневнике головной боли\n/my_notes - отобразить записи в дневнике\n/feedback - оставить отзыв/пожелание по работе бота\n\nЛюбую из этих команд Вы" +
                "всегда можете найти и вызвать из 'Меню' слева.\n\nСпасибо, что выбрали нас!");
        return sendMessage;
    }

    public SendMessage feedbackMsg(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getMessage().getChatId()));
        sendMessage.setText("Введите ваш или пожелание о работе бота:");
        return sendMessage;
    }


//    public boolean hasUser(Message message) {
//        if (message == null) {
//            return true;
//        }
//        return usersRepository.hasUserByChatId(message.getChatId()).isPresent();
//    }

    //    private void saveComment(Long surveyId, String comment) {
//        tabletsRepository.updateCommentBySurveyId(surveyId, comment);
//    }



}
