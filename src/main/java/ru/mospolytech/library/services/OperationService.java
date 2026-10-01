package ru.mospolytech.library.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.events.OperationEvents;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class OperationService {
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public OperationService(AuditService audit, ApplicationEventPublisher events) {
        this.audit = audit;
        this.events = events;
    }

    public long begin(String operation) {
        events.publishEvent(new OperationEvents.Before(operation));
        return audit.begin();
    }

    public long complete(String operation, long id) {
        events.publishEvent(new OperationEvents.Changed(operation, id));
        return id;
    }
}
