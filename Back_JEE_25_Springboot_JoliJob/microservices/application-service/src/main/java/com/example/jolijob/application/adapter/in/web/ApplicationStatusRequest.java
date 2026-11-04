package com.example.jolijob.application.adapter.in.web;

import com.example.jolijob.application.domain.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationStatusRequest(@NotNull ApplicationStatus status) {
}
