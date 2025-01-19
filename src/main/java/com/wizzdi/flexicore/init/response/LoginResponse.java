package com.wizzdi.flexicore.init.response;


import com.flexicore.model.User;

public record LoginResponse(
       String id,
       String username,
       String name,
       String lastName) {
    public static LoginResponse ofUser(User user) {

            return new LoginResponse(user.getId(),user.getEmail(),user.getName(),user.getLastName());
    }
}
