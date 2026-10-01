package ru.mospolytech.library.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.entities.AppUser;
import ru.mospolytech.library.repository.DemoDataRepository;
import ru.mospolytech.library.repository.RoleRepository;
import ru.mospolytech.library.repository.UserRepository;

@Service
public class BootstrapService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final DemoDataRepository demoData;
    private final PasswordService passwords;

    public BootstrapService(
            UserRepository users,
            RoleRepository roles,
            DemoDataRepository demoData,
            PasswordService passwords) {
        this.users = users;
        this.roles = roles;
        this.demoData = demoData;
        this.passwords = passwords;
    }

    @Transactional
    public void initialize(
            boolean demo, String adminPassword, String librarianPassword, String viewerPassword) {
        users.lockBootstrap();
        if (users.existsAny()) {
            return;
        }
        createUser("admin", adminPassword, "ADMIN");
        if (demo) {
            createUser("librarian", librarianPassword, "LIBRARIAN");
            createUser("viewer", viewerPassword, "VIEWER");
            demoData.populate();
        }
    }

    private void createUser(String login, String password, String roleCode) {
        var role =
                roles.findByCode(roleCode)
                        .orElseThrow(() -> new IllegalStateException("Missing role: " + roleCode));
        users.insert(new AppUser(null, login, passwords.encode(password), role.roleId(), true));
    }
}
