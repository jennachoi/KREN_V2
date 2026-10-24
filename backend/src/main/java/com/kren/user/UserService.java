package com.kren.user;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kren.block.BlockRepository;
import com.kren.common.ApiException;
import com.kren.common.ErrorCode;
import com.kren.common.PageResponse;
import com.kren.user.dto.InterestResponse;
import com.kren.user.dto.MyProfileResponse;
import com.kren.user.dto.PublicProfileResponse;
import com.kren.user.dto.UpdateProfileRequest;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final InterestRepository interestRepository;
	private final BlockRepository blockRepository;

	public UserService(UserRepository userRepository, InterestRepository interestRepository,
			BlockRepository blockRepository) {
		this.userRepository = userRepository;
		this.interestRepository = interestRepository;
		this.blockRepository = blockRepository;
	}

	public MyProfileResponse getMyProfile(long meId) {
		return MyProfileResponse.from(load(meId));
	}

	/** Blocked users (in either direction) look nonexistent rather than forbidden. */
	public PublicProfileResponse getPublicProfile(long meId, long userId) {
		User user = load(userId);
		if (meId != userId && blockRepository.existsBetween(meId, userId)) {
			throw new ApiException(ErrorCode.USER_NOT_FOUND);
		}
		return PublicProfileResponse.from(user);
	}

	@Transactional
	public MyProfileResponse updateProfile(long meId, UpdateProfileRequest req) {
		requireDifferentLanguages(req.nativeLang(), req.learningLang());
		List<Interest> interests = interestRepository.findAllById(req.interestIds());
		if (interests.size() != req.interestIds().size()) {
			throw new ApiException(ErrorCode.INVALID_INTEREST);
		}
		User me = load(meId);
		me.updateProfile(req.displayName().strip(), req.country(), req.nativeLang(), req.learningLang(),
				blankToNull(req.bio()), new HashSet<>(interests));
		return MyProfileResponse.from(me);
	}

	public PageResponse<PublicProfileResponse> findPenpals(long meId, Set<Long> interestIds, int page, int size) {
		User me = load(meId);
		PageRequest pageable = PageRequest.of(page, size);
		var result = (interestIds == null || interestIds.isEmpty())
				? userRepository.findPenpals(meId, me.getNativeLang(), me.getLearningLang(), pageable)
				: userRepository.findPenpalsWithAnyInterest(meId, me.getNativeLang(), me.getLearningLang(),
						interestIds, pageable);
		return PageResponse.of(result, PublicProfileResponse::from);
	}

	public List<InterestResponse> listInterests() {
		return interestRepository.findAll().stream().map(InterestResponse::from).toList();
	}

	public static void requireDifferentLanguages(String nativeLang, String learningLang) {
		if (nativeLang.equals(learningLang)) {
			throw new ApiException(ErrorCode.LANGUAGES_MUST_DIFFER);
		}
	}

	private User load(long id) {
		return userRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
	}

	private static String blankToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.strip();
	}

}
