package com.guvi.ecommerce.dto;

import com.guvi.ecommerce.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSummary {
    private Long id;
    private String name;
    private String email;

    public static UserSummary from(User user) {
        if (user == null) return null;
        return new UserSummary(user.getId(), user.getName(), user.getEmail());
    }
}
