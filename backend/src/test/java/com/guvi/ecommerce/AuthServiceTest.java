package com.guvi.ecommerce;

import com.guvi.ecommerce.config.JwtUtil;
import com.guvi.ecommerce.dto.*;
import com.guvi.ecommerce.entity.User;
import com.guvi.ecommerce.repository.UserRepository;
import com.guvi.ecommerce.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alice");
        req.setEmail("alice@test.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken("alice@test.com", "CUSTOMER")).thenReturn("token123");

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dup@test.com");
        req.setPassword("pass");

        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void login_success() {
        AuthRequest req = new AuthRequest();
        req.setEmail("user@test.com");
        req.setPassword("pass");

        User user = User.builder().email("user@test.com").name("User").role(User.Role.CUSTOMER).build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("user@test.com", "CUSTOMER")).thenReturn("jwt");

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt");
    }
}
