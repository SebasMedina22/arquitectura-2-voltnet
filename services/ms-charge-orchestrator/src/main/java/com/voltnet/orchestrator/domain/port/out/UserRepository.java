package com.voltnet.orchestrator.domain.port.out;

import com.voltnet.orchestrator.domain.model.User;
import com.voltnet.orchestrator.domain.model.UserId;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(UserId id);

    void save(User user);
}
