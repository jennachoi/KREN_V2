package com.kren.letter.dto;

public record RandomQuotaResponse(int limit, int used, int remaining) {
}
