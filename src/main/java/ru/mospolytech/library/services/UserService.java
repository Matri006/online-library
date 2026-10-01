package ru.mospolytech.library.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.AppUser;
import ru.mospolytech.library.exceptions.LibraryException;
import ru.mospolytech.library.repository.RoleRepository;
import ru.mospolytech.library.repository.UserRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordService passwords;
    private final OperationService operations;

    public UserService(
            UserRepository users,
            RoleRepository roles,
            PasswordService passwords,
            OperationService operations) {
        this.users = users;
        this.roles = roles;
        this.passwords = passwords;
        this.operations = operations;
    }

    public List<Responses.User> findAll() {
        return users.findAll();
    }

    @Transactional
    public long save(Long id, Requests.User request) {
        long actor = operations.begin("save_user");
        if (id != null && id == actor && (!request.active() || !request.role().equals("ADMIN"))) {
            throw new LibraryException("SELF_LOCKOUT");
        }
        String hash = null;
        if (id == null || (request.password() != null && !request.password().isEmpty())) {
            hash = passwords.encode(request.password());
        }
        var role =
                roles.findByCode(request.role())
                        .orElseThrow(() -> new LibraryException("INVALID_DATA"));
        var user =
                new AppUser(
                        id,
                        request.login().toLowerCase(java.util.Locale.ROOT),
                        hash,
                        role.roleId(),
                        request.active());
        if (id == null) {
            id = users.insert(user);
        } else if (!users.update(user)) {
            throw new LibraryException("ENTITY_NOT_FOUND");
        }
        return operations.complete("save_user", id);
    }
}
