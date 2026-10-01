package ru.mospolytech.library.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.Student;
import ru.mospolytech.library.exceptions.LibraryException;
import ru.mospolytech.library.repository.StudentRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository students;
    private final OperationService operations;

    public StudentService(StudentRepository students, OperationService operations) {
        this.students = students;
        this.operations = operations;
    }

    public List<Responses.Student> findAll(String query, int offset) {
        return students.findAll(query, Math.max(0, offset));
    }

    @Transactional
    public long save(Long id, Requests.Student request) {
        operations.begin("save_student");
        var entity =
                new Student(
                        id,
                        request.studentCardNumber().trim(),
                        request.fullName().trim(),
                        request.facultyId(),
                        request.active());
        if (id == null) {
            id = students.insert(entity);
        } else if (!students.update(entity)) {
            throw new LibraryException("ENTITY_NOT_FOUND");
        }
        return operations.complete("save_student", id);
    }
}
