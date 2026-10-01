package ru.mospolytech.library.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.Branch;
import ru.mospolytech.library.repository.BranchRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BranchService {
    private final BranchRepository branches;
    private final OperationService operations;

    public BranchService(BranchRepository branches, OperationService operations) {
        this.branches = branches;
        this.operations = operations;
    }

    public List<Responses.Branch> findAll() {
        return branches.findAll().stream()
                .map(
                        branch ->
                                new Responses.Branch(
                                        branch.branchId(),
                                        branch.name(),
                                        branch.address(),
                                        branch.branchType(),
                                        branch.phone(),
                                        branch.isActive()))
                .toList();
    }

    @Transactional
    public long save(Long id, Requests.Branch request) {
        operations.begin("save_branch");
        var branch =
                new Branch(
                        id,
                        request.name(),
                        request.address(),
                        request.branchType(),
                        request.phone(),
                        request.active());
        return operations.complete("save_branch", branches.save(branch));
    }
}
