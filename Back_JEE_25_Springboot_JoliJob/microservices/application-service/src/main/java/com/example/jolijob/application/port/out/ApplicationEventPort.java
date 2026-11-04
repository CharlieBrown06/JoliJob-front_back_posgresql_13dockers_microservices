package com.example.jolijob.application.port.out;

import com.example.jolijob.application.domain.ApplicationEvent;

public interface ApplicationEventPort {
    void publish(ApplicationEvent event);
}
