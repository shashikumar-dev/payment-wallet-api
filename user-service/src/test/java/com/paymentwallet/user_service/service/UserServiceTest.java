package com.paymentwallet.user_service.service;

import com.paymentwallet.user_service.dto.RegisterRequest;
import com.paymentwallet.user_service.dto.RegisterResponse;
import com.paymentwallet.user_service.entity.User;
import com.paymentwallet.user_service.exception.EmailAlreadyExistsException;
import com.paymentwallet.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterRequest request = new RegisterRequest(
                "Shashi",
                "shashi@walletpay",
                "shashi@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("shashi@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashed-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response =
                userService.register(request);

        assertEquals("Shashi", response.name());
        assertEquals("shashi@example.com", response.email());

        verify(userRepository)
                .existsByEmail("shashi@example.com");

        verify(passwordEncoder)
                .encode("password123");

        verify(userRepository)
                .save(any(User.class));
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest(
                "Piya",
                "priya@walletpay",
                "shashi@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("shashi@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.register(request)
        );

        verify(userRepository)
                .existsByEmail("shashi@example.com");

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(userRepository, never())
                .save(any(User.class));
    }
}