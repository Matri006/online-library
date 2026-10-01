package ru.mospolytech.library.events;

import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class OperationEvents {
    public record Before(String operation) {}

    public record Changed(String operation, long id) {}

    @EventListener
    public void before(Before event) {
        LoggerFactory.getLogger(getClass()).debug("Before {}", event.operation());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void after(Changed event) {
        LoggerFactory.getLogger(getClass())
                .info("Committed {} id={}", event.operation(), event.id());
    }
}
