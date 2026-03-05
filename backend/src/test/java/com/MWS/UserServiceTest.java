package com.MWS;

import com.MWS.db.postgresql.repository.UserRepository;
import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.model.UserEntity;
import com.MWS.service.UserServiceRelease;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository UserRepository;

    @InjectMocks
    private UserServiceRelease UserServiceRelease;

    @Test
    public void SelectAndSuccessfulRegistration() {
        CreateUserDTO successfulDto = new CreateUserDTO(
                "Аня",
                "testuser1@mail.ru",
                "+79106754432",
                "123456"
        );
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setName("Аня");
        user.setEmail("testuser1@mail.ru");
        user.setPhoneNumber("+79106754432");
        user.setPassword("123456");

        when(UserRepository.findByEmail("testuser1@mail.ru")).thenReturn(Optional.empty());
        when(UserRepository.save(any(UserEntity.class))).thenReturn(user);

        GetSimpleUserDto result = UserServiceRelease.createUser(successfulDto);

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("testuser1@mail.ru", result.email());
        assertEquals("+79106754432", result.phoneNumber());

        verify(UserRepository).findByEmail("testuser1@mail.ru");
        verify(UserRepository).save(any(UserEntity.class));
    }

    @Test
    public void InvalidPhoneNumberRegistration() {
        CreateUserDTO dto = new CreateUserDTO(
                "Борис",
                "testuser2@mail.ru",
                "+9-167-54432",
                "654321"
        );


        UUID userId = UUID.randomUUID();
        UserEntity User = new UserEntity();
        User.setId(userId);
        User.setName("Борис");
        User.setEmail("testuser2@mail.ru");
        User.setPhoneNumber("+9-167-54432");
        User.setPassword("654321");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            UserServiceRelease.createUser(dto);
        });
        assertTrue(exception.getMessage().contains("Некорректный номер телефона"));

        verify(UserRepository, never()).findByEmail("testuser2@mail.ru");
        verify(UserRepository, never()).save(any(UserEntity.class));
    }

    @Test
    public void DoubleEmailRegistration () {
        CreateUserDTO dto = new CreateUserDTO(
                "Пользователь3",
                "testuser1@mail.ru",
                "+74532657888",
                "4087893"
        );

        UUID userId = UUID.randomUUID();
        UserEntity User = new UserEntity();
        User.setId(userId);
        User.setName("Пользователь3");
        User.setEmail("testuser1@mail.ru");
        User.setPhoneNumber("+74532657888");
        User.setPassword("4087893");

        when(UserRepository.findByEmail("testuser1@mail.ru"))
                .thenReturn(Optional.of(User));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            UserServiceRelease.createUser(dto);
        });

        assertTrue(exception.getMessage().contains("Email " + dto.email() + " уже занят."));
        verify(UserRepository).findByEmail("testuser1@mail.ru");
        verify(UserRepository, never()).save(any(UserEntity.class));
    }

    @Test
    public void SelectAndOneMoreSuccessfulRegistration() {
        CreateUserDTO successfulDto = new CreateUserDTO(
                "Глеб123",
                "gglleebb@gmail.com",
                "+76665554433",
                "jqewgfiygqi"
        );

        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setName("Глеб123");
        user.setEmail("gglleebb@gmail.com");
        user.setPhoneNumber("+76665554433");
        user.setPassword("jqewgfiygqi");

        when(UserRepository.findByEmail("gglleebb@gmail.com")).thenReturn(Optional.empty());
        when(UserRepository.save(any(UserEntity.class))).thenReturn(user);

        GetSimpleUserDto result = UserServiceRelease.createUser(successfulDto);

        assertNotNull(result);
        assertEquals("+76665554433", result.phoneNumber());

        verify(UserRepository).findByEmail("gglleebb@gmail.com");
        verify(UserRepository).save(any(UserEntity.class));
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
            UserServiceRelease.createUser(successfulDtoTwo);
        });
        assertTrue(exception.getMessage().contains("Пароль должен быть от 6 до 100 символов"));

        verify(UserRepository, never()).findByEmail("daria@gmail.com");
        verify(UserRepository, never()).save(any(UserEntity.class));
    }

}
