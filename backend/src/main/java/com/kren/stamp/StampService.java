package com.kren.stamp;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kren.common.ApiException;
import com.kren.common.ErrorCode;
import com.kren.common.PageResponse;
import com.kren.stamp.dto.CheckinResponse;
import com.kren.stamp.dto.StampLedgerEntryResponse;
import com.kren.user.UserRepository;

/**
 * Every balance change is a pair: one conditional/atomic UPDATE on {@code users.stamp_balance} plus
 * one {@code stamp_ledger} row, always inside the caller's transaction.
 */
@Service
public class StampService {

	public static final int SIGNUP_BONUS = 5;
	public static final int CHECKIN_REWARD = 1;
	public static final int LETTER_COST = 1;
	public static final int CORRECTION_REWARD = 1;

	private final UserRepository userRepository;
	private final StampLedgerRepository ledgerRepository;
	private final Clock clock;

	public StampService(UserRepository userRepository, StampLedgerRepository ledgerRepository, Clock clock) {
		this.userRepository = userRepository;
		this.ledgerRepository = ledgerRepository;
		this.clock = clock;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void grantSignupBonus(long userId) {
		userRepository.addStamps(userId, SIGNUP_BONUS);
		ledgerRepository.save(new StampLedgerEntry(userId, SIGNUP_BONUS, StampReason.SIGNUP, null));
	}

	/**
	 * Must run inside the transaction that inserts the letter: if the letter insert fails, the
	 * deduction rolls back with it. Throws (and so rolls everything back) when the balance is 0.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void spendForLetter(long userId) {
		if (userRepository.deductOneStamp(userId) == 0) {
			throw new ApiException(ErrorCode.INSUFFICIENT_STAMPS);
		}
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void recordLetterSpend(long userId, long letterId) {
		ledgerRepository.save(new StampLedgerEntry(userId, -LETTER_COST, StampReason.SEND_LETTER, letterId));
	}

	/**
	 * "Correct to earn": paid in the same transaction that saves the correction, whose one-per-letter
	 * unique constraint guarantees this runs at most once per letter.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void rewardCorrection(long userId, long letterId) {
		userRepository.addStamps(userId, CORRECTION_REWARD);
		ledgerRepository.save(new StampLedgerEntry(userId, CORRECTION_REWARD, StampReason.CORRECTION, letterId));
	}

	@Transactional
	public CheckinResponse checkIn(long userId) {
		LocalDate today = LocalDate.now(clock);
		if (ledgerRepository.insertCheckin(userId, today) == 0) {
			throw new ApiException(ErrorCode.ALREADY_CHECKED_IN);
		}
		userRepository.addStamps(userId, CHECKIN_REWARD);
		ledgerRepository.save(new StampLedgerEntry(userId, CHECKIN_REWARD, StampReason.CHECKIN, null));
		int balance = userRepository.findById(userId).orElseThrow().getStampBalance();
		return new CheckinResponse(today, CHECKIN_REWARD, balance);
	}

	@Transactional(readOnly = true)
	public PageResponse<StampLedgerEntryResponse> ledger(long userId, int page, int size) {
		return PageResponse.of(ledgerRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(page, size)),
				StampLedgerEntryResponse::from);
	}

}
