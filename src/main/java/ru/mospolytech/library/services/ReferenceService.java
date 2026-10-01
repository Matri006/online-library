package ru.mospolytech.library.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.Author;
import ru.mospolytech.library.entities.Faculty;
import ru.mospolytech.library.entities.Publisher;
import ru.mospolytech.library.entities.ReferenceType;
import ru.mospolytech.library.exceptions.LibraryException;
import ru.mospolytech.library.repository.AuthorRepository;
import ru.mospolytech.library.repository.FacultyRepository;
import ru.mospolytech.library.repository.PublisherRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReferenceService {
    private final AuthorRepository authors;
    private final PublisherRepository publishers;
    private final FacultyRepository faculties;
    private final OperationService operations;

    public ReferenceService(
            AuthorRepository authors,
            PublisherRepository publishers,
            FacultyRepository faculties,
            OperationService operations) {
        this.authors = authors;
        this.publishers = publishers;
        this.faculties = faculties;
        this.operations = operations;
    }

    private ReferenceType parseType(String type) {
        return switch (type) {
            case "authors" -> ReferenceType.AUTHORS;
            case "publishers" -> ReferenceType.PUBLISHERS;
            case "faculties" -> ReferenceType.FACULTIES;
            default -> throw new LibraryException("ENTITY_NOT_FOUND");
        };
    }

    public List<Responses.Reference> findAll(String type, String query, int offset) {
        int safeOffset = Math.max(0, offset);
        return switch (parseType(type)) {
            case AUTHORS ->
                    authors.findAll(query, safeOffset).stream()
                            .map(
                                    author ->
                                            new Responses.Reference(
                                                    author.authorId(),
                                                    null,
                                                    null,
                                                    author.fullName(),
                                                    author.fullName(),
                                                    null))
                            .toList();
            case PUBLISHERS ->
                    publishers.findAll(query, safeOffset).stream()
                            .map(
                                    publisher ->
                                            new Responses.Reference(
                                                    null,
                                                    publisher.publisherId(),
                                                    null,
                                                    null,
                                                    publisher.name(),
                                                    null))
                            .toList();
            case FACULTIES ->
                    faculties.findAll(query, safeOffset).stream()
                            .map(
                                    faculty ->
                                            new Responses.Reference(
                                                    null,
                                                    null,
                                                    faculty.facultyId(),
                                                    null,
                                                    faculty.name(),
                                                    faculty.isActive()))
                            .toList();
        };
    }

    @Transactional
    public long save(String type, Long id, Requests.Reference request) {
        operations.begin("save_reference");
        long savedId =
                switch (parseType(type)) {
                    case AUTHORS -> saveAuthor(new Author(id, request.name().trim()));
                    case PUBLISHERS -> savePublisher(new Publisher(id, request.name().trim()));
                    case FACULTIES ->
                            saveFaculty(new Faculty(id, request.name().trim(), request.active()));
                };
        return operations.complete("save_reference", savedId);
    }

    private long saveAuthor(Author author) {
        if (author.authorId() == null) {
            return authors.insert(author);
        }
        if (!authors.update(author)) {
            throw new LibraryException("ENTITY_NOT_FOUND");
        }
        return author.authorId();
    }

    private long savePublisher(Publisher publisher) {
        if (publisher.publisherId() == null) {
            return publishers.insert(publisher);
        }
        if (!publishers.update(publisher)) {
            throw new LibraryException("ENTITY_NOT_FOUND");
        }
        return publisher.publisherId();
    }

    private long saveFaculty(Faculty faculty) {
        if (faculty.facultyId() == null) {
            return faculties.insert(faculty);
        }
        if (!faculties.update(faculty)) {
            throw new LibraryException("ENTITY_NOT_FOUND");
        }
        return faculty.facultyId();
    }
}
