package com.kren.correction;

import com.kren.correction.dto.CorrectionResponse;

/** The writer of a letter just got it back corrected. Delivered to listeners after commit. */
public record CorrectionReceivedEvent(long writerId, CorrectionResponse correction) {
}
