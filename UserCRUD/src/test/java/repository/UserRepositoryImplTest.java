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
import org.junit.jupiter.api.Nested;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Nested
    class WhenUserNotExist {

        @Test
        @DisplayName("Поиск: по email должен вернуть Empty, если пользователь не найден")
        void shouldReturnEmptyWhenUserNotFoundByEmail() {
            Optional<UserEntity> found = userRepository.findByEmail("non-existent@test.com");
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Поиск: по id должен вернуть Empty, если пользователь не найден")
        void shouldReturnEmptyWhenUserNotFoundById() {
            Optional<UserEntity> found = userRepository.findById(55L);
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Поиск: по имени должен вернуть Empty, если пользователь не найден")
        void shouldReturnEmptyWhenUserNotFoundByName() {
            Optional<UserEntity> found = userRepository.findByUsername("non-existent-name");
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Удаление: должен выбросить исключение, если id не найден")
        void shouldThrowExceptionWhenUserNotFound() {
            assertThrows(EntityNotFoundException.class, () -> userRepository.deleteById(1L));
        }

        @Test
        @DisplayName("Должен создать пользователя и присвоить id")
        void shouldSaveNewUser() {
            UserEntity user = UserEntity.builder()
                    .name("Ivan")
                    .email("ivan@mail.com")
                    .age(25)
                    .build();
            assertNull(user.getId());
            assertNull(user.getCreatedAt());

            user = userRepository.save(user);
            assertNotNull(user.getId());
        }

        @Test
        @DisplayName("Сохранить новых пользователей группой меньше, чем batch без присвоения даты создания")
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
            assertNull(userRepository.findById(firstId).get().getCreatedAt());
        }

        @Test
        @DisplayName("Сохранить новых пользователей группой больше, чем batch c flush  присвоением даты создания")
        void shouldSave60NewUsersFindAll() {

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

            Long firstId = users.get(0).getId();
            assertNotNull(firstId);
            assertTrue(userRepository.findById(firstId).isPresent());
            assertNotNull(userRepository.findById(firstId).get().getCreatedAt());
        }
    }

    @Nested
    class WhenUserExist {
        UserEntity user;

        @BeforeEach
        void setUp() {
            user = UserEntity.builder()
                    .name("Ivan")
                    .email("ivan@mail.com")
                    .age(25)
                    .build();

            user = userRepository.save(user);
        }

        @AfterEach
        void tearDown() {
            if (userRepository.findById(user.getId()).isPresent()) {
                userRepository.deleteById(user.getId());
            }
        }

        @Test
        @DisplayName("Поиск: по email")
        void shouldFindByEmail() {
            Optional<UserEntity> byEmail = userRepository.findByEmail("ivan@mail.com");

            assertTrue(byEmail.isPresent());
            assertEquals("Ivan", byEmail.get().getName());
        }

        @Test
        @DisplayName("Поиск: по имени")
        void shouldFindByName() {
            Optional<UserEntity> byUsername = userRepository.findByUsername("Ivan");

            assertTrue(byUsername.isPresent());
            assertEquals(25, byUsername.get().getAge());
        }

        @Test
        @DisplayName("Поиск: по id")
        void shouldFindById() {
            Optional<UserEntity> byId = userRepository.findById(user.getId());

            assertTrue(byId.isPresent());
            assertEquals(25, byId.get().getAge());
        }

        @Test
        @DisplayName("Удаление: удалить пользователя")
        void shouldDeleteUserById() {
            Optional<UserEntity> byUsername = userRepository.findByUsername("Ivan");
            assertTrue(byUsername.isPresent());

            userRepository.deleteById(user.getId());

            assertFalse(userRepository.findByUsername("Ivan").isPresent());
        }

        @Test
        @DisplayName("Поиск: должен найти всех: в кеше и в БД")
        void shouldFindUsersFromDatabaseAndCache() {

            List<UserEntity> users = IntStream.range(0, 60) // больше batch
                    .mapToObj(i -> {
                        return UserEntity.builder()
                                .name("user" + i)
                                .email("mail" + i + "@test.com")
                                .age(i)
                                .build();

                    })
                    .toList();

            userRepository.createAll(users);

            users = IntStream.range(0, 10) // меньше batch
                    .mapToObj(i -> {
                        return UserEntity.builder()
                                .name("newUser" + i)
                                .email("newMail" + i + "@test.com")
                                .age(i)
                                .build();

                    })
                    .toList();

            userRepository.createAll(users);

            users = userRepository.findAll();
            assertEquals(71, users.size());
        }
    }
}
