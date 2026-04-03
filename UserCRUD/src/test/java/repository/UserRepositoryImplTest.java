package repository;

import jakarta.persistence.EntityNotFoundException;
import model.UserEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.BaseIntegrationTest;
import util.HibernateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryImplTest extends BaseIntegrationTest {

    private final UserRepository userRepository = new UserRepositoryImpl();
    private Session session;

    @BeforeEach
    void setUp() {
        session = HibernateUtil.getSessionFactory().openSession();
        ManagedSessionContext.bind(session);
        session.beginTransaction();
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.getTransaction().rollback();
            ManagedSessionContext.unbind(HibernateUtil.getSessionFactory());
            session.close();
        }
    }

    @Test
    @DisplayName("Должен вернуть Empty, если пользователь не найден")
    void shouldReturnEmptyWhenUserNotFound() {
        Optional<UserEntity> found = userRepository.findByEmail("non-existent@test.com");
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Удаление: должен выбросить исключение, если id не найден")
    void shouldThrowExceptionWhenUserNotFound() {

        assertThrows(EntityNotFoundException.class, () -> userRepository.deleteById(1L));
    }

    @Test
    @DisplayName("Должен создать пользователя и найти его по email, по имени")
    void shouldSaveNewUserAndFindByEmail() {
        UserEntity user = UserEntity.builder()
                .name("Ivan")
                .email("ivan@mail.com")
                .age(25)
                .build();

        userRepository.save(user);

        Optional<UserEntity> byEmail = userRepository.findByEmail("ivan@mail.com");
        assertTrue(byEmail.isPresent());
        assertEquals("Ivan", byEmail.get().getName());

    }

    @Test
    @DisplayName("Удаление: должен создать пользователя, найти его по имени, затем удалить")
    void shouldFindByUsernameAndDelete() {
        UserEntity user = UserEntity.builder()
                .name("Ivan")
                .email("ivan@mail.com")
                .age(25)
                .build();

        user = userRepository.save(user);

        Optional<UserEntity> byUsername = userRepository.findByUsername("Ivan");
        assertTrue(byUsername.isPresent());
        assertEquals(25, byUsername.get().getAge());

        userRepository.deleteById(user.getId());

        assertFalse(userRepository.findByUsername("Ivan").isPresent());
    }

    @Test
    @DisplayName("Сохранить новых пользователей группой меньше, чем batch, найти по id")
    void shouldSave10NewUsersAndFindById() {

        List<UserEntity> users = IntStream.range(0, 10)
                .mapToObj(i -> {
                    return UserEntity.builder()
                            .name("user" + i)
                            .email("mail" + i + "@test.com")
                            .age(i)
                            .build();

                })
                .toList();
        userRepository.createAll(users);

        Long firstId = users.get(0).getId();
        assertNotNull(firstId);
        assertTrue(userRepository.findById(firstId).isPresent());
    }

    @Test
    @DisplayName("Сохранить новых пользователей группой больше, чем batch, найти всех")
    void shouldSave60NewUsersfindAll() {

        int count = 60;
        List<UserEntity> users = IntStream.range(0, count)
                .mapToObj(i -> {
                    return UserEntity.builder()
                            .name("user" + i)
                            .email("mail" + i + "@test.com")
                            .age(i)
                            .build();

                })
                .toList();

        userRepository.createAll(users);
        List<UserEntity> allUsers = userRepository.findAll();
        assertEquals(count, allUsers.size());
    }
}
