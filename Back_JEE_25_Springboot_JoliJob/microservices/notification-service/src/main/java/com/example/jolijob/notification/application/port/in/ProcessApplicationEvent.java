package com.example.jolijob.notification.application.port.in;

import com.example.jolijob.notification.application.domain.ApplicationEvent;

public interface ProcessApplicationEvent {
    void process(ApplicationEvent event);
}
