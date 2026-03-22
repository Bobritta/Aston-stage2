package controller;

import exception.UniqueConstraintViolationException;
import exception.handler.GlobalExceptionHandler;
import model.UserCreateDTO;
import model.UserResponseDTO;
import service.UserService;
import service.UserServiceImpl;

import java.util.List;
import java.util.Scanner;

public class CommandLineUserMenu {

    private final UserService userService = new UserServiceImpl();
    private final Scanner scanner = new Scanner(System.in);

    public void start() {
        System.out.println(ConsoleColors.CYAN + "=== User Service CLI ===" + ConsoleColors.RESET);

        while (true) {
            printMenu();
            String choice = scanner.nextLine();

            GlobalExceptionHandler.handle(() -> {
                switch (choice) {
                    case "1" -> handleCreate();
                    case "2" -> handleFindAll();
                    case "3" -> handleFindById();
                    case "4" -> handleFindByEmail();
                    case "5" -> handleDelete();
                    case "6" -> handleUpdate();
                    case "0" -> System.exit(0);
                    default -> System.out.println(ConsoleColors.RED.wrap("Неверный ввод!"));
                }
            });
        }
    }

    private void printMenu() {
        System.out.println("\nВыберите действие:");
        System.out.println("1. Создать пользователя");
        System.out.println("2. Показать всех");
        System.out.println("3. Найти по ID");
        System.out.println("4. Найти по email");
        System.out.println("5. Удалить пользователя");
        System.out.println("6. Обновить пользователя");
        System.out.println("0. Выйти");
        System.out.print("> ");
    }

    private void handleCreate() {
        boolean success = false;
        while (!success) {
            try {
                UserCreateDTO dto = readDto();
                userService.createUser(dto);
                System.out.println(ConsoleColors.GREEN.wrap("Успешно создано!"));
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

        userService.findById(id).ifPresentOrElse(
                user -> System.out.println(ConsoleColors.BLUE.wrap(user.toString())),
                () -> System.out.println(ConsoleColors.YELLOW.wrap("Пользователь не найден."))
        );
    }

    private void handleFindByEmail() {
        System.out.print("Введите Email: ");
        String email = scanner.nextLine();

        userService.findByEmail(email).ifPresentOrElse(
                user -> System.out.println(ConsoleColors.BLUE.wrap("ID: " + user.id() + " | " + user)),
                () -> System.out.println(ConsoleColors.YELLOW.wrap("Пользователь с таким email не найден."))
        );
    }

    private void handleDelete() {
        System.out.print("Введите ID для удаления: ");
        long id = Long.parseLong(scanner.nextLine());

        if (userService.deleteById(id)) {
            System.out.println(ConsoleColors.GREEN.wrap("Удаление выполнено."));
        } else {
            System.out.println(ConsoleColors.RED.wrap("Удаление невозможно: ID не найден."));
        }
    }

    private void handleUpdate() {
        System.out.println(ConsoleColors.YELLOW.wrap("Введите ID (или 'email' для поиска):"));
        String input = scanner.nextLine();
        long id;

        if ("email".equalsIgnoreCase(input)) {
            System.out.print("Email для поиска: ");
            id = userService.findByEmail(scanner.nextLine())
                    .map(UserResponseDTO::id)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
            System.out.println(ConsoleColors.CYAN.wrap("Обновляем пользователя с ID: " + id));
        } else {
            id = Long.parseLong(input);
        }

        System.out.println("Введите новые данные:");
        UserCreateDTO dto = readDto();

        if (userService.updateUser(id, dto)) {
            System.out.println(ConsoleColors.GREEN.wrap("Данные успешно обновлены!"));
        } else {
            System.out.println(ConsoleColors.RED.wrap("Ошибка обновления: ID не найден."));
        }
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