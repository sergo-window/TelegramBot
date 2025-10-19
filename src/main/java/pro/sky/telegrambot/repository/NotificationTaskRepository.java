package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.sky.telegrambot.model.NotificationTask;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

    List<NotificationTask> findByNotificationDateTimeLessThanEqualAndIsSentFalse(LocalDateTime dateTime);

    List<NotificationTask> findByChatId(Long chatId);

    List<NotificationTask> findByIsSentFalse();

    @Query("SELECT nt FROM NotificationTask nt " +
            "WHERE nt.notificationDateTime = :dateTime " +
            "AND nt.isSent = false")
    List<NotificationTask> findTasksByExactDateTime(@Param("dateTime") LocalDateTime dateTime);
}
