package service;

import exception.DataAccessException;
import exception.UniqueConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import mapper.UserMapper;
import model.User;
import model.UserCreateDTO;
import model.UserResponseDTO;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import repository.UserRepository;
import repository.UserRepositoryImpl;
import util.HibernateUtil;
import util.ValidationUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository = new UserRepositoryImpl();
    private final UserMapper userMapper = UserMapper.INSTANCE;

    private <R> R inTransaction(Supplier<R> action) {
        Transaction transaction = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            R result = action.get();
            transaction.commit();
            return result;
        }  catch (ConstraintViolationException e) {
            if (transaction != null) transaction.rollback();
            if ("23505".equals(e.getSQLState())) {
                throw new UniqueConstraintViolationException("Данные уже существуют", e.getConstraintName(), e);
            }
            throw new DataAccessException("Нарушение ограничений базы данных", e);
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            log.error("Критическая ошибка Hibernate: {}", e.getMessage(), e);
            throw new DataAccessException("Ошибка при работе с БД", e);
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Непредвиденная ошибка: " + e.getMessage(), e);
        }
    }

    @Override
    public void createUser(UserCreateDTO dto) {
        ValidationUtil.validate(dto);
        inTransaction(() -> {
            User user = userMapper.toEntity(dto);
            return userRepository.save(user);
        });
    }

    @Override
    public Optional<UserResponseDTO> findById(long id) {
        return inTransaction(() ->
                userRepository.findById(id).map(userMapper::toResponseDTO)
        );
    }

    @Override
    public Optional<UserResponseDTO> findByEmail(String email) {
        return inTransaction(() ->
                userRepository.findByEmail(email).map(userMapper::toResponseDTO)
        );
    }

    @Override
    public Optional<UserResponseDTO> findByUsername(String username) {
        return inTransaction(() ->
                userRepository.findByUsername(username).map(userMapper::toResponseDTO)
        );
    }

    @Override
    public List<UserResponseDTO> findAll() {
        return inTransaction(() ->
                userRepository.findAll().stream()
                        .map(userMapper::toResponseDTO)
                        .toList()
        );
    }

    @Override
    public boolean updateUser(long id, UserCreateDTO dto) {
        ValidationUtil.validate(dto);
        return inTransaction(() ->
                userRepository.findById(id).map(user -> {
                    user.setName(dto.name());
                    user.setEmail(dto.email());
                    user.setAge(dto.age());
                    userRepository.save(user);
                    return true;
                }).orElse(false)
        );
    }

    @Override
    public boolean deleteById(long id) {
        return inTransaction(() ->
                userRepository.findById(id)
                        .map(user -> {
                            userRepository.deleteById(id);
                            return true;
                        })
                        .orElse(false)
        );
    }
}
