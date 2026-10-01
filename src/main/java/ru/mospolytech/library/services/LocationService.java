package ru.mospolytech.library.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.StorageLocation;
import ru.mospolytech.library.exceptions.LibraryException;
import ru.mospolytech.library.repository.LocationRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LocationService {
    private final LocationRepository locations;
    private final OperationService operations;

    public LocationService(LocationRepository locations, OperationService operations) {
        this.locations = locations;
        this.operations = operations;
    }

    public List<Responses.Location> findAll() {
        return locations.findAll();
    }

    @Transactional
    public long save(Long id, Requests.Location request) {
        operations.begin("save_location");
        var entity =
                new StorageLocation(
                        id,
                        request.branchId(),
                        request.code().trim(),
                        request.name().trim(),
                        request.active());
        if (id == null) {
            id = locations.insert(entity);
        } else if (!locations.update(entity)) {
            throw new LibraryException("ENTITY_NOT_FOUND");
        }
        return operations.complete("save_location", id);
    }
}
