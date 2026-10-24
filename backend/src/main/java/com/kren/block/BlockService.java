package com.kren.block;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kren.block.dto.BlockedUserResponse;
import com.kren.common.ApiException;
import com.kren.common.ErrorCode;
import com.kren.common.PageResponse;
import com.kren.user.UserRepository;

/**
 * Who sees what once a block exists (in either direction): profile and penpal search hide the other
 * person, and neither can send the other new letters. Past letters stay in both mailboxes.
 */
@Service
public class BlockService {

	private final BlockRepository blockRepository;
	private final UserRepository userRepository;

	public BlockService(BlockRepository blockRepository, UserRepository userRepository) {
		this.blockRepository = blockRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public void block(long blockerId, long blockedId) {
		if (blockerId == blockedId) {
			throw new ApiException(ErrorCode.CANNOT_BLOCK_SELF);
		}
		if (!userRepository.existsById(blockedId)) {
			throw new ApiException(ErrorCode.USER_NOT_FOUND);
		}
		blockRepository.insertIfAbsent(blockerId, blockedId);
	}

	/** Idempotent like {@link #block}: unblocking someone you never blocked is fine. */
	@Transactional
	public void unblock(long blockerId, long blockedId) {
		blockRepository.deleteByPair(blockerId, blockedId);
	}

	@Transactional(readOnly = true)
	public PageResponse<BlockedUserResponse> list(long blockerId, int page, int size) {
		return PageResponse.of(blockRepository.findByBlocker(blockerId, PageRequest.of(page, size)),
				BlockedUserResponse::from);
	}

}
