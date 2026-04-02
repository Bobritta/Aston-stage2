package service;

import exception.DataAccessException;
import exception.UniqueConstraintViolationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import mapper.UserMapper;
import model.UserEntity;
import model.UserCreateDTO;
import model.UserResponseDTO;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.UserRepository;
import repository.UserRepositoryImpl;
import util.HibernateUtil;
import util.ValidationUtil;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository = new UserRepositoryImpl();
    private final UserMapper userMapper = UserMapper.INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private <R> R inTransaction(String operationName, Supplier<R> action) {
        log.debug("Старт транзакции: {}", operationName);
        Transaction transaction = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            R result = action.get();
            transaction.commit();
            logger.debug("Транзакция '{}' успешно завершена", operationName);
            return result;
        } catch (ConstraintViolationException e) {
            if (transaction != null) transaction.rollback();
            logger.warn("Нарушение ограничений в '{}': SQLState={}, Constraint={}",
                    operationName, e.getSQLState(), e.getConstraintName());
            if ("23505".equals(e.getSQLState())) {
                throw new UniqueConstraintViolationException("Данные уже существуют", e.getConstraintName(), e);
            }
            throw new DataAccessException("Ошибка целостности данных при " + operationName, e);
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            logger.error("Критическая ошибка Hibernate в '{}': {}", operationName, e.getMessage(), e);
            throw new DataAccessException("Сбой базы данных при " + operationName, e);
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            logger.error("Непредвиденная ошибка в '{}': ", operationName, e);
            throw new RuntimeException("Системная ошибка: " + e.getMessage(), e);
        }
    }

    @Override
    public UserResponseDTO createUser(UserCreateDTO dto) {
        ValidationUtil.validate(dto);
        logger.debug("Данные валидны для email: {}", dto.email());

        UserEntity savedUserEntity = inTransaction("createUser", () -> {
            UserEntity userEntity = userMapper.toEntity(dto);
            UserEntity result = userRepository.save(userEntity);
            logger.debug("Hibernate сохранил сущность с ID: {}", result.getId());
            return result;
        });
        logger.info("Пользователь успешно зарегистрирован: ID={}, Email={}",
                savedUserEntity.getId(), savedUserEntity.getEmail());

        return userMapper.toResponseDTO(savedUserEntity);
    }


    @Override
    public UserResponseDTO findById(long id) {
        logger.debug("Запрос findById для ID: {}", id);
        return inTransaction("findById", () ->
                userRepository.findById(id).map(userMapper::toResponseDTO)
                        .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + id + " не найден"))
        );
    }

    @Override
    public UserResponseDTO findByEmail(String email) {
        logger.debug("Запрос findByEmail: {}", email);
        return inTransaction("findByEmail", () ->
                userRepository.findByEmail(email).map(userMapper::toResponseDTO)
                        .orElseThrow(() -> new EntityNotFoundException("Пользователь с email " + email + " не найден"))
        );
    }

    @Override
    public UserResponseDTO findByUsername(String username) {
        logger.debug("Запрос findByUsername: {}", username);
        return inTransaction("findByUsername", () ->
                userRepository.findByUsername(username).map(userMapper::toResponseDTO)
                        .orElseThrow(() -> new EntityNotFoundException("Пользователь с username " + username + " не найден"))
        );
    }

    @Override
    public List<UserResponseDTO> findAll() {
        logger.debug("Запрос findAll");
        List<UserResponseDTO> users = inTransaction("findAll", () ->
                userRepository.findAll().stream()
                        .map(userMapper::toResponseDTO)
                        .toList()
        );
        log.info("Найдено пользователей: {}", users.size());
        return users;
    }

    @Override
    public UserResponseDTO updateUser(long id, UserCreateDTO dto) {
        logger.debug("Запрос на обновление ID {}: {}", id, dto);
        ValidationUtil.validate(dto);

        UserEntity updatedUserEntity = inTransaction("updateUser", () ->
                userRepository.findById(id)
                        .map(user -> {
                            user.setName(dto.name());
                            user.setEmail(dto.email());
                            user.setAge(dto.age());
                            return userRepository.save(user);
                        })
                        .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + id + " не найден"))
        );

        logger.info("Пользователь ID {} успешно обновлен", id);
        return userMapper.toResponseDTO(updatedUserEntity);
    }

    @Override
    public UserResponseDTO deleteById(long id) {
        logger.debug("Запрос на удаление ID: {}", id);

        UserEntity deletedUserEntity = inTransaction("deleteById", () ->
                userRepository.findById(id)
                        .map(user -> {
                            userRepository.deleteById(id);
                            return user; // Возвращаем объект, чтобы подтвердить, что он был
                        })
                        .orElseThrow(() -> new EntityNotFoundException("Невозможно удалить: пользователь с ID " + id + " не найден"))
        );

        logger.info("Пользователь ID {} успешно удален", id);
        return userMapper.toResponseDTO(deletedUserEntity);
    }
}
