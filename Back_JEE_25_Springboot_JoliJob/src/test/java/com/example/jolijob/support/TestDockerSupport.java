package com.example.jolijob.support;

import org.testcontainers.DockerClientFactory;

public final class TestDockerSupport {

    private TestDockerSupport() {
    }

    public static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        }
        catch (Throwable ex) {
            return false;
        }
    }
}

