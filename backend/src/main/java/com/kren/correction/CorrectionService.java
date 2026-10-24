package com.kren.correction;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kren.common.ApiException;
import com.kren.common.ErrorCode;
import com.kren.common.PageResponse;
import com.kren.correction.dto.CorrectionRequest;
import com.kren.correction.dto.CorrectionResponse;
import com.kren.letter.Letter;
import com.kren.letter.LetterService;
import com.kren.stamp.StampService;
import com.kren.user.UserRepository;

@Service
public class CorrectionService {

	private final CorrectionRepository correctionRepository;
	private final LetterService letterService;
	private final UserRepository userRepository;
	private final StampService stampService;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	public CorrectionService(CorrectionRepository correctionRepository, LetterService letterService,
			UserRepository userRepository, StampService stampService, ApplicationEventPublisher events, Clock clock) {
		this.correctionRepository = correctionRepository;
		this.letterService = letterService;
		this.userRepository = userRepository;
		this.stampService = stampService;
		this.events = events;
		this.clock = clock;
	}

	/**
	 * The recipient of a delivered letter corrects it once and earns a stamp. Saving the correction and
	 * paying the reward share one transaction; the one-per-letter unique constraint decides any race,
	 * so a double submit can't pay twice.
	 */
	@Transactional
	public CorrectionResponse correct(long correctorId, long letterId, CorrectionRequest req) {
		Letter letter = letterService.loadVisible(correctorId, letterId);
		if (!letter.isRecipient(correctorId)) {
			throw new ApiException(ErrorCode.FORBIDDEN, "Only the recipient can correct a letter");
		}
		Instant now = clock.instant();
		if (!letter.isDeliveredAt(now)) {
			throw new ApiException(ErrorCode.LETTER_IN_TRANSIT);
		}
		List<CorrectionLine> lines = req.lines().stream()
			.map(l -> new CorrectionLine(l.original().strip(), l.corrected().strip(), blankToNull(l.note())))
			.toList();
		for (CorrectionLine line : lines) {
			if (line.original().equals(line.corrected()) || !letter.getBody().contains(line.original())) {
				throw new ApiException(ErrorCode.INVALID_CORRECTION);
			}
		}
		if (correctionRepository.findByLetter_Id(letterId).isPresent()) {
			throw new ApiException(ErrorCode.ALREADY_CORRECTED);
		}

		Correction correction;
		try {
			correction = correctionRepository.saveAndFlush(new Correction(letter,
					userRepository.getReferenceById(correctorId), blankToNull(req.comment()), lines, now));
		}
		catch (DataIntegrityViolationException e) {
			// Lost a race with an identical submit: uq_corrections_letter.
			throw new ApiException(ErrorCode.ALREADY_CORRECTED);
		}
		// Build the response before paying out: the stamp UPDATE clears the persistence context, which
		// would detach the lazily-loaded corrector.
		CorrectionResponse response = CorrectionResponse.from(correction);
		stampService.rewardCorrection(correctorId, letterId);
		events.publishEvent(new CorrectionReceivedEvent(letter.getSender().getId(), response));
		return response;
	}

	@Transactional(readOnly = true)
	public CorrectionResponse get(long viewerId, long letterId) {
		letterService.loadVisible(viewerId, letterId);
		return correctionRepository.findByLetter_Id(letterId)
			.map(CorrectionResponse::from)
			.orElseThrow(() -> new ApiException(ErrorCode.CORRECTION_NOT_FOUND));
	}

	/** Every correction others made to my letters: my personal list of "what I got wrong". */
	@Transactional(readOnly = true)
	public PageResponse<CorrectionResponse> notebook(long writerId, int page, int size) {
		return PageResponse.of(correctionRepository.findNotebook(writerId, PageRequest.of(page, size)),
				CorrectionResponse::from);
	}

	private static String blankToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.strip();
	}

}
