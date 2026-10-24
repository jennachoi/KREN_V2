package com.kren.user;

import java.util.List;
import java.util.Set;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kren.auth.AuthUser;
import com.kren.common.PageResponse;
import com.kren.user.dto.InterestResponse;
import com.kren.user.dto.MyProfileResponse;
import com.kren.user.dto.PublicProfileResponse;
import com.kren.user.dto.UpdateProfileRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/users/me")
	public MyProfileResponse me(@AuthenticationPrincipal AuthUser me) {
		return userService.getMyProfile(me.id());
	}

	@PutMapping("/users/me")
	public MyProfileResponse updateMe(@AuthenticationPrincipal AuthUser me,
			@Valid @RequestBody UpdateProfileRequest request) {
		return userService.updateProfile(me.id(), request);
	}

	@GetMapping("/users/{userId}")
	public PublicProfileResponse profile(@AuthenticationPrincipal AuthUser me, @PathVariable long userId) {
		return userService.getPublicProfile(me.id(), userId);
	}

	/** {@code ?interestId=1&interestId=4} narrows to penpals sharing at least one of those interests. */
	@GetMapping("/penpals")
	public PageResponse<PublicProfileResponse> penpals(@AuthenticationPrincipal AuthUser me,
			@RequestParam(name = "interestId", required = false) Set<Long> interestIds,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return userService.findPenpals(me.id(), interestIds, page, size);
	}

	@GetMapping("/interests")
	public List<InterestResponse> interests() {
		return userService.listInterests();
	}

}
