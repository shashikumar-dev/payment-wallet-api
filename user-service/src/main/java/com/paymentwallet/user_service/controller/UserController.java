package com.paymentwallet.user_service.controller;

import com.paymentwallet.user_service.dto.UserLookupResponse;
import com.paymentwallet.user_service.dto.UserResponse;
import com.paymentwallet.user_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(
            Authentication authentication
    ) {

        Long userId =
                (Long) authentication.getPrincipal();

        return userService.getUserById(userId);
    }

    @GetMapping("/lookup")
    public ResponseEntity<UserLookupResponse>
    lookupUser(
            @RequestParam String walletPayId
    ) {

        return ResponseEntity.ok(
                userService.findByWalletPayId(
                        walletPayId
                )
        );
    }

    @PostMapping("/lookup-by-ids")
    public List<UserLookupResponse> lookupUsersByIds(
            @RequestBody List<Long> userIds
    ) {

        return userService.findUsersByIds(
                userIds
        );
    }
}