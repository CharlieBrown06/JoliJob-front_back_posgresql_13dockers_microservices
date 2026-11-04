package com.example.jolijob.application.port.out;

import com.example.jolijob.application.domain.Application;
import com.example.jolijob.shared.event.ApplicationEventType;

public interface ApplicationEventPort {
    void publish(Application application, ApplicationEventType type);
}
