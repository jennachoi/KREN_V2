package com.kren.letter;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kren.common.ApiException;
import com.kren.common.ErrorCode;
import com.kren.common.PageResponse;
import com.kren.letter.ConversationRepository.ConversationRow;
import com.kren.letter.dto.ConversationSummary;
import com.kren.letter.dto.ConversationThread;
import com.kren.letter.dto.LetterResponse;
import com.kren.letter.dto.LetterSummary;
import com.kren.user.UserRepository;
import com.kren.user.dto.UserSummary;

/**
 * The mailbox organised by person. Read-only: opening a letter (and so marking it read) still goes
 * through {@link LetterService#read}.
 */
@Service
public class ConversationService {

	private final ConversationRepository conversationRepository;
	private final LetterRepository letterRepository;
	private final LetterService letterService;
	private final UserRepository userRepository;
	private final CountryLocations locations;
	private final Clock clock;

	public ConversationService(ConversationRepository conversationRepository, LetterRepository letterRepository,
			LetterService letterService, UserRepository userRepository, CountryLocations locations, Clock clock) {
		this.conversationRepository = conversationRepository;
		this.letterRepository = letterRepository;
		this.letterService = letterService;
		this.userRepository = userRepository;
		this.locations = locations;
		this.clock = clock;
	}

	/** Two queries per page however many conversations: the grouped rows, then their newest letters. */
	@Transactional(readOnly = true)
	public PageResponse<ConversationSummary> list(long me, int page, int size) {
		Instant now = clock.instant();
		Page<ConversationRow> rows = conversationRepository.findConversations(me, now, PageRequest.of(page, size));
		Map<Long, Letter> lastLetters = letterRepository
			.findByIdIn(rows.getContent().stream().map(ConversationRow::getLastLetterId).toList())
			.stream()
			.collect(Collectors.toMap(Letter::getId, Function.identity()));
		return PageResponse.of(rows, row -> {
			Letter last = lastLetters.get(row.getLastLetterId());
			return new ConversationSummary(UserSummary.from(last.counterpartOf(me)), LetterSummary.from(last, me, now),
					last.getSender().getId() == me, row.getUnread(), row.getAwaiting(), row.getInFlight());
		});
	}

	/**
	 * The newest {@code size} letters with a penpal, or the ones before {@code beforeId}, returned
	 * oldest first so they read top to bottom. Only letters I sent or received can ever match, so
	 * there's nothing to hide; a cursor letter that isn't mine is a 403 like any other letter.
	 */
	@Transactional(readOnly = true)
	public ConversationThread thread(long me, long penpalId, Long beforeId, int size) {
		var penpal = userRepository.findById(penpalId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
		PageRequest oneExtra = PageRequest.of(0, size + 1);
		List<Letter> newestFirst;
		if (beforeId == null) {
			newestFirst = conversationRepository.findThread(me, penpalId, oneExtra);
		}
		else {
			Letter cursor = letterService.loadVisible(me, beforeId);
			newestFirst = conversationRepository.findThreadBefore(me, penpalId, cursor.getSentAt(), cursor.getId(),
					oneExtra);
		}
		boolean hasEarlier = newestFirst.size() > size;
		List<Letter> letters = new ArrayList<>(newestFirst.subList(0, Math.min(size, newestFirst.size())));
		Collections.reverse(letters);

		Set<Long> corrected = letters.isEmpty() ? Set.of()
				: conversationRepository.findCorrectedAmong(letters.stream().map(Letter::getId).toList());
		Instant now = clock.instant();
		return new ConversationThread(UserSummary.from(penpal),
				letters.stream()
					.map(l -> LetterResponse.from(l, me, now, locations, corrected.contains(l.getId())))
					.toList(),
				hasEarlier);
	}

}
