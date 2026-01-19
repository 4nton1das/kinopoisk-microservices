package com.example.kinopoiskapicontract.dto.user;

import org.springframework.hateoas.RepresentationModel;

public class UserDTO extends RepresentationModel<UserDTO> {
    private final Long id;
    private final String username;
    private final String email;

    public UserDTO(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
