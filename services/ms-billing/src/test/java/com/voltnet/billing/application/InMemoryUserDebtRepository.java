package com.voltnet.billing.application;

import com.voltnet.billing.domain.model.UserDebt;
import com.voltnet.billing.domain.model.UserId;
import com.voltnet.billing.domain.port.out.UserDebtRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserDebtRepository implements UserDebtRepository {

    private final Map<String, UserDebt> store = new HashMap<>();

    @Override
    public Optional<UserDebt> findByUserId(UserId userId) {
        return Optional.ofNullable(store.get(userId.value()));
    }

    @Override
    public UserDebt save(UserDebt debt) {
        store.put(debt.userId().value(), debt);
        return debt;
    }
}
