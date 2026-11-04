package com.example.jolijob.application.web.dto;

import com.example.jolijob.application.domain.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationStatusRequest(@NotNull ApplicationStatus status) {}
