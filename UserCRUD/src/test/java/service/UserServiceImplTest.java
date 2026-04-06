package service;

import exception.DataAccessException;
import exception.UniqueConstraintViolationException;
import jakarta.persistence.EntityNotFoundException;
import mapper.UserMapper;
import model.UserCreateDTO;
import model.UserEntity;
import model.UserResponseDTO;
import model.UserUpdateDTO;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import repository.UserRepository;
import util.HibernateUtil;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Spy
    UserMapper userMapper = UserMapper.INSTANCE;

    @Spy
    Logger logger = NOPLogger.NOP_LOGGER;

    @InjectMocks
    @Spy
    UserServiceImpl userService;

    @Nested
    class WhenInTransactionMocked {
        @BeforeEach
        void setUp() {
            Mockito.lenient().doAnswer(invocation -> {
                Supplier<?> action = invocation.getArgument(1);
                return action.get();
            }).when(userService).inTransaction(anyString(), any());
        }

        @Test
        void shouldCreateUserWithCorrectEmail() {
            UserCreateDTO userCreateDTO = new UserCreateDTO("testuser", "correct@mail.ru", 18);
            UserEntity entity = UserEntity.builder().name("testuser").build();
            when(userRepository.save(any())).thenReturn(entity);

            UserResponseDTO response = userService.createUser(userCreateDTO);

            verify(userMapper).toEntity(userCreateDTO);
            assertEquals(response.name(), userCreateDTO.name());
            verify(userRepository).save(any());
        }

        @Test
        void shouldFindByIdWhenUserExists() {
            UserEntity entity = UserEntity.builder().id(1L).name("testuser").createdAt(Instant.now()).build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

            UserResponseDTO response = userService.findById(1L);

            assertEquals(1L, response.id());
            assertEquals("testuser", response.name());
            verify(userRepository).findById(1L);
        }

        @Test
        void shouldFindByNameWhenUserExists() {
            UserEntity entity = UserEntity.builder().id(1L).name("testuser").build();
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(entity));

            UserResponseDTO response = userService.findByUsername("testuser");

            assertEquals(1L, response.id());
            assertEquals("testuser", response.name());
            verify(userRepository).findByUsername("testuser");
        }

        @Test
        void shouldFindByEmailWhenUserExists() {
            UserEntity entity = UserEntity.builder().id(1L).name("testuser").email("Email@mail.ru").build();
            when(userRepository.findByEmail("Email@mail.ru")).thenReturn(Optional.of(entity));

            UserResponseDTO response = userService.findByEmail("Email@mail.ru");

            assertEquals(1L, response.id());
            assertEquals("testuser", response.name());
            verify(userRepository).findByEmail("Email@mail.ru");
        }

        @Test
        void shouldThrowException_WhenFindById_WhenUserNotExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.findById(1L));
            verify(userRepository).findById(1L);
        }

        @Test
        void shouldThrowException_WhenFindByNa_meWhenUserNotExists() {
            when(userRepository.findByUsername("Name")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.findByUsername("Name"));
            verify(userRepository).findByUsername("Name");
        }

        @Test
        void shouldThrowException_WhenFindByEm_ailWhenUserNotExists() {
            when(userRepository.findByEmail("Email@mail.ru")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.findByEmail("Email@mail.ru"));
            verify(userRepository).findByEmail("Email@mail.ru");
        }

        @Test
        void shouldFindAllUsers() {
            UserEntity entity1 = UserEntity.builder().id(1L).name("user1").build();
            UserEntity entity2 = UserEntity.builder().id(2L).name("user2").build();
            when(userRepository.findAll()).thenReturn(java.util.List.of(entity1, entity2));

            var response = userService.findAll();

            assertEquals(2, response.size());
            assertEquals("user1", response.get(0).name());
            assertEquals("user2", response.get(1).name());
            verify(userRepository).findAll();
        }

        @Test
        void shouldReturnEmptyListWhenNoUsers() {
            when(userRepository.findAll()).thenReturn(java.util.List.of());

            var response = userService.findAll();

            assertNotNull(response);
            assertEquals(0, response.size());
            verify(userRepository).findAll();
        }

        @Test
        void shouldUpdateUser_IfExists() {
            long id = 1L;
            Instant originalCreatedAt = Instant.now().minusSeconds(3600);

            UserEntity existingEntity = UserEntity.builder()
                    .id(id)
                    .name("oldName")
                    .email("old@mail.ru")
                    .age(25)
                    .createdAt(originalCreatedAt)
                    .build();

            UserUpdateDTO updateDTO = new UserUpdateDTO(id, "newName", "new@mail.ru", 30);

            when(userRepository.findById(id)).thenReturn(Optional.of(existingEntity));
            when(userRepository.save(any(UserEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UserResponseDTO response = userService.updateUser(id, updateDTO);

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(userCaptor.capture());

            UserEntity savedEntity = userCaptor.getValue();
            assertEquals("newName", savedEntity.getName());
            assertEquals("new@mail.ru", savedEntity.getEmail());
            assertEquals(30, savedEntity.getAge());
            assertEquals(id, savedEntity.getId(), "ID не должен измениться");
            assertEquals(originalCreatedAt, savedEntity.getCreatedAt(), "Дата создания должна сохраниться");

            assertNotNull(response);
            assertEquals(id, response.id());
            assertEquals("newName", response.name());
            assertEquals("new@mail.ru", response.email());
            assertNotNull(response.createdAt());
        }

        @Test
        void shouldThrowException_WhenUpdateUser_IfNotExists() {
            long id = 1L;
            UserUpdateDTO updateDTO = new UserUpdateDTO(id, "newName", "Bad@mail.ru", 30);

            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.updateUser(id, updateDTO));
            verify(userRepository).findById(id);
        }

        @Test
        void shouldDeleteByIdWhenUserExists() {
            UserEntity entity = UserEntity.builder().id(1L).name("testuser").createdAt(Instant.now()).build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

            UserResponseDTO response = userService.deleteById(1L);

            assertEquals(1L, response.id());
            assertEquals("testuser", response.name());
            verify(userRepository).deleteById(1L);
        }

        @Test
        void shouldThrowException_WhenDeleteUser_IfNotExists() {
            long id = 1L;
            UserUpdateDTO updateDTO = new UserUpdateDTO(id, "newName", "Bad@mail.ru", 30);

            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.deleteById(id));
            verify(userRepository).findById(id);
        }
    }

    @Test
    @DisplayName("Проверка использования валидации при создании пользователя с некорректным email")
    void shouldNotCreateUserWithIncorrectEmail() {
        UserCreateDTO userCreateDTO = new UserCreateDTO("testuser", "invalid-email-format", 18);

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(userCreateDTO));
    }


    @Nested
    class InTransactionInternalLogicTests {

        private MockedStatic<HibernateUtil> mockedHibernate;
        private Transaction transaction;
        private Session session;

        @BeforeEach
        void setUp() {
            mockedHibernate = mockStatic(HibernateUtil.class);
            SessionFactory sessionFactory = mock(SessionFactory.class);
            session = mock(Session.class);
            transaction = mock(Transaction.class);

            mockedHibernate.when(HibernateUtil::getSessionFactory).thenReturn(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.isOpen()).thenReturn(true);
            when(session.beginTransaction()).thenReturn(transaction);
        }

        @AfterEach
        void tearDown() {
            mockedHibernate.close();
        }

        @Test
        void shouldCommitTransactionAndReturnResult_WhenActionSucceeds() {
            UserCreateDTO dto = new UserCreateDTO("success", "ok@mail.ru", 25);
            UserEntity expectedEntity = UserEntity.builder().id(100L).build();
            when(userRepository.save(any())).thenReturn(expectedEntity);

            UserResponseDTO response = userService.createUser(dto);

            assertNotNull(response);
            verify(session).beginTransaction();
            verify(transaction).commit();
            verify(transaction, never()).rollback();
            verify(session).close();
        }


        @Test
        void shouldThrowUniqueConstraintViolationException_WhenSqlStateIs23505() {
            UserCreateDTO dto = new UserCreateDTO("user", "duplicate@mail.ru", 20);

            when(userRepository.save(any())).thenThrow(
                    new ConstraintViolationException("Duplicate", new SQLException("", "23505"), "users_email_key")
            );

            assertThrows(UniqueConstraintViolationException.class, () -> userService.createUser(dto));
            verify(transaction).rollback();
        }

        @Test
        void shouldThrowUniqueConstraintViolationException_WhenSqlStateIsNot23505() {
            UserCreateDTO dto = new UserCreateDTO("user", "somethingWrong@mail.ru", 20);

            when(userRepository.save(any())).thenThrow(
                    new ConstraintViolationException("Other error", new SQLException("", ""), "users_email_key")
            );

            assertThrows(DataAccessException.class, () -> userService.createUser(dto));
            verify(transaction).rollback();
        }

        @Test
        void shouldThrowDataAccessException_WhenOtherHibernateExceptionOccurs() {
            UserCreateDTO dto = new UserCreateDTO("user", "error@mail.ru", 20);

            when(userRepository.save(any())).thenThrow(new HibernateException("error"));

            assertThrows(DataAccessException.class, () -> userService.createUser(dto));
            verify(transaction).rollback();
        }

        @Test
        void shouldThrowRuntimeException_WhenUnexpectedExceptionOccurs() {
            UserCreateDTO dto = new UserCreateDTO("user", "crash@mail.ru", 20);

            when(userRepository.save(any())).thenThrow(new RuntimeException("Unexpected"));

            assertThrows(RuntimeException.class, () -> userService.createUser(dto));
            verify(transaction).rollback();
        }
    }
}