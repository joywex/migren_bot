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
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MigrenBotService {

    private final SurveyRepository surveyRepository;
    private final UsersRepository usersRepository;
    private final TabletsRepository tabletsRepository;
    private final UserCommentState userCommentState;
    private ConcurrentHashMap<Long, UserCommentState> userState;
    public List<Integer> msgIdsList;

    public String getChatIdForDeleteMsg() {
        return chatIdForDeleteMsg;
    }

    private String chatIdForDeleteMsg;

    public MigrenBotService(SurveyRepository surveyRepository, UsersRepository usersRepository, TabletsRepository tabletsRepository, UserCommentState userCommentState) {
        this.surveyRepository = surveyRepository;
        this.usersRepository = usersRepository;
        this.tabletsRepository = tabletsRepository;
        this.userCommentState = userCommentState;
        this.userState = new ConcurrentHashMap<>();
        this.msgIdsList = new ArrayList<>();
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
                        sendMessage = painChoice(update.getMessage());
                    } else {
                        sendMessage.setChatId(String.valueOf(chatId));
                        sendMessage.setText("Вы уже добавляли запись сегодня. Не переживайте, я всё сохранил. Вернусь к Вам завтра с повторным опросом.");
                    }
                    break;
                default:
                    sendMessage.setChatId(String.valueOf(chatId));
                    sendMessage.setText("Нажмите на одну из кнопок.");
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
                if (tabletsRepository.existsBySurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, getFormatDate()))) {
                    newLastQuestion = "Лекарство помогло от головной боли?";
                    usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), newLastQuestion);
                } else {
                    setQuestion(update);
                }
                msgId = update.getCallbackQuery().getMessage().getMessageId();
                msgIdsList.add(msgId);
                break;
            case "Лекарство помогло от головной боли?":
                newLastQuestion = "Жалете оставить комментарий?\n Это может быть как название лекарства, которое Вы принимали, так и описание причины и продолжительности головной боли?";
                sendMessage = getCallBackDataHelp(update);
                usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), newLastQuestion);
                msgId = update.getCallbackQuery().getMessage().getMessageId();
                msgIdsList.add(msgId);
                break;
            case "Жалете оставить комментарий?\n Это может быть как название лекарства, которое Вы принимали, так и описание причины и продолжительности головной боли?":
                sendMessage = getCallBackDataComment(update);
                setQuestion(update);
                msgId = update.getCallbackQuery().getMessage().getMessageId();
                msgIdsList.add(msgId);
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
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        sendMessage.setText("Лекарство помогло от головной боли?");

        createKeyboard();
        sendMessage.setReplyMarkup(createKeyboard());
        return sendMessage;
    }

    private SendMessage commentChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        sendMessage.setText("Жалете оставить комментарий?\n Это может быть как название лекарства, которое Вы принимали, так и описание причины и продолжительности головной боли?");

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
                break;
            case "0":
                sendMessage.setText("Запись успешно добавлена от " + getFormatDate() + ". До встречи завтра!");

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
                break;
            case "0":
                sendMessage.setText("Отлично, завтра я спрошу Вас снова!");
                break;
        }
        return sendMessage;
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
                break;
            case "0":
                tabletsRepository.updateHelpBySurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, getFormatDate()), false);
                sendMessage = commentChoice(update);
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
                UserCommentState userCommentState = new UserCommentState();
                userCommentState.setCurrentState("waiting_comment");
                userState.put(chatId, userCommentState);
                sendMessage.setText("Оставьте комментарий ниже или нажмите `Отмена`.");
                break;
            case "0":
                sendMessage.setText("Запись успешно добавлена от " + getFormatDate() + ". До встречи завтра!");
                break;
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

    private String getFormatDate() {
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


}
