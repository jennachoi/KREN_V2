package com.kren.letter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RandomLetterRequest(@NotBlank @Size(max = 5000) String body) {
}
