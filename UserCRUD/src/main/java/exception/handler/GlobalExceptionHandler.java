package exception.handler;

import exception.ApplicationException;
import exception.DataAccessException;
import exception.UniqueConstraintViolationException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static <T> T handle(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (UniqueConstraintViolationException e) {
            logger.warn("Нарушение уникальности в БД: constraint={}", e.getConstraintName());
            throw new ApplicationException("Данные уже существуют", 409);
        } catch (EntityNotFoundException e) {
            logger.warn("Объект не найден: {}", e.getMessage());
            throw new ApplicationException("Запрашиваемый ресурс не найден", 404);
        } catch (DataAccessException e) {
            logger.error("Ошибка уровня доступа к данным (Hibernate/JDBC): ", e);
            throw new ApplicationException("Внутренняя ошибка базы данных", 500);
        } catch (Exception e) {
            logger.error("Непредвиденное исключение: ", e);
            throw new ApplicationException("Произошла системная ошибка", 500);
        }
    }
}
