//package com.MWS.handlers;
//
//import com.MWS.dto.ShareRequestDto;
//import com.MWS.dto.FilePermissionDto;
//import com.MWS.dto.get.GetSimpleUserDto;
//import com.MWS.model.Roles;
//import com.MWS.repository.UserRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.JsonNode;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockHttpSession;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@Testcontainers
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//public class PermissionControllerContainerTest {
//
//    @Container
//    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
//            .withDatabaseName("cloud3_data")
//            .withUsername("test")
//            .withPassword("test");
//
//    @DynamicPropertySource
//    static void properties(DynamicPropertyRegistry registry) {
//        // PostgreSQL
//        registry.add("spring.datasource.url", postgres::getJdbcUrl);
//        registry.add("spring.datasource.username", postgres::getUsername);
//        registry.add("spring.datasource.password", postgres::getPassword);
//
//        // ОТКЛЮЧАЕМ Redis - используем обычные HTTP сессии
//        registry.add("spring.session.store-type", () -> "none");
//
//        // Отключаем авто-настройки и доверяемся Flyway
//        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
//        registry.add("spring.jpa.generate-ddl", () -> "false");
//
//        // Включаем Flyway
//        registry.add("spring.flyway.enabled", () -> "true");
//        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
//        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
//    }
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    private static MockHttpSession ownerSession;
//    private static MockHttpSession targetSession;
//    private static MockHttpSession otherSession;
//
//    private static UUID ownerId;
//    private static UUID targetId;
//    private static UUID otherId;
//    private static UUID ownerFileId;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        // Очищаем БД
//        userRepository.deleteAll();
//
//        // Создаем пользователей и получаем их ID из ответа
//        ownerId = createUserAndGetId("owner@test.com", "Owner", "+79161234567", "password123");
//        targetId = createUserAndGetId("target@test.com", "Target", "+79161234568", "password123");
//        otherId = createUserAndGetId("other@test.com", "Other", "+79161234569", "password123");
//
//        // Логинимся и получаем сессии
//        ownerSession = loginAndGetSession("owner@test.com", "password123");
//        targetSession = loginAndGetSession("target@test.com", "password123");
//        otherSession = loginAndGetSession("other@test.com", "password123");
//
//        // Проверяем что в сессии есть userId
//        assertNotNull(ownerSession.getAttribute("userId"), "Owner session должен содержать userId");
//        assertNotNull(targetSession.getAttribute("userId"), "Target session должен содержать userId");
//        assertNotNull(otherSession.getAttribute("userId"), "Other session должен содержать userId");
//
//        // Создаем файл для owner
//        ownerFileId = createFileForOwner(ownerSession, ownerId);
//        assertNotNull(ownerFileId, "File ID не должен быть null");
//
//        System.out.println("=== TEST SETUP SUCCESS ===");
//        System.out.println("ownerId: " + ownerId);
//        System.out.println("targetId: " + targetId);
//        System.out.println("otherId: " + otherId);
//        System.out.println("ownerFileId: " + ownerFileId);
//        System.out.println("ownerSession userId: " + ownerSession.getAttribute("userId"));
//        System.out.println("targetSession userId: " + targetSession.getAttribute("userId"));
//        System.out.println("otherSession userId: " + otherSession.getAttribute("userId"));
//    }
//
//    private UUID createUserAndGetId(String email, String name, String phone, String password) throws Exception {
//        String userJson = String.format(
//                "{\"name\":\"%s\",\"email\":\"%s\",\"phoneNumber\":\"%s\",\"password\":\"%s\"}",
//                name, email, phone, password
//        );
//
//        MvcResult result = mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(userJson))
//                .andExpect(status().isCreated())
//                .andReturn();
//
//        String jsonResponse = result.getResponse().getContentAsString();
//        GetSimpleUserDto user = objectMapper.readValue(jsonResponse, GetSimpleUserDto.class);
//
//        return user.id();
//    }
//
//    private MockHttpSession loginAndGetSession(String email, String password) throws Exception {
//        String loginJson = String.format(
//                "{\"email\":\"%s\",\"password\":\"%s\"}",
//                email, password
//        );
//
//        MvcResult result = mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(loginJson))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
//        assertNotNull(session.getAttribute("userId"), "После логина userId должен быть в сессии");
//
//        return session;
//    }
//
//    private UUID createFileForOwner(MockHttpSession session, UUID userId) throws Exception {
//        assertNotNull(userId, "userId не должен быть null в createFileForOwner");
//        assertNotNull(session.getAttribute("userId"), "session должна содержать userId");
//
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.txt",
//                MediaType.TEXT_PLAIN_VALUE,
//                "Hello World".getBytes()
//        );
//
//        MvcResult result = mockMvc.perform(multipart("/api/files/upload")
//                        .file(file)
//                        .session(session)
//                        .param("userId", userId.toString())
//                        .param("category", "test"))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        String jsonResponse = result.getResponse().getContentAsString();
//        System.out.println("Upload response: " + jsonResponse);
//
//        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
//
//        JsonNode fileNode = jsonNode.get("file");
//        assertNotNull(fileNode, "Ответ должен содержать поле file");
//
//        JsonNode idNode = fileNode.get("id");
//        assertNotNull(idNode, "file должен содержать поле id");
//
//        String fileIdStr = idNode.asText();
//        UUID fileId = UUID.fromString(fileIdStr);
//
//        System.out.println("Created file with ID: " + fileId);
//        return fileId;
//    }
//
//    @Test
//    @Order(1)
//    void ownerCanShareFile() throws Exception {
//        System.out.println("=== Testing shareFile ===");
//        System.out.println("Owner session userId: " + ownerSession.getAttribute("userId"));
//
//        ShareRequestDto shareRequest = new ShareRequestDto(ownerFileId, "target@test.com", Roles.READER);
//
//        MvcResult result = mockMvc.perform(post("/api/permission/share")
//                        .session(ownerSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isCreated())
//                .andReturn();
//
//        FilePermissionDto response = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                FilePermissionDto.class
//        );
//
//        assertNotNull(response);
//        assertEquals(ownerFileId, response.fileId());
//        assertEquals(targetId, response.user().id());
//        assertEquals(Roles.READER, response.role());
//        assertTrue(response.isActive());
//    }
//
//    @Test
//    @Order(2)
//    void ownerCanChangeRole() throws Exception {
//        ShareRequestDto shareRequest = new ShareRequestDto(ownerFileId, "target@test.com", Roles.READER);
//
//        mockMvc.perform(post("/api/permission/share")
//                        .session(ownerSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isCreated());
//
//        MvcResult result = mockMvc.perform(put("/api/permission/{fileId}/role/{targetUserId}", ownerFileId, targetId)
//                        .session(ownerSession)
//                        .param("role", "EDITOR"))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        FilePermissionDto response = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                FilePermissionDto.class
//        );
//
//        assertEquals(Roles.EDITOR, response.role());
//    }
//
//    @Test
//    @Order(3)
//    void userCanGetSharedFiles() throws Exception {
//        ShareRequestDto shareRequest = new ShareRequestDto(ownerFileId, "target@test.com", Roles.READER);
//
//        mockMvc.perform(post("/api/permission/share")
//                        .session(ownerSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isCreated());
//
//        MvcResult result = mockMvc.perform(get("/api/permission/shared-with-me")
//                        .session(targetSession))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        FilePermissionDto[] sharedFiles = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                FilePermissionDto[].class
//        );
//
//        assertTrue(sharedFiles.length > 0);
//        assertEquals(ownerFileId, sharedFiles[0].fileId());
//    }
//
//    @Test
//    @Order(4)
//    void ownerCanGetFileAccessors() throws Exception {
//        ShareRequestDto shareRequest = new ShareRequestDto(ownerFileId, "target@test.com", Roles.READER);
//
//        mockMvc.perform(post("/api/permission/share")
//                        .session(ownerSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isCreated());
//
//        MvcResult result = mockMvc.perform(get("/api/permission/{fileId}/accessors", ownerFileId)
//                        .session(ownerSession))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        FilePermissionDto[] accessors = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                FilePermissionDto[].class
//        );
//
//        assertTrue(accessors.length > 0);
//        assertEquals(targetId, accessors[0].user().id());
//    }
//
//    @Test
//    @Order(5)
//    void ownerCanRevokeAccess() throws Exception {
//        ShareRequestDto shareRequest = new ShareRequestDto(ownerFileId, "target@test.com", Roles.READER);
//
//        mockMvc.perform(post("/api/permission/share")
//                        .session(ownerSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isCreated());
//
//        mockMvc.perform(delete("/api/permission/{fileId}/revoke/{targetUserId}", ownerFileId, targetId)
//                        .session(ownerSession))
//                .andExpect(status().isNoContent());
//
//        MvcResult result = mockMvc.perform(get("/api/permission/shared-with-me")
//                        .session(targetSession))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        FilePermissionDto[] sharedFiles = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                FilePermissionDto[].class
//        );
//
//        assertEquals(0, sharedFiles.length);
//    }
//
//    @Test
//    @Order(6)
//    void cannotShareWithYourself() throws Exception {
//        ShareRequestDto shareRequest = new ShareRequestDto(ownerFileId, "owner@test.com", Roles.READER);
//
//        mockMvc.perform(post("/api/permission/share")
//                        .session(ownerSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @Order(7)
//    void nonOwnerCannotShare() throws Exception {
//        ShareRequestDto shareRequest = new ShareRequestDto(ownerFileId, "target@test.com", Roles.READER);
//
//        mockMvc.perform(post("/api/permission/share")
//                        .session(otherSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @Order(8)
//    void cannotShareNonExistentFile() throws Exception {
//        UUID nonExistentFileId = UUID.randomUUID();
//        ShareRequestDto shareRequest = new ShareRequestDto(nonExistentFileId, "target@test.com", Roles.READER);
//
//        mockMvc.perform(post("/api/permission/share")
//                        .session(ownerSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @Order(9)
//    void cannotShareWithNonExistentUser() throws Exception {
//        ShareRequestDto shareRequest = new ShareRequestDto(ownerFileId, "nonexistent@test.com", Roles.READER);
//
//        mockMvc.perform(post("/api/permission/share")
//                        .session(ownerSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @Order(10)
//    void nonOwnerCannotRevoke() throws Exception {
//        ShareRequestDto shareRequest = new ShareRequestDto(ownerFileId, "target@test.com", Roles.READER);
//
//        mockMvc.perform(post("/api/permission/share")
//                        .session(ownerSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isCreated());
//
//        mockMvc.perform(delete("/api/permission/{fileId}/revoke/{targetUserId}", ownerFileId, targetId)
//                        .session(otherSession))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @Order(11)
//    void nonOwnerCannotChangeRole() throws Exception {
//        ShareRequestDto shareRequest = new ShareRequestDto(ownerFileId, "target@test.com", Roles.READER);
//
//        mockMvc.perform(post("/api/permission/share")
//                        .session(ownerSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isCreated());
//
//        mockMvc.perform(put("/api/permission/{fileId}/role/{targetUserId}", ownerFileId, targetId)
//                        .session(otherSession)
//                        .param("role", "EDITOR"))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @Order(12)
//    void nonOwnerCannotGetAccessors() throws Exception {
//        ShareRequestDto shareRequest = new ShareRequestDto(ownerFileId, "target@test.com", Roles.READER);
//
//        mockMvc.perform(post("/api/permission/share")
//                        .session(ownerSession)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(shareRequest)))
//                .andExpect(status().isCreated());
//
//        mockMvc.perform(get("/api/permission/{fileId}/accessors", ownerFileId)
//                        .session(otherSession))
//                .andExpect(status().isForbidden());
//    }
//}