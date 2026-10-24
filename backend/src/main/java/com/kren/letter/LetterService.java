package com.kren.letter;

import java.time.Clock;
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
	private final CountryLocations locations;
	private final DeliveryPolicy deliveryPolicy;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	public LetterService(LetterRepository letterRepository, UserRepository userRepository,
			BlockRepository blockRepository, StampService stampService, CountryLocations locations,
			DeliveryPolicy deliveryPolicy, ApplicationEventPublisher events, Clock clock) {
		this.letterRepository = letterRepository;
		this.userRepository = userRepository;
		this.blockRepository = blockRepository;
		this.stampService = stampService;
		this.locations = locations;
		this.deliveryPolicy = deliveryPolicy;
		this.events = events;
		this.clock = clock;
	}

	@Transactional
	public LetterResponse send(long senderId, long recipientId, String body) {
		return deliver(senderId, recipientId, null, body);
	}

	/** Either participant may reply; the reply goes to the other one. You can't answer what hasn't arrived. */
	@Transactional
	public LetterResponse reply(long senderId, long parentId, String body) {
		Letter parent = loadVisible(senderId, parentId);
		if (parent.isSealedFor(senderId, clock.instant())) {
			throw new ApiException(ErrorCode.LETTER_IN_TRANSIT);
		}
		return deliver(senderId, parent.counterpartOf(senderId).getId(), parent, body);
	}

	/**
	 * One transaction: deduct a stamp, insert the letter, write the ledger row. Any failure (no stamps,
	 * bad recipient, DB error) rolls back all three, so a stamp is never spent without a letter or
	 * vice versa. The letter then flies for a distance-based time; the recipient is told it's on its
	 * way once this commits, and that it has arrived when the dispatcher lands it.
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
		Instant now = clock.instant();
		Integer distanceKm = locations.distanceKm(sender.getCountry(), recipient.getCountry()).orElse(null);
		Letter letter = letterRepository.save(new Letter(sender, recipient, parent, body.strip(), now, distanceKm,
				now.plus(deliveryPolicy.travelTime(distanceKm))));
		stampService.recordLetterSpend(senderId, letter.getId());
		events.publishEvent(new LetterSentEvent(recipientId, LetterSummary.from(letter, recipientId, now)));
		return LetterResponse.from(letter, senderId, now, locations);
	}

	/**
	 * Opening a delivered letter you received marks it read (the sender's read receipt). Opening one
	 * still in flight shows the tracker data with the body withheld.
	 */
	@Transactional
	public LetterResponse read(long viewerId, long letterId) {
		Letter letter = loadVisible(viewerId, letterId);
		Instant now = clock.instant();
		letter.markReadBy(viewerId, now);
		return LetterResponse.from(letter, viewerId, now, locations);
	}

	@Transactional(readOnly = true)
	public PageResponse<LetterSummary> inbox(long userId, int page, int size) {
		Instant now = clock.instant();
		return PageResponse.of(letterRepository.findByRecipient_IdOrderBySentAtDescIdDesc(userId,
				PageRequest.of(page, size)), l -> LetterSummary.from(l, userId, now));
	}

	@Transactional(readOnly = true)
	public PageResponse<LetterSummary> sent(long userId, int page, int size) {
		Instant now = clock.instant();
		return PageResponse.of(letterRepository.findBySender_IdOrderBySentAtDescIdDesc(userId,
				PageRequest.of(page, size)), l -> LetterSummary.from(l, userId, now));
	}

	/** 404 if it doesn't exist, 403 if it exists but isn't yours. */
	Letter loadVisible(long userId, long letterId) {
		Letter letter = letterRepository.findById(letterId)
			.orElseThrow(() -> new ApiException(ErrorCode.LETTER_NOT_FOUND));
		if (!letter.isParticipant(userId)) {
			throw new ApiException(ErrorCode.FORBIDDEN);
		}
		return letter;
	}

}
