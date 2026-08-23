package com.guvi.ecommerce.service;

import com.guvi.ecommerce.dto.UpdateProfileRequest;
import com.guvi.ecommerce.dto.UserResponse;
import com.guvi.ecommerce.entity.User;
import com.guvi.ecommerce.exception.BadRequestException;
import com.guvi.ecommerce.exception.ResourceNotFoundException;
import com.guvi.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Old Name").email("buyer@test.com")
                .password("hashed-old").phone("111").role(User.Role.CUSTOMER).build();
    }

    private UpdateProfileRequest request(String name, String phone) {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName(name);
        req.setPhone(phone);
        return req;
    }

    @Test
    void getProfile_returnsResponseWithoutPassword() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getProfile("buyer@test.com");

        assertThat(response.getEmail()).isEqualTo("buyer@test.com");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    void getProfile_unknownUser_throwsNotFound() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("ghost@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_updatesNameAndPhone() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateProfile("buyer@test.com", request("New Name", "999"));

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getPhone()).isEqualTo("999");
        assertThat(user.getPassword()).isEqualTo("hashed-old");
    }

    @Test
    void updateProfile_withCorrectCurrentPassword_reencodesNewPassword() {
        UpdateProfileRequest req = request("New Name", "999");
        req.setCurrentPassword("old-plain");
        req.setNewPassword("new-plain");

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-plain", "hashed-old")).thenReturn(true);
        when(passwordEncoder.encode("new-plain")).thenReturn("hashed-new");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateProfile("buyer@test.com", req);

        assertThat(user.getPassword()).isEqualTo("hashed-new");
    }

    @Test
    void updateProfile_withWrongCurrentPassword_isRejected() {
        UpdateProfileRequest req = request("New Name", "999");
        req.setCurrentPassword("wrong");
        req.setNewPassword("new-plain");

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-old")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateProfile("buyer@test.com", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_blankNewPassword_keepsExistingPassword() {
        UpdateProfileRequest req = request("New Name", "999");
        req.setNewPassword("   ");

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateProfile("buyer@test.com", req);

        assertThat(user.getPassword()).isEqualTo("hashed-old");
        verify(passwordEncoder, never()).encode(any());
    }
}
