package com.paymentwallet.user_service.service;

import com.paymentwallet.user_service.dto.*;
import com.paymentwallet.user_service.entity.User;
import com.paymentwallet.user_service.entity.UserStatus;
import com.paymentwallet.user_service.exception.EmailAlreadyExistsException;
import com.paymentwallet.user_service.exception.InvalidCredentialsException;
import com.paymentwallet.user_service.exception.UserNotFoundException;
import com.paymentwallet.user_service.exception.WalletPayIdAlreadyExistsException;
import com.paymentwallet.user_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse register(RegisterRequest request) {

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(
                    "Email is already registered"
            );
        }

        String walletPayId =
                request.walletPayId()
                        .trim()
                        .toLowerCase()
                        + "@walletpay";

        if (userRepository.existsByWalletPayId(walletPayId)) {
            throw new WalletPayIdAlreadyExistsException();
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        User user = new User(
                request.name().trim(),
                normalizedEmail,
                walletPayId,
                passwordHash
        );

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getWalletPayId(),
                savedUser.getStatus(),
                savedUser.getCreatedAt()
        );
    }

    public User authenticate(LoginRequest request) {

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase();

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    public UserResponse getUserById(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new UserNotFoundException(userId)
                );

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getWalletPayId(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    public UserLookupResponse findByWalletPayId(
            String walletPayId
    ) {

        String normalizedWalletPayId =
                walletPayId
                        .trim()
                        .toLowerCase();

        User user = userRepository
                .findByWalletPayId(normalizedWalletPayId)
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "WalletPay user not found"
                        )
                );

        return toLookupResponse(user);
    }

    public List<UserLookupResponse> findUsersByIds(
            List<Long> userIds
    ) {

        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return userRepository
                .findAllById(userIds)
                .stream()
                .map(this::toLookupResponse)
                .toList();
    }

    private UserLookupResponse toLookupResponse(
            User user
    ) {

        return new UserLookupResponse(
                user.getId(),
                user.getName(),
                user.getWalletPayId()
        );
    }
}