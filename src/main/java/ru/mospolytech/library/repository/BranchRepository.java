package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.entities.Branch;

import java.util.LinkedHashMap;
import java.util.List;

@Repository
public class BranchRepository {
    private final JdbcTemplate jdbc;
    private final JsonColumns json;

    public BranchRepository(JdbcTemplate jdbc, JsonColumns json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<Branch> findAll() {
        return jdbc.query(
                "select * from branch order by name", new DataClassRowMapper<>(Branch.class));
    }

    public long save(Branch branch) {
        var data = new LinkedHashMap<String, Object>();
        data.put("name", branch.name());
        data.put("address", branch.address());
        data.put("branchType", branch.branchType());
        data.put("phone", branch.phone());
        data.put("active", branch.isActive());
        if (branch.branchId() == null) {
            return ((Number)
                            jdbc.queryForMap(
                                            "call library_api.add_branch(?::jsonb, null)",
                                            json.write(data))
                                    .get("id"))
                    .longValue();
        }
        jdbc.update(
                "call library_api.update_branch(?, ?::jsonb)", branch.branchId(), json.write(data));
        return branch.branchId();
    }
}
