package com.MWS.service;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.model.User;
import com.MWS.repository.UserRepository;
import com.MWS.repository.UserRepositoryPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@Testcontainers
public class UserServiceContainerTest {

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:13")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    private static UserServiceRelease userService;
    private static UserRepository userRepository;
    private static MockedStatic<com.MWS.storage.Database> databaseMock;
    private static boolean isDatabaseMigrated = false;

    @BeforeAll
    static void setUp() throws Exception {
        if (!isDatabaseMigrated) {
            migrateDatabaseWithFlyway();
            isDatabaseMigrated = true;
        }

        databaseMock = mockStatic(com.MWS.storage.Database.class);
        databaseMock.when(com.MWS.storage.Database::getConnection)
                .thenAnswer(invocation -> DriverManager.getConnection(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                ));

        userRepository = new UserRepositoryPostgres();
        userService = new UserServiceRelease(userRepository);
    }

    private static void migrateDatabaseWithFlyway() {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .locations("classpath:migration")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }

    @BeforeEach
    void clearData() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
             Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM users");
        }
    }

    @AfterAll
    static void tearDown() {
        if (databaseMock != null) {
            databaseMock.close();
        }
    }

    // CREATE

    @Test
    public void SelectAndSuccessfulRegistration() {
        CreateUserDTO successfulDto = new CreateUserDTO(
                "Аня",
                "testuser1@mail.ru",
                "+79106754432",
                "123456"
        );

        GetSimpleUserDto result = userService.createUser(successfulDto);

        assertNotNull(result);
        assertEquals("testuser1@mail.ru", result.email());
        assertEquals("+79106754432", result.phoneNumber());

        Optional<User> savedUser = userRepository.findByEmail("testuser1@mail.ru");
        assertTrue(savedUser.isPresent());
        assertEquals("Аня", savedUser.get().getName());
    }

    @Test
    public void InvalidPhoneNumberRegistration() {
        CreateUserDTO dto = new CreateUserDTO(
                "Борис",
                "testuser2@mail.ru",
                "+9-167-54432",
                "654321"
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(dto);
        });
        assertTrue(exception.getMessage().contains("Некорректный номер телефона"));
    }

    @Test
    public void DoubleEmailRegistration() {
        CreateUserDTO firstUser = new CreateUserDTO(
                "Пользователь2",
                "testuser1@mail.ru",
                "+74532657888",
                "4087893"
        );
        userService.createUser(firstUser);

        CreateUserDTO dto = new CreateUserDTO(
                "Пользователь3",
                "testuser1@mail.ru",
                "+74532657888",
                "4087893"
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(dto);
        });

        assertTrue(exception.getMessage().contains("Email " + dto.email() + " уже занят."));
    }

    @Test
    public void SelectAndOneMoreSuccessfulRegistration() {
        CreateUserDTO successfulDto = new CreateUserDTO(
                "Глеб123",
                "gglleebb@gmail.com",
                "+76665554433",
                "jqewgfiygqi"
        );

        GetSimpleUserDto result = userService.createUser(successfulDto);

        assertNotNull(result);
        assertEquals("+76665554433", result.phoneNumber());

        Optional<User> savedUser = userRepository.findByEmail("gglleebb@gmail.com");
        assertTrue(savedUser.isPresent());
        assertEquals("Глеб123", savedUser.get().getName());
    }

    @Test
    public void InvalidPasswordRegistration() {
        CreateUserDTO successfulDtoTwo = new CreateUserDTO(
                "Даша123",
                "daria@gmail.com",
                "+72409825541",
                "jqi"
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(successfulDtoTwo);
        });
        assertTrue(exception.getMessage().contains("Пароль должен быть от 6 до 100 символов"));
    }

    // GET

    @Test
    public void GetUserByIdSuccessfully() {
        CreateUserDTO userDto = new CreateUserDTO(
                "Аня",
                "testuser1@mail.ru",
                "+79106754432",
                "123456"
        );
        GetSimpleUserDto createdUser = userService.createUser(userDto);
        UUID userId = createdUser.id();

        GetSimpleUserDto result = userService.getUser(userId);

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("Аня", result.name());
        assertEquals("testuser1@mail.ru", result.email());
        assertEquals("+79106754432", result.phoneNumber());
    }

    @Test
    public void InvalidGetNonExistentUser() {
        UUID nonExistentId = UUID.randomUUID();
        jakarta.persistence.EntityNotFoundException exception = assertThrows(
                jakarta.persistence.EntityNotFoundException.class,
                () -> userService.getUser(nonExistentId)
        );
        assertTrue(exception.getMessage().contains("Пользователь не найден: " + nonExistentId));
    }

    // UPDATE

    @Test
    public void UpdateUserSuccessfully() {
        CreateUserDTO createDto = new CreateUserDTO(
                "Вова",
                "vovauser@mail.ru",
                "+78765675444",
                "yuty43uty"
        );
        GetSimpleUserDto createdUser = userService.createUser(createDto);
        UUID userId = createdUser.id();

        CreateUserDTO updateDto = new CreateUserDTO(
                "Вова Новый",
                "vova_updated@mail.ru",
                "+79106759999",
                "newpassword123"
        );

        GetSimpleUserDto result = userService.updateUser(userId, updateDto);

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("Вова Новый", result.name());
        assertEquals("vova_updated@mail.ru", result.email());
        assertEquals("+79106759999", result.phoneNumber());

        Optional<User> updatedUser = userRepository.findByEmail("vova_updated@mail.ru");
        assertTrue(updatedUser.isPresent());
        assertEquals("Вова Новый", updatedUser.get().getName());
        assertEquals("+79106759999", updatedUser.get().getPhoneNumber());
    }

    @Test
    public void InvalidUpdateNonExistentUser() {
        UUID nonExistentId = UUID.randomUUID();
        CreateUserDTO updateDto = new CreateUserDTO(
                "Никто",
                "nonexistent@mail.ru",
                "+79100000000",
                "password123"
        );

        java.util.NoSuchElementException exception = assertThrows(
                java.util.NoSuchElementException.class,
                () -> userService.updateUser(nonExistentId, updateDto)
        );
        assertTrue(exception.getMessage().contains("Такого пользователя не существует: " + nonExistentId));
    }

    @Test
    public void InvalidUpdateWithDuplicateEmail() {
        CreateUserDTO firstUser = new CreateUserDTO(
                "Аня",
                "testuser1@mail.ru",
                "+79106754432",
                "123456"
        );
        GetSimpleUserDto firstCreated = userService.createUser(firstUser);
        UUID firstUserId = firstCreated.id();

        CreateUserDTO secondUser = new CreateUserDTO(
                "Глеб123",
                "gglleebb@gmail.com",
                "+76665554433",
                "jqewgfiygqi"
        );
        GetSimpleUserDto secondCreated = userService.createUser(secondUser);
        UUID secondUserId = secondCreated.id();

        CreateUserDTO updateDto = new CreateUserDTO(
                "Глеб123",
                "testuser1@mail.ru",
                "+76665554433",
                "jqewgfiygqi"
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(secondUserId, updateDto);
        });
        assertTrue(exception.getMessage().contains("Email testuser1@mail.ru уже занят."));
    }

    @Test
    public void UpdateUserWithSameEmailSuccessfully() {
        CreateUserDTO createDto = new CreateUserDTO(
                "Аня",
                "testuser1@mail.ru",
                "+79106754432",
                "123456"
        );
        GetSimpleUserDto createdUser = userService.createUser(createDto);
        UUID userId = createdUser.id();

        CreateUserDTO updateDto = new CreateUserDTO(
                "Аня Updated",
                "testuser1@mail.ru",
                "+79106759999",
                "newpassword123"
        );

        GetSimpleUserDto result = userService.updateUser(userId, updateDto);

        assertNotNull(result);
        assertEquals("Аня Updated", result.name());
        assertEquals("testuser1@mail.ru", result.email());
        assertEquals("+79106759999", result.phoneNumber());
    }

    // DELETE

    @Test
    public void DeleteUserSuccessfully() {
        CreateUserDTO createDto = new CreateUserDTO(
                "Даша123",
                "daria@gmail.com",
                "+72409825541",
                "767676"
        );
        GetSimpleUserDto createdUser = userService.createUser(createDto);
        UUID userId = createdUser.id();

        Optional<User> userBeforeDelete = userRepository.findById(userId);
        assertTrue(userBeforeDelete.isPresent());

        userService.deleteUser(userId);

        Optional<User> userAfterDelete = userRepository.findById(userId);
        assertFalse(userAfterDelete.isPresent(), "Пользователь должен быть удален");
    }

    @Test
    public void InvalidDeleteNonExistentUser() {
        UUID nonExistentId = UUID.randomUUID();

        jakarta.persistence.EntityNotFoundException exception = assertThrows(
                jakarta.persistence.EntityNotFoundException.class,
                () -> userService.deleteUser(nonExistentId)
        );
        assertTrue(exception.getMessage().contains("Пользователь не найден: " + nonExistentId));
    }

    // LOGIN

    @Test
    public void LoginUserSuccessfully() {
        CreateUserDTO createDto = new CreateUserDTO(
                "Коля",
                "kolya@mail.ru",
                "+79106754432",
                "654321"
        );
        userService.createUser(createDto);

        UUID userId = userService.loginUser("kolya@mail.ru", "654321");

        assertNotNull(userId);

        Optional<User> user = userRepository.findByEmail("kolya@mail.ru");
        assertTrue(user.isPresent());
        assertEquals(user.get().getId(), userId);
    }

    @Test
    public void InvalidLoginWrongEmail() {
        CreateUserDTO createDto = new CreateUserDTO(
                "Коля",
                "kolya@mail.ru",
                "+79106754432",
                "654321"
        );
        userService.createUser(createDto);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.loginUser("wrong@mail.ru", "654321");
        });
        assertTrue(exception.getMessage().contains("Пользователь не найден"));
    }

    @Test
    public void InvalidLoginWrongPassword() {
        CreateUserDTO createDto = new CreateUserDTO(
                "Коля",
                "kolya@mail.ru",
                "+79106754432",
                "654321"
        );
        userService.createUser(createDto);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.loginUser("kolya@mail.ru", "incorrectPassword");
        });
        assertTrue(exception.getMessage().contains("Неверный пароль"));
    }
}