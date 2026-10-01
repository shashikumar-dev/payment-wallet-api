package com.paymentwallet.user_service.repository;

import com.paymentwallet.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    boolean existsByWalletPayId(String walletPayId);

    Optional<User> findByWalletPayId(String walletPayId);
}