package controller;

import exception.UniqueConstraintViolationException;
import exception.handler.GlobalExceptionHandler;
import mapper.UserMapper;
import model.UserCreateDTO;
import model.UserResponseDTO;
import model.UserUpdateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.UserRepository;
import repository.UserRepositoryImpl;
import service.UserService;
import service.UserServiceImpl;

import java.util.List;
import java.util.Scanner;

public class CommandLineUserMenu {
    private static final Logger logger = LoggerFactory.getLogger(CommandLineUserMenu.class);

    private final Scanner scanner = new Scanner(System.in);
    private final UserService userService;

    public CommandLineUserMenu() {
        this.userService = new UserServiceImpl(
                new UserRepositoryImpl(),
                UserMapper.INSTANCE,
                LoggerFactory.getLogger(UserServiceImpl.class)
        );
    }

    public void start() {
        logger.info("Приложение запущено");
        System.out.println(ConsoleColors.CYAN + "=== User Service CLI ===" + ConsoleColors.RESET);

        while (true) {
            printMenu();
            String choice = scanner.nextLine();
            logger.debug("Пользователь выбрал пункт меню: {}", choice);

            GlobalExceptionHandler.handle(() -> {
                switch (choice) {
                    case "1" -> handleCreate();
                    case "2" -> handleFindAll();
                    case "3" -> handleFindById();
                    case "4" -> handleFindByEmail();
                    case "5" -> handleFindByUsername();
                    case "6" -> handleDelete();
                    case "7" -> handleUpdate();
                    case "0" -> {
                        logger.info("Завершение работы пользователем");
                        System.exit(0);
                    }
                    default -> System.out.println(ConsoleColors.RED.wrap("Неверный ввод!"));
                }
                return null;
            });
        }
    }

    private void printMenu() {
        System.out.println("\nВыберите действие:");
        System.out.println("1. Создать пользователя");
        System.out.println("2. Показать всех");
        System.out.println("3. Найти по ID");
        System.out.println("4. Найти по email");
        System.out.println("5. Найти по имени пользователя");
        System.out.println("6. Удалить пользователя");
        System.out.println("7. Обновить пользователя");
        System.out.println("0. Выйти");
        System.out.print("> ");
    }

    private void handleCreate() {
        boolean success = false;
        while (!success) {
            try {
                UserCreateDTO dto = readDto();
                UserResponseDTO responseDTO = userService.createUser(dto);
                System.out.println(ConsoleColors.GREEN.wrap("Успешно создан пользователь с ID: " + responseDTO.id()));
                success = true;
            } catch (IllegalArgumentException | UniqueConstraintViolationException e) {
                System.out.println(ConsoleColors.RED.wrap("Ошибка в данных: " + e.getMessage()));
                System.out.println("Попробуйте заполнить данные заново...");
            }
        }
    }

    private void handleFindById() {
        System.out.print("Введите ID: ");
        long id = Long.parseLong(scanner.nextLine());
        System.out.println(ConsoleColors.BLUE.wrap(userService.findById(id).toString()));
    }

    private void handleFindByEmail() {
        System.out.print("Введите Email: ");
        String email = scanner.nextLine();
        System.out.println(ConsoleColors.BLUE.wrap(userService.findByEmail(email).toString()));
    }

    private void handleFindByUsername() {
        System.out.print("Введите имя пользователя: ");
        String username = scanner.nextLine();
        System.out.println(ConsoleColors.BLUE.wrap(userService.findByUsername(username).toString()));
    }

    private void handleDelete() {
        System.out.print("Введите ID для удаления: ");
        long id = Long.parseLong(scanner.nextLine());

        UserResponseDTO deleted = userService.deleteById(id);
        System.out.println(ConsoleColors.GREEN.wrap("Удален пользователь: " + deleted.name()));
    }

    private void handleUpdate() {
        System.out.println(ConsoleColors.YELLOW.wrap("Введите ID (или 'email' для поиска):"));
        String input = scanner.nextLine();
        long id;

        if ("email".equalsIgnoreCase(input)) {
            System.out.print("Email для поиска: ");
            id = userService.findByEmail(scanner.nextLine()).id();
            System.out.println(ConsoleColors.CYAN.wrap("Обновляем пользователя с ID: " + id));
        } else {
            id = Long.parseLong(input);
        }

        System.out.println("Введите новые данные:");
        UserCreateDTO createDto = readDto();

        UserUpdateDTO updateDto = UserUpdateDTO.builder()
                .id(id)
                .name(createDto.name())
                .email(createDto.email())
                .age(createDto.age())
                .build();

        UserResponseDTO updated = userService.updateUser(id, updateDto);
        System.out.println(ConsoleColors.GREEN.wrap("Обновлен: " + updated.name()));
    }

    private UserCreateDTO readDto() {
        System.out.print("Имя: ");
        String name = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Возраст: ");
        int age = Integer.parseInt(scanner.nextLine());

        return UserCreateDTO.builder()
                .name(name)
                .email(email)
                .age(age)
                .build();
    }

    private void handleFindAll() {
        List<UserResponseDTO> users = userService.findAll();
        if (users.isEmpty()) {
            System.out.println(ConsoleColors.YELLOW.wrap("База данных пуста."));
        } else {
            System.out.println(ConsoleColors.PURPLE + "--- Список пользователей ---" + ConsoleColors.RESET);
            users.forEach(user -> System.out.println(ConsoleColors.BLUE.wrap(user.toString())));
        }
    }

}