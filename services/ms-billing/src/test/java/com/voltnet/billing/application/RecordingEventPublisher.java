package com.voltnet.billing.application;

import com.voltnet.billing.domain.event.UserDebtUpdatedEvent;
import com.voltnet.billing.domain.port.out.UserDebtEventPublisher;

import java.util.ArrayList;
import java.util.List;

public class RecordingEventPublisher implements UserDebtEventPublisher {

    private final List<UserDebtUpdatedEvent> published = new ArrayList<>();

    @Override
    public void publish(UserDebtUpdatedEvent event) {
        published.add(event);
    }

    public List<UserDebtUpdatedEvent> events() {
        return published;
    }
}
