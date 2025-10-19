package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetMe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.service.TelegramBotService;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final TelegramBot telegramBot;
    private final TelegramBotService telegramBotService;

    @Autowired
    public TelegramBotUpdatesListener(TelegramBot telegramBot, TelegramBotService telegramBotService) {
        this.telegramBot = telegramBot;
        this.telegramBotService = telegramBotService;
    }

    @PostConstruct
    public void init() {
        logger.info("=== Initializing Telegram Bot Updates Listener ===");

        try {

            telegramBot.setUpdatesListener(this);
            logger.info("✅ Updates Listener set successfully");

            var me = telegramBot.execute(new GetMe());
            if (me.isOk()) {
                logger.info("✅ Bot @{} is ready to receive messages", me.user().username());
            } else {
                logger.error("❌ Bot is not accessible: {}", me.description());
            }

        } catch (Exception e) {
            logger.error("❌ Failed to initialize bot: ", e);
        }
    }

    @Override
    public int process(List<Update> updates) {
        logger.info("=== Received {} updates ===", updates.size());

        updates.forEach(update -> {
            logger.info("Processing update ID: {}", update.updateId());

            if (update.message() != null && update.message().text() != null) {
                String messageText = update.message().text();
                Long chatId = update.message().chat().id();
                String user = update.message().from().firstName();

                logger.info("📨 Message from {} ({}): {}", user, chatId, messageText);

                if ("/start".equals(messageText)) {
                    logger.info("Handling /start command");
                    telegramBotService.handleStartCommand(chatId);
                } else {
                    logger.info("Handling reminder creation");
                    telegramBotService.handleReminderCreation(chatId, messageText);
                }
            } else {
                logger.warn("❌ Update without text message: {}", update);
            }
        });

        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}
