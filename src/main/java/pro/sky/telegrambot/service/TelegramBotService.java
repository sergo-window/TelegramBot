package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotService {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotService.class);

    private final NotificationTaskRepository taskRepository;
    private final TelegramBot telegramBot;

    private static final Pattern PATTERN = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public TelegramBotService(NotificationTaskRepository taskRepository, TelegramBot telegramBot) {
        this.taskRepository = taskRepository;
        this.telegramBot = telegramBot;
    }

    public void handleStartCommand(Long chatId) {
        String welcomeMessage = "Добро пожаловать! 🤖\nЯ бот для создания напоминаний.\n\n" +
                "Для создания напоминания отправьте сообщение в формате:\n" +
                "dd.MM.yyyy HH:mm Текст напоминания\n\n" +
                "Примеры:\n" +
                "• 25.12.2024 15:30 Поздравить с Новым годом\n" +
                "• 01.01.2024 00:00 Встретить Новый год";

        sendMessage(chatId, welcomeMessage);
        logger.info("Sent welcome message to chat: {}", chatId);
    }

    public void handleReminderCreation(Long chatId, String messageText) {
        logger.info("Attempting to create reminder. Chat: {}, Message: {}", chatId, messageText);

        try {
            NotificationTask task = parseAndCreateTask(chatId, messageText);
            logger.info("Parsed task - DateTime: {}, Text: {}",
                    task.getNotificationDateTime(), task.getMessageText());

            NotificationTask savedTask = taskRepository.save(task);
            logger.info("✅ Task saved to DB with ID: {}", savedTask.getId());

            String successMessage = "✅ Напоминание создано!\n" +
                    "📅 Дата: " + savedTask.getNotificationDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + "\n" +
                    "📝 Текст: " + savedTask.getMessageText();

            sendMessage(chatId, successMessage);

        } catch (IllegalArgumentException e) {
            logger.error("❌ Failed to parse message: {}", e.getMessage());
            String errorMessage = "❌ Не удалось создать напоминание. " + e.getMessage();
            sendMessage(chatId, errorMessage);
        } catch (Exception e) {
            logger.error("❌ Unexpected error: ", e);
            sendMessage(chatId, "❌ Произошла непредвиденная ошибка");
        }
    }

    @Scheduled(cron = "0 * * * * *")
    public void sendScheduledNotifications() {
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<NotificationTask> tasks = taskRepository.findTasksByExactDateTime(currentTime);

        logger.info("Found {} tasks for time: {}", tasks.size(), currentTime);

        for (NotificationTask task : tasks) {
            try {
                String reminderMessage = "🔔 Напоминание!\n" + task.getMessageText();
                sendMessage(task.getChatId(), reminderMessage);

                task.markAsSent();
                taskRepository.save(task);

                logger.info("Notification sent for task: {}", task.getId());

            } catch (Exception e) {
                logger.error("Failed to send notification for task {}: {}", task.getId(), e.getMessage());
                task.markAttempt();
                taskRepository.save(task);
            }
        }
    }

    private NotificationTask parseAndCreateTask(Long chatId, String message) {
        Matcher matcher = PATTERN.matcher(message);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Некорректный формат сообщения. Ожидается: dd.MM.yyyy HH:mm Текст напоминания");
        }

        String dateTimeString = matcher.group(1);
        String reminderText = matcher.group(3);

        LocalDateTime notificationDateTime = LocalDateTime.parse(dateTimeString, FORMATTER);

        if (notificationDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Дата напоминания не может быть в прошлом");
        }

        return new NotificationTask(chatId, reminderText, notificationDateTime);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        telegramBot.execute(message);
    }
}