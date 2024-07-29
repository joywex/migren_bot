package com.example.migren.migrbot.service;

import com.example.migren.migrbot.entity.SurveyEntity;
import com.example.migren.migrbot.entity.TabletsEntity;
import com.example.migren.migrbot.entity.UsersEntity;
import com.example.migren.migrbot.repository.SurveyRepository;
import com.example.migren.migrbot.repository.TabletsRepository;
import com.example.migren.migrbot.repository.UsersRepository;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MigrenBotService {

    private final SurveyRepository surveyRepository;
    private final UsersRepository usersRepository;
    private final TabletsRepository tabletsRepository;
    private final ConcurrentHashMap<Long, String> userState;

    @Getter
    private final ConcurrentHashMap<Long, String> userQuestion;

    @Getter
    public List<Integer> msgIdsDeleteList;

    @Getter
    public List<Integer> msgIdsList;

    @Getter
    private String chatIdForDeleteMsg;

    public MigrenBotService(SurveyRepository surveyRepository, UsersRepository usersRepository, TabletsRepository tabletsRepository) {
        this.surveyRepository = surveyRepository;
        this.usersRepository = usersRepository;
        this.tabletsRepository = tabletsRepository;
        this.userQuestion = new ConcurrentHashMap<>();
        this.userState = new ConcurrentHashMap<>();
        this.msgIdsList = Collections.synchronizedList(new ArrayList<>());
        this.msgIdsDeleteList = Collections.synchronizedList(new ArrayList<>());
    }

    public SendMessage sendMessage(Update update) {
        SendMessage sendMessage = new SendMessage();
        long chatId;

        if (update.hasCallbackQuery()) {
            sendMessage = chooseCallbackData(update);
            chatId = update.getCallbackQuery().getMessage().getChatId();
        } else if (userState.containsKey(update.getMessage().getChatId())) {
            chatId = update.getMessage().getChatId();
            tabletsRepository.updateCommentBySurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, getFormatDate()), update.getMessage().getText());
            userState.remove(chatId);
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText("Ваш комментарий и запись успешно добавлены от " + getFormatDate() + ". Увидимся завтра.");
        } else {
            chatId = update.getMessage().getChatId();
            String msg = update.getMessage().getText().toLowerCase();
            switch (msg) {
                case "/start":
                    createUser(update.getMessage());
                    if (surveyRepository.findIdByChatIdAndPainDate(update.getMessage().getChatId(), getFormatDate()) == null) {
//                        sendMessage = painChoice(update);
                        sendMessage = firstMsg(update);
                    } else {
                        sendMessage.setChatId(String.valueOf(chatId));
                        sendMessage.setText("Вы уже добавляли запись сегодня. Не переживайте, я всё сохранил. Вернусь к Вам завтра с повторным опросом.");
                    }
                    break;
                case "/add_note":
                    sendMessage = noteChoice(update.getMessage());
                    break;
                default:
                    sendMessage.setChatId(String.valueOf(chatId));
                    sendMessage.setText("Воспользуйтесь одной из команд меню, чтобы использовать все доступные функции бота.");
                    break;
            }
        }
        return sendMessage;
    }

    public SendMessage firstMsg(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getMessage().getChatId()));
        sendMessage.setText("Добро пожаловать!\n\n Use /add_note to put note");
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
        chatIdForDeleteMsg = String.valueOf(chatId);
        String newLastQuestion = "";
        int msgId = update.getCallbackQuery().getMessage().getMessageId();

        switch (usersRepository.getLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId())) {
            case "":
                sendMessage = getCallBackDataPain(update);
                msgId = update.getCallbackQuery().getMessage().getMessageId();
                msgIdsList.add(msgId);
                if (surveyRepository.existsByChatIdAndPainDate(chatId, getFormatDate())) {
                    newLastQuestion = "Принимали ли Вы лекарство?";
                    usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), newLastQuestion);
                }
                break;
            case "Принимали ли Вы лекарство?":
                sendMessage = getCallBackDataTablets(update);
                msgId = update.getCallbackQuery().getMessage().getMessageId();
                msgIdsList.add(msgId);
                if (tabletsRepository.existsBySurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, getFormatDate()))) {
                    newLastQuestion = "Лекарство помогло от головной боли?";
                    usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), newLastQuestion);
                } else {
                    setQuestion(update);
                }
                break;
            case "Лекарство помогло от головной боли?":
                newLastQuestion = "Желаете оставить комментарий?\n\nЭто может быть как название лекарства, которое Вы принимали, так и описание причины и продолжительности головной боли?";
                sendMessage = getCallBackDataHelp(update);
                usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), newLastQuestion);
                msgId = update.getCallbackQuery().getMessage().getMessageId();
                msgIdsList.add(msgId);
                break;
            case "Желаете оставить комментарий?\n\nЭто может быть как название лекарства, которое Вы принимали, так и описание причины и продолжительности головной боли?":
                sendMessage = getCallBackDataComment(update);
                msgIdsDeleteList.add(msgId);
                setQuestion(update);
                msgId = update.getCallbackQuery().getMessage().getMessageId();
                msgIdsList.add(msgId);
                break;
            case "Выбор записи":
                sendMessage = getCallbackDataNoteChoice(update);
                msgIdsDeleteList.add(msgId);
                break;
            case "Выбор даты записи":
                sendMessage = getCallBackDataDateChoice(update);
                msgIdsDeleteList.add(msgId);
                break;

        }
        return sendMessage;
    }

    private SendMessage noteChoice(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(message.getChatId()));
        sendMessage.setText("Запись в дневник головной боли.\n\nЗапись на какой день Вы хотели бы сделать? Ниже нажмите на одну из кнопок в соответствии с Вашим запросом.");
        createMonthKeyboard();
        sendMessage.setReplyMarkup(createNoteKeyboard());

        usersRepository.updateLastQuestionByChatId(message.getChatId(), "Выбор записи");
        return sendMessage;
    }

    private SendMessage dateChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        sendMessage.setText("Запись на прошедшие дни.\n\nВыберите день этого месяца, где Вы хотите добавить запись о головной боли.");
        createMonthKeyboard();
        sendMessage.setReplyMarkup(createMonthKeyboard());
        return sendMessage;
    }

    public SendMessage painChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
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
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        sendMessage.setText("Лекарство помогло от головной боли?");

        createKeyboard();
        sendMessage.setReplyMarkup(createKeyboard());
        return sendMessage;
    }

    private SendMessage commentChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        sendMessage.setText("*Желаете оставить комментарий?*\n\nЭто может быть как название лекарства, которое Вы принимали, так и описание причины и продолжительности головной боли?");
        sendMessage.setParseMode("Markdown");

        createKeyboard();
        sendMessage.setReplyMarkup(createKeyboard());
        return sendMessage;
    }

    private SendMessage getCallBackDataTablets(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "1":
                TabletsEntity tabletsEntity = new TabletsEntity();
                tabletsEntity.setSurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, getFormatDate()));
                tabletsRepository.save(tabletsEntity);
                sendMessage = helpChoice(update);
                updateUserQuestionState(chatId, "Принимал лекарство");
                break;
            case "0":
                sendMessage.setText("Запись успешно добавлена от " + getFormatDate() + ". До встречи завтра!");
                updateUserQuestionState(chatId, "Не принимал лекарство");

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
                surveyEntity.setPainDate(getFormatDate());
                surveyRepository.save(surveyEntity);
                sendMessage = tabletsChoice(update);
                updateUserQuestionState(chatId, "Голова болела");
                break;
            case "0":
                sendMessage.setText("Отлично, завтра я спрошу Вас снова!");
                updateUserQuestionState(chatId, "Голова не болела");
                break;
        }
        return sendMessage;
    }

    private void updateUserQuestionState(long chatId, String lastQuestion) {
        userQuestion.put(chatId, lastQuestion);
    }


    private SendMessage getCallBackDataHelp(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "1":
                tabletsRepository.updateHelpBySurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, getFormatDate()), true);
                sendMessage = commentChoice(update);
                updateUserQuestionState(chatId, "Лекарство помогло");
                break;
            case "0":
                tabletsRepository.updateHelpBySurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, getFormatDate()), false);
                sendMessage = commentChoice(update);
                updateUserQuestionState(chatId, "Лекарство не помогло");
                break;
        }
        return sendMessage;
    }

    private SendMessage getCallBackDataComment(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "1":
                userState.put(chatId, "waiting_comment");
                sendMessage.setText("Оставьте комментарий ниже.");
                break;
            case "0":
                sendMessage.setText("Запись успешно добавлена от " + getFormatDate() + ". До встречи завтра!");
                break;
        }
        userQuestion.remove(chatId);
        return sendMessage;
    }

    private SendMessage getCallbackDataNoteChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "0":
                if (surveyRepository.findIdByChatIdAndPainDate(update.getCallbackQuery().getMessage().getChatId(), getFormatDate()) == null) {
                        sendMessage = painChoice(update);
                } else {
                    sendMessage.setText("Вы уже добавляли запись сегодня. Не переживайте, я всё сохранил. Вернусь к Вам завтра с повторным опросом.");
                }
                usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), "");
                break;
            case "1":
                sendMessage = dateChoice(update);
                usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), "Выбор даты записи");
        }
        return sendMessage;
    }

    private SendMessage getCallBackDataDateChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        String callbackData = update.getCallbackQuery().getData();

        switch (callbackData) {
            case "0":
                sendMessage.setText("Вы отменили запись на прошедшие дни. Можете создать запись снова, используя команды из меню.");
                usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), "");
        }
        return sendMessage;
    }

    private void saveComment(Long surveyId, String comment) {
        tabletsRepository.updateCommentBySurveyId(surveyId, comment);
    }

    private void setQuestion(Update update) {
        String newLastQuestion = "";
        usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), newLastQuestion);
    }

    public String getFormatDate() {
        LocalDateTime currentDate = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return currentDate.format(formatter);
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

    private InlineKeyboardMarkup createNoteKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> msgButtons = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText("Сегодняшний день");
        inlineKeyboardButton.setCallbackData("0");
        row1.add(inlineKeyboardButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
        inlineKeyboardButton1.setText("Любой предыдущий день");
        inlineKeyboardButton1.setCallbackData("1");
        row2.add(inlineKeyboardButton1);

        msgButtons.add(row1);
        msgButtons.add(row2);
        inlineKeyboardMarkup.setKeyboard(msgButtons);
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup createMonthKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> msgButtons = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        String dateStr = getFormatDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate date = LocalDate.parse(dateStr, formatter);

        for (int i = 1; i < date.getDayOfMonth(); i++) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(String.valueOf(i));
            inlineKeyboardButton.setCallbackData(String.valueOf(i));
            row.add(inlineKeyboardButton);

            if (i % 7 == 0) {
                msgButtons.add(row);
                row = new ArrayList<>();
            }
        }

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText("Отмена");
        inlineKeyboardButton.setCallbackData("0");
        row.add(inlineKeyboardButton);

        msgButtons.add(row);
        inlineKeyboardMarkup.setKeyboard(msgButtons);
        return inlineKeyboardMarkup;
    }


}
