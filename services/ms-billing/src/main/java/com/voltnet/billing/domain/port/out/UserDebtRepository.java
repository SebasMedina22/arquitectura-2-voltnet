package com.voltnet.billing.domain.port.out;

import com.voltnet.billing.domain.model.UserDebt;
import com.voltnet.billing.domain.model.UserId;

import java.util.Optional;

public interface UserDebtRepository {

    Optional<UserDebt> findByUserId(UserId userId);

    UserDebt save(UserDebt debt);
}
