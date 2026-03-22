package exception.handler;

import exception.DataAccessException;
import exception.UniqueConstraintViolationException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static void handle(Runnable action) {
        try {
            action.run();
        } catch (UniqueConstraintViolationException e) {
            logger.warn("Нарушение уникальности: {}", e.getConstraintName());
            System.err.println("Ошибка: Запись с такими данными уже существует.");
        } catch (EntityNotFoundException e) {
            logger.warn("Сущность не найдена: {}", e.getMessage());
            System.err.println("Ошибка: Данные не найдены.");
        } catch (DataAccessException e) {
            logger.error("Критический сбой данных: ", e);
            System.err.println("Внутренняя ошибка системы.");
        } catch (Exception e) {
            logger.error("Непредвиденная системная ошибка: ", e);
            System.err.println("Произошла неизвестная ошибка.");
        }
    }
}
