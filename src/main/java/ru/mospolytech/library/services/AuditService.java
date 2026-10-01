package ru.mospolytech.library.services;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.AppUser;
import ru.mospolytech.library.exceptions.LibraryException;
import ru.mospolytech.library.repository.AuditRepository;
import ru.mospolytech.library.repository.UserRepository;

import java.util.List;

@Service
public class AuditService {
    private final AuditRepository audit;
    private final UserRepository users;

    public AuditService(AuditRepository audit, UserRepository users) {
        this.audit = audit;
        this.users = users;
    }

    public Long actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return users.findByLoginIgnoreCase(authentication.getName())
                .map(AppUser::userId)
                .orElse(null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long begin() {
        Long id = actor();
        if (id == null) {
            throw new LibraryException("ACCESS_DENIED");
        }
        audit.setActor(id);
        return id;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(String code, String path) {
        audit.logFailure(actor(), code, path);
    }

    @Transactional(readOnly = true)
    public List<Responses.Audit> findAll(int offset, String query) {
        return audit.findAll(Math.max(0, offset), query);
    }
}
