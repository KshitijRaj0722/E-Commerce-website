package com.guvi.ecommerce.repository;

import com.guvi.ecommerce.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {
        return userRepository.save(User.builder()
                .name("Test User")
                .email(email)
                .password("hashed")
                .role(User.Role.CUSTOMER)
                .build());
    }

    @Test
    void findByEmail_returnsPersistedUser() {
        persistUser("found@test.com");

        Optional<User> result = userRepository.findByEmail("found@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test User");
    }

    @Test
    void findByEmail_unknownEmail_returnsEmpty() {
        assertThat(userRepository.findByEmail("nobody@test.com")).isEmpty();
    }

    @Test
    void existsByEmail_reflectsPersistence() {
        persistUser("exists@test.com");

        assertThat(userRepository.existsByEmail("exists@test.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@test.com")).isFalse();
    }

    @Test
    void createdAt_isPopulatedOnPersist() {
        User saved = persistUser("stamped@test.com");

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
