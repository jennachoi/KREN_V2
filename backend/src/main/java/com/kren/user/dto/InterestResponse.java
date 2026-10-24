package com.kren.user.dto;

import com.kren.user.Interest;

public record InterestResponse(Long id, String name) {

	public static InterestResponse from(Interest interest) {
		return new InterestResponse(interest.getId(), interest.getName());
	}

}
