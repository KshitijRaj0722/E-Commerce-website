package com.guvi.ecommerce.service;

import com.guvi.ecommerce.dto.UpdateProfileRequest;
import com.guvi.ecommerce.dto.UserResponse;
import com.guvi.ecommerce.entity.User;
import com.guvi.ecommerce.exception.BadRequestException;
import com.guvi.ecommerce.exception.ResourceNotFoundException;
import com.guvi.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getProfile(String email) {
        return UserResponse.from(getUser(email));
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getUser(email);
        user.setName(request.getName());
        user.setPhone(request.getPhone());

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null
                    || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
