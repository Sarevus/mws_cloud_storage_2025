package com.MWS;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.MockedStatic;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerContainerTest {

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:13")
                    .withDatabaseName("testdb")
                    .withUsername("test_user")
                    .withPassword("passwd");

    private static final String URL = "http://localhost:4567";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUpAll() throws Exception {
        Connection testConnection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
        createTables(testConnection);
        testConnection.close();

        // Подменяю Database.getConnection, чтобы он возвращал соединение с тестовой БД вместо продакшн
        MockedStatic<com.MWS.storage.Database> databaseMock = mockStatic(com.MWS.storage.Database.class);
        databaseMock.when(com.MWS.storage.Database::getConnection)
                .thenAnswer(invocation -> DriverManager.getConnection(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                ));

        Thread serverThread = new Thread(() -> Main.main(new String[]{}));
        serverThread.start();

        sleep(3000);
    }

    private static void createTables(Connection connection) throws Exception {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id UUID PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    phonenumber VARCHAR(20),
                    password VARCHAR(255) NOT NULL
                )
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
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

    // REGISTER

    @Test
    void SuccessfulRegistration() throws Exception {
        String requestBody = """
            {
                "name": "Аня",
                "email": "testuser1@mail.ru",
                "phoneNumber": "+79106754432",
                "password": "123456"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
    }

    @Test
    void InvalidPhoneNumber() throws Exception {
        String requestBody = """
            {
                "name": "Борис",
                "email": "testuser2@mail.ru",
                "phoneNumber": "+9-167-54432",
                "password": "654321"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    @Test
    void InvalidDoubleEmail() throws Exception {
        String firstRequestBody = """
            {
                "name": "Пользователь3",
                "email": "testuser3@mail.ru",
                "phoneNumber": "+74532657888",
                "password": "4087893"
            }
            """;

        HttpRequest firstRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(firstRequestBody))
                .build();

        HttpResponse<String> firstResponse = httpClient.send(firstRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, firstResponse.statusCode());

        String secondRequestBody = """
            {
                "name": "Пользователь2",
                "email": "testuser3@mail.ru",
                "phoneNumber": "+74532657888",
                "password": "4087893"
            }
            """;

        HttpRequest secondRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(secondRequestBody))
                .build();

        HttpResponse<String> response = httpClient.send(secondRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    @Test
    void InvalidPassword() throws Exception {
        String requestBody = """
            {
                "name": "Даша123",
                "email": "daria@gmail.com",
                "phoneNumber": "+72409825541",
                "password": "jqi"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    // GET

    @Test
    void GetUserByIdSuccessfully() throws Exception {
        String registerBody = """
            {
                "name": "Аня",
                "email": "testuserAnna@mail.ru",
                "phoneNumber": "+79106754432",
                "password": "123456"
            }
            """;

        HttpRequest registerRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(registerBody))
                .build();

        HttpResponse<String> registerResponse = httpClient.send(registerRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, registerResponse.statusCode());

        String userId = extractUserIdFromResponse(registerResponse.body());

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/" + userId))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
    }

    @Test
    void InvalidGetNonExistentUser() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/" + nonExistentId))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void GetInvalidUUID() throws Exception {
        String invalidId = "invalid-uuid";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/" + invalidId))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    // UPDATE

    @Test
    void UpdateUserSuccessfully() throws Exception {
        String registerBody = """
            {
                "name": "Аня",
                "email": "testuserAnna@mail.ru",
                "phoneNumber": "+79106754432",
                "password": "123456"
            }
            """;

        HttpRequest registerRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(registerBody))
                .build();

        HttpResponse<String> registerResponse = httpClient.send(registerRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, registerResponse.statusCode());

        String userId = extractUserIdFromResponse(registerResponse.body());

        String updateBody = """
            {
                "name": "Аня Updated",
                "email": "anya_updated@mail.ru",
                "phoneNumber": "+79106759999",
                "password": "newpassword123"
            }
            """;

        HttpRequest updateRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/" + userId))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(updateBody))
                .build();

        HttpResponse<String> response = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
    }

    @Test
    void InvalidUpdateWithInvalidUUID() throws Exception {
        String invalidId = "invalid-uuid";
        String updateBody = """
            {
                "name": "Пользователь",
                "email": "user@mail.ru",
                "phoneNumber": "+79101234567",
                "password": "password123"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/" + invalidId))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(updateBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    // DELETE

    @Test
    void DeleteUserSuccessfully() throws Exception {
        String registerBody = """
            {
                "name": "Аня",
                "email": "testuserDelete@mail.ru",
                "phoneNumber": "+79106754432",
                "password": "123456"
            }
            """;

        HttpRequest registerRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(registerBody))
                .build();

        HttpResponse<String> registerResponse = httpClient.send(registerRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, registerResponse.statusCode());

        String userId = extractUserIdFromResponse(registerResponse.body());

        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/" + userId))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
    }

    @Test
    void InvalidDeleteNonExistentUser() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/" + nonExistentId))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void InvalidDeleteWithInvalidUUID() throws Exception {
        String invalidId = "invalid-uuid";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + "/api/users/" + invalidId))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    // Извлечение ID пользователя из JSON-ответа
    private String extractUserIdFromResponse(String responseBody) {
        try {
            com.MWS.dto.get.GetSimpleUserDto userDto = objectMapper.readValue(responseBody, com.MWS.dto.get.GetSimpleUserDto.class);
            return userDto.id().toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}