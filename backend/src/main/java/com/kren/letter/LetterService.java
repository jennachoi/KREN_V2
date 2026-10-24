package com.kren.letter;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kren.block.BlockRepository;
import com.kren.common.ApiException;
import com.kren.common.ErrorCode;
import com.kren.common.PageResponse;
import com.kren.letter.dto.LetterResponse;
import com.kren.letter.dto.LetterSummary;
import com.kren.stamp.StampService;
import com.kren.user.User;
import com.kren.user.UserRepository;

@Service
public class LetterService {

	private final LetterRepository letterRepository;
	private final UserRepository userRepository;
	private final BlockRepository blockRepository;
	private final StampService stampService;
	private final ApplicationEventPublisher events;

	public LetterService(LetterRepository letterRepository, UserRepository userRepository,
			BlockRepository blockRepository, StampService stampService, ApplicationEventPublisher events) {
		this.letterRepository = letterRepository;
		this.userRepository = userRepository;
		this.blockRepository = blockRepository;
		this.stampService = stampService;
		this.events = events;
	}

	@Transactional
	public LetterResponse send(long senderId, long recipientId, String body) {
		return deliver(senderId, recipientId, null, body);
	}

	/** Either participant may reply; the reply goes to the other one. */
	@Transactional
	public LetterResponse reply(long senderId, long parentId, String body) {
		Letter parent = loadVisible(senderId, parentId);
		return deliver(senderId, parent.counterpartOf(senderId).getId(), parent, body);
	}

	/**
	 * One transaction: deduct a stamp, insert the letter, write the ledger row. Any failure (no stamps,
	 * bad recipient, DB error) rolls back all three, so a stamp is never spent without a letter or
	 * vice versa. The recipient's live notification fires only once this commits.
	 */
	private LetterResponse deliver(long senderId, long recipientId, Letter parent, String body) {
		if (senderId == recipientId) {
			throw new ApiException(ErrorCode.CANNOT_SEND_TO_SELF);
		}
		if (!userRepository.existsById(recipientId) || blockRepository.existsBetween(senderId, recipientId)) {
			throw new ApiException(ErrorCode.USER_NOT_FOUND);
		}
		// Deduct first: it takes the row lock on the sender, so concurrent sends queue up here.
		stampService.spendForLetter(senderId);

		// Loaded after the deduction (which clears the persistence context) so nothing is stale.
		User sender = userRepository.getReferenceById(senderId);
		User recipient = userRepository.getReferenceById(recipientId);
		Letter letter = letterRepository.save(new Letter(sender, recipient, parent, body.strip()));
		stampService.recordLetterSpend(senderId, letter.getId());
		events.publishEvent(new LetterSentEvent(recipientId, LetterSummary.from(letter, recipientId)));
		return LetterResponse.from(letter);
	}

	/** Opening a letter you received marks it read (the read receipt the sender sees). */
	@Transactional
	public LetterResponse read(long viewerId, long letterId) {
		Letter letter = loadVisible(viewerId, letterId);
		letter.markReadBy(viewerId, Instant.now());
		return LetterResponse.from(letter);
	}

	@Transactional(readOnly = true)
	public PageResponse<LetterSummary> inbox(long userId, int page, int size) {
		return PageResponse.of(letterRepository.findByRecipient_IdOrderBySentAtDescIdDesc(userId,
				PageRequest.of(page, size)), l -> LetterSummary.from(l, userId));
	}

	@Transactional(readOnly = true)
	public PageResponse<LetterSummary> sent(long userId, int page, int size) {
		return PageResponse.of(letterRepository.findBySender_IdOrderBySentAtDescIdDesc(userId,
				PageRequest.of(page, size)), l -> LetterSummary.from(l, userId));
	}

	/** 404 if it doesn't exist, 403 if it exists but isn't yours. */
	private Letter loadVisible(long userId, long letterId) {
		Letter letter = letterRepository.findById(letterId)
			.orElseThrow(() -> new ApiException(ErrorCode.LETTER_NOT_FOUND));
		if (!letter.isParticipant(userId)) {
			throw new ApiException(ErrorCode.FORBIDDEN);
		}
		return letter;
	}

}
