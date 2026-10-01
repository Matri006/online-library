package ru.mospolytech.library.services;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.repository.RoleRepository;
import ru.mospolytech.library.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class AccountService implements UserDetailsService {
    private final UserRepository users;
    private final RoleRepository roles;

    public AccountService(UserRepository users, RoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var user =
                users.findByLoginIgnoreCase(username.trim())
                        .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
        var role =
                roles.findById(user.roleId())
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "Роль пользователя не найдена"));
        return User.withUsername(user.login())
                .password(user.passwordHash())
                .roles(role.code())
                .disabled(!user.isActive())
                .build();
    }
}
