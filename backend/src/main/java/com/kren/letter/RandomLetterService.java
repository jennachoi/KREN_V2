package com.kren.letter;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kren.common.ApiException;
import com.kren.common.ErrorCode;
import com.kren.letter.dto.LetterResponse;
import com.kren.letter.dto.RandomQuotaResponse;
import com.kren.user.User;
import com.kren.user.UserRepository;

/**
 * "Send to a random penpal": the letter goes to someone whose languages mirror yours, who you've
 * never written to or heard from, and who isn't on either side of a block. A few per day.
 */
@Service
public class RandomLetterService {

	private final RandomLetterQuota quota;
	private final UserRepository userRepository;
	private final LetterService letterService;
	private final Clock clock;
	private final int dailyLimit;

	public RandomLetterService(RandomLetterQuota quota, UserRepository userRepository, LetterService letterService,
			Clock clock, @Value("${kren.random-letters.daily-limit:3}") int dailyLimit) {
		this.quota = quota;
		this.userRepository = userRepository;
		this.letterService = letterService;
		this.clock = clock;
		this.dailyLimit = dailyLimit;
	}

	/**
	 * One transaction: claim a daily slot, pick the recipient, send (which spends a stamp). If there's
	 * nobody new to write to, or no stamp, everything rolls back and the slot isn't used up.
	 */
	@Transactional
	public LetterResponse send(long senderId, String body) {
		if (!quota.tryConsume(senderId, today(), dailyLimit)) {
			throw new ApiException(ErrorCode.RANDOM_LIMIT_REACHED);
		}
		User sender = userRepository.findById(senderId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
		long recipientId = userRepository
			.findRandomNewPenpal(senderId, sender.getNativeLang(), sender.getLearningLang())
			.orElseThrow(() -> new ApiException(ErrorCode.NO_RANDOM_MATCH));
		return letterService.send(senderId, recipientId, body);
	}

	@Transactional(readOnly = true)
	public RandomQuotaResponse quota(long userId) {
		int used = quota.usedOn(userId, today());
		return new RandomQuotaResponse(dailyLimit, used, Math.max(0, dailyLimit - used));
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}

}
