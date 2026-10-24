package com.kren.stamp.dto;

import java.time.LocalDate;

public record CheckinResponse(LocalDate date, int reward, int balance) {
}
