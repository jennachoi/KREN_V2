package com.kren.letter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendLetterRequest(@NotNull Long recipientId, @NotBlank @Size(max = 5000) String body) {
}
