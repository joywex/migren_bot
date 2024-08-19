package com.example.migren.migrbot.service;

import com.example.migren.migrbot.entity.SurveyEntity;
import com.example.migren.migrbot.entity.TabletsEntity;
import com.example.migren.migrbot.entity.UsersEntity;
import com.example.migren.migrbot.repository.SurveyRepository;
import com.example.migren.migrbot.repository.TabletsRepository;
import com.example.migren.migrbot.repository.UsersRepository;
import com.example.migren.migrbot.utils.Utils;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MigrenBotService {

    private final SurveyRepository surveyRepository;
    private final UsersRepository usersRepository;
    private final TabletsRepository tabletsRepository;
    private final ConcurrentHashMap<Long, String> userState;
    private final ConcurrentHashMap<Long, String> userPainDate;

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
        this.userPainDate = new ConcurrentHashMap<>();
        this.userQuestion = new ConcurrentHashMap<>();
        this.userState = new ConcurrentHashMap<>();
        this.msgIdsList = Collections.synchronizedList(new ArrayList<>());
        this.msgIdsDeleteList = Collections.synchronizedList(new ArrayList<>());
    }

    //setChatId везде использовать метод из Utils
    public SendMessage sendMessage(Update update) {
        SendMessage sendMessage = new SendMessage();
        long chatId;

        if (update.hasCallbackQuery()) {
            sendMessage = chooseCallbackData(update);
            chatId = update.getCallbackQuery().getMessage().getChatId();
        } else if (userState.containsKey(update.getMessage().getChatId()) && userState.get(update.getMessage().getChatId()).equals("waiting_comment")) {
            chatId = update.getMessage().getChatId();
            tabletsRepository.updateCommentBySurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, Utils.getFormatDate()), update.getMessage().getText());
            userState.remove(chatId);
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText("Ваш комментарий и запись успешно добавлены от " + userPainDate.get(chatId) + ". Увидимся завтра.");
            userPainDate.remove(chatId);
        } else if (userState.containsKey(update.getMessage().getChatId()) && userState.get(update.getMessage().getChatId()).equals("waiting_feedback")) {
            chatId = update.getMessage().getChatId();
            usersRepository.updateFeedbackByChatId(chatId, update.getMessage().getText());
            userState.remove(chatId);
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText("Благодарим за обращение! Мы записали Ваш отзыв.");
        } else {
            chatId = update.getMessage().getChatId();
            String msg = update.getMessage().getText().toLowerCase();
            switch (msg) {
                case "/start":
                    createUser(update.getMessage());
                    sendMessage = Utils.firstMsg(update);
                    break;
                case "/add_note":
                    sendMessage = noteChoice(update);
                    break;
                case "/my_notes":
                    sendMessage = painDateCalendar(update);
                    break;
                case "/feedback":
                    userState.put(update.getMessage().getChatId(), "waiting_feedback");
                    sendMessage = Utils.feedbackMsg(update);
                    break;
                default:
                    sendMessage.setChatId(String.valueOf(chatId));
                    sendMessage.setText("Воспользуйтесь одной из команд меню, чтобы использовать все доступные функции бота.");
                    break;
            }
        }
        return sendMessage;
    }

    //удалить если не нужен
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
                sendMessage = getCallBackDataPain(update, userPainDate.get(chatId));
                msgId = update.getCallbackQuery().getMessage().getMessageId();
                msgIdsList.add(msgId);
                if (surveyRepository.existsByChatIdAndPainDate(chatId, userPainDate.get(chatId))) {
                    newLastQuestion = "Принимали ли Вы лекарство?";
                    usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), newLastQuestion);
                }
                break;
            case "Принимали ли Вы лекарство?":
                sendMessage = getCallBackDataTablets(update);
                msgId = update.getCallbackQuery().getMessage().getMessageId();
                msgIdsList.add(msgId);
                if (tabletsRepository.existsBySurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, userPainDate.get(chatId)))) {
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
            case "Календарь":
                sendMessage = getCallBackDataCalendar(update);
                setQuestion(update);
                msgIdsDeleteList.add(msgId);
                break;

        }
        return sendMessage;
    }

    private SendMessage noteChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getMessage().getChatId()));
        sendMessage.setText("*Запись в дневник головной боли.*\n\nЗапись на какой день Вы хотели бы сделать? Ниже нажмите на одну из кнопок в соответствии с Вашим запросом.");
        sendMessage.setParseMode("Markdown");
        createMonthKeyboard(update);
        sendMessage.setReplyMarkup(Utils.createNoteKeyboard());

        usersRepository.updateLastQuestionByChatId(update.getMessage().getChatId(), "Выбор записи");
        return sendMessage;
    }

    private SendMessage painDateCalendar(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getMessage().getChatId()));
        sendMessage.setText("*Ваш календарь головной боли за этот месяц.*\n\nВсе кнопки носят информативный характер. В них не заложены какие-либо функции.\n\n \uD83D\uDD3A _- отметка о наличии в этот день головной боли_");
        sendMessage.setParseMode("Markdown");
        createPainDateKeyboard(update);
        sendMessage.setReplyMarkup(createPainDateKeyboard(update));

        usersRepository.updateLastQuestionByChatId(update.getMessage().getChatId(), "Календарь");
        return sendMessage;
    }

    private SendMessage dateChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        sendMessage.setText("*Запись на прошедшие дни.*\n\nВыберите день этого месяца, где Вы хотите добавить запись о головной боли.\n\n \uD83D\uDD3A _- отметка о наличии в этот день головной боли_");
        sendMessage.setParseMode("Markdown");
        createMonthKeyboard(update);
        sendMessage.setReplyMarkup(createMonthKeyboard(update));
        return sendMessage;
    }

    public SendMessage painChoice(Update update, String date) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(Utils.getChatId(update)));
        sendMessage.setText("У вас болела голова?\n\nЗапись создаётся на " + date);
        Utils.createKeyboard();
        sendMessage.setReplyMarkup(Utils.createKeyboard());
        userPainDate.put(Utils.getChatId(update), date);
        return sendMessage;
    }

    private SendMessage tabletsChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        sendMessage.setText("Принимали ли Вы лекарство?");

        Utils.createKeyboard();
        sendMessage.setReplyMarkup(Utils.createKeyboard());
        return sendMessage;
    }

    private SendMessage helpChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        sendMessage.setText("Лекарство помогло от головной боли?");

        Utils.createKeyboard();
        sendMessage.setReplyMarkup(Utils.createKeyboard());
        return sendMessage;
    }

    private SendMessage commentChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        sendMessage.setText("*Желаете оставить комментарий?*\n\nЭто может быть как название лекарства, которое Вы принимали, так и описание причины и продолжительности головной боли?");
        sendMessage.setParseMode("Markdown");

        Utils.createKeyboard();
        sendMessage.setReplyMarkup(Utils.createKeyboard());
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
                tabletsEntity.setSurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, Utils.getFormatDate()));
                tabletsRepository.save(tabletsEntity);
                sendMessage = helpChoice(update);
                updateUserQuestionState(chatId, "Принимал лекарство");
                break;
            case "0":
                sendMessage.setText("Запись успешно добавлена от " + userPainDate.get(chatId) + ". До встречи завтра!");
                userPainDate.remove(chatId);
                setQuestion(update);
                updateUserQuestionState(chatId, "Не принимал лекарство");
                break;

        }
        return sendMessage;
    }

    private SendMessage getCallBackDataPain(Update update, String date) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "1":
                SurveyEntity surveyEntity = new SurveyEntity();
                surveyEntity.setChatId(update.getCallbackQuery().getMessage().getChatId());
                surveyEntity.setPainDate(date);
                surveyRepository.save(surveyEntity);
                sendMessage = tabletsChoice(update);
                updateUserQuestionState(chatId, "Голова болела");
                break;
            case "0":
                setQuestion(update);
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
                tabletsRepository.updateHelpBySurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, Utils.getFormatDate()), true);
                sendMessage = commentChoice(update);
                updateUserQuestionState(chatId, "Лекарство помогло");
                break;
            case "0":
                tabletsRepository.updateHelpBySurveyId(surveyRepository.findIdByChatIdAndPainDate(chatId, Utils.getFormatDate()), false);
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
                sendMessage.setText("Запись успешно добавлена от " + userPainDate.get(chatId) + ". До встречи завтра!");
                userPainDate.remove(chatId);
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
                if (surveyRepository.findIdByChatIdAndPainDate(update.getCallbackQuery().getMessage().getChatId(), Utils.getFormatDate()) == null) {
                    sendMessage = painChoice(update, Utils.getFormatDate());
                } else {
                    sendMessage.setText("Вы уже добавляли запись сегодня. Не переживайте, я всё сохранил. Вернусь к Вам завтра с повторным опросом.");
                }
                usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), "");
                break;
            case "1":
                if (surveyRepository.findIdByChatIdAndPainDate(update.getCallbackQuery().getMessage().getChatId(), Utils.getPreviousDateFormat()) == null) {
                    sendMessage = painChoice(update, Utils.getPreviousDateFormat());
                } else {
                    sendMessage.setText("Вы уже добавляли запись на вчерашнее число. Не переживайте, я всё сохранил. Вернусь к Вам завтра с повторным опросом.");
                }
                usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), "");
                break;
            case "2":
                sendMessage = dateChoice(update);
                usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), "Выбор даты записи");
                break;
        }
        return sendMessage;
    }

    private SendMessage getCallBackDataDateChoice(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        String callbackData = update.getCallbackQuery().getData();


        // заменить на if
        switch (callbackData) {
            case "0":
                sendMessage.setText("Вы отменили запись на прошедшие дни. Можете создать запись снова, используя команды из меню.");
                usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), "");
                break;
            default:
                int dayOfMonth = Integer.parseInt(callbackData);
                LocalDate currentDate = LocalDate.now();
                LocalDate selectDay = currentDate.withDayOfMonth(dayOfMonth);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                String formattedDate = selectDay.format(formatter);

                if (!surveyRepository.existsByChatIdAndPainDate(Utils.getChatId(update), formattedDate)) {
                    sendMessage = painChoice(update, formattedDate);
                    usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), "");
                    userPainDate.put(Utils.getChatId(update), formattedDate);
                } else {
                    sendMessage.setText("Запись о наличии головной боли на выбранный день уже существует.");
                    setQuestion(update);
                }
                break;
        }
        return sendMessage;
    }

    public SendMessage getCallBackDataCalendar(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        String callbackData = update.getCallbackQuery().getData();
        usersRepository.updateLastQuestionByChatId(update.getCallbackQuery().getMessage().getChatId(), "");

        switch (callbackData) {
            case "333":
                sendMessage.setText("Кнопки в разделе просмотра календаря исключительно нформативные. В них не заложены какие-либо функции.");
                setQuestion(update);
                break;
            case "0":
                sendMessage.setText("Календарь закрыт.");
                break;
            default:
                setQuestion(update);
                break;
        }
        return sendMessage;
    }

    public List<EditMessageText> addEdits() {
        List<EditMessageText> editMessages = new ArrayList<>();


        // заменить userQuestion и msgIdList на потокобезопасный List с объектами класса, который хранит поля chatId, question, messageId
        if (!msgIdsList.isEmpty()) {
            for (Map.Entry<Long, String> entry : userQuestion.entrySet()) {
                long chatId = entry.getKey();
                String question = entry.getValue();

                for (Integer messageId : msgIdsList) {
                    EditMessageText editMessageText = new EditMessageText();
                    editMessageText.setChatId(String.valueOf(chatId));
                    editMessageText.setMessageId(messageId);
                    editMessageText.setParseMode("Markdown");

                    switch (question) {
                        case "Голова болела":
                            editMessageText.setText("У вас болела голова?\n\n\n*Записал ответ:*\nБыла головная боль \uD83E\uDD74");
                            break;
                        case "Голова не болела":
                            editMessageText.setText("У вас болела голова?\n\n\n*Записал ответ:*\nГоловной боли не было \uD83D\uDD25");
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
                    editMessages.add(editMessageText);
                }
            }
        }
        return editMessages;
    }

    //если не используешь то удаляй
    private void saveComment(Long surveyId, String comment) {
        tabletsRepository.updateCommentBySurveyId(surveyId, comment);
    }

    private void setQuestion(Update update) {
        String newLastQuestion = "";
        usersRepository.updateLastQuestionByChatId(Utils.getChatId(update), newLastQuestion);
    }

    private void createUser(Message message) {
        if (usersRepository.hasUserByChatId(message.getChatId()).isEmpty()) {
            UsersEntity usersEntity = new UsersEntity();
            usersEntity.setChatId(message.getChatId());
            usersEntity.setLastQuestion("");
            usersRepository.save(usersEntity);
        }
    }

    private InlineKeyboardMarkup createMonthKeyboard(Update update) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> msgButtons = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        String dateStr = Utils.getFormatDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate date = LocalDate.parse(dateStr, formatter);


        for (int i = 1; i <= date.getDayOfMonth(); i++) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            // метод для фикса ошибки следом
            if (surveyRepository.existsByChatIdAndPainDate(Utils.getChatId(update), Utils.dateConverter(i))) {
                inlineKeyboardButton.setText(i + "\uD83D\uDD3A");
            } else {
                inlineKeyboardButton.setText(String.valueOf(i));
            }
            inlineKeyboardButton.setCallbackData(String.valueOf(i));
            row.add(inlineKeyboardButton);

            if (i % 7 == 0) {
                msgButtons.add(row);
                row = new ArrayList<>();
            }
        }

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("Отмена");
        cancelButton.setCallbackData("0");
        cancelRow.add(cancelButton);

        msgButtons.add(row);
        msgButtons.add(cancelRow);
        inlineKeyboardMarkup.setKeyboard(msgButtons);
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup createPainDateKeyboard(Update update) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> msgButtons = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        String dateStr = Utils.getFormatDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate date = LocalDate.parse(dateStr, formatter);

        for (int i = 1; i <= date.getDayOfMonth(); i++) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            if (surveyRepository.existsByChatIdAndPainDate(Utils.getChatId(update), Utils.dateConverter(i))) {
                inlineKeyboardButton.setText(i + "\uD83D\uDD3A");
            } else {
                inlineKeyboardButton.setText(String.valueOf(i));
            }
            inlineKeyboardButton.setCallbackData("333");
            row.add(inlineKeyboardButton);

            if (i % 7 == 0) {
                msgButtons.add(row);
                row = new ArrayList<>();
            }
        }

        List<InlineKeyboardButton> finishRow = new ArrayList<>();
        InlineKeyboardButton finishButton = new InlineKeyboardButton();
        finishButton.setText("Закрыть");
        finishButton.setCallbackData("0");
        finishRow.add(finishButton);

        msgButtons.add(row);
        msgButtons.add(finishRow);
        inlineKeyboardMarkup.setKeyboard(msgButtons);
        return inlineKeyboardMarkup;
    }

}
