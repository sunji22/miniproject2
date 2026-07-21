package com.mycom.myapp.point.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.common.exception.InsufficientPointException;
import com.mycom.myapp.common.exception.UserNotFoundException;
import com.mycom.myapp.point.entity.PointHistory;
import com.mycom.myapp.point.entity.PointType;
import com.mycom.myapp.point.repository.PointHistoryRepository;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointServiceImpl implements PointService {

	private final PointHistoryRepository pointHistoryRepository;
	private final UserRepository userRepository;		// User 잔액 업데이트 위함

	@Override
	// #1. 포인트 충전 (핵심 로직)
	@Transactional
	public void chargePoint(Long userId, int amount) {
		// 1. 로그 기록 ( Info 레벨 )
		log.info("포인트 충전 시도 : userId = {}, amount = {}", userId, amount);

		// 2. 유저 조회 ( 유저 존재 X 예외 처리 )
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));

		// 3. 유저 잔액 갱신 (주의사항 : User 클래스에 적절한 setter 혹은 로직 메서드 필요)
		user.setPointBalance(user.getPointBalance() + amount);
		userRepository.save(user);

		// 4. 포인트 이력 저장
		PointHistory history = new PointHistory(
				null,
				user,
				null,			// 충전은 참여 내역에 없기 때문에 null
				amount,
				PointType.CHARGE,
				user.getPointBalance(),
				LocalDateTime.now()
		);
		pointHistoryRepository.save(history);

		// 5. 성공 로그
		log.info("포인트 충전 완료 : userId = {}, amount = {}", userId, amount);
	}

	@Override
	// #2. 포인트 조회 처리
	public int getPointBalance(Long userId) {
		// 유저 조회 ( 유저 존재 X 예외 처리 )
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		return user.getPointBalance();
	}

	@Override
	// #3. 포인트 잠금 처리 ( 정산 등을 위해 일정 금액 잠금 )
	@Transactional
	public void lockPoint(Long userId, int amount) {
		// 유저 조회 ( 유저 존재 X 예외 처리 )
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));

		// 포인트 부족 시 예외 처리
		if (user.getPointBalance() < amount) {
			throw new InsufficientPointException(amount, user.getPointBalance());
		}

		user.setPointBalance(user.getPointBalance() - amount);					// 잠금 (차감)

		PointHistory history = new PointHistory(null, user, null, amount, PointType.DEPOSIT_LOCK, user.getPointBalance(), LocalDateTime.now());
		pointHistoryRepository.save(history);
	}

	@Override
	// #4. 포인트 차감 처리 ( 최종적으로 돈이 나감 )
	@Transactional
	public void withdrawPoint(Long userId, int amount) {
		// 유저 조회 ( 유저 존재 X 예외 처리 )
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));

		// 포인트 부족 시 예외 처리
		if (user.getPointBalance() < amount) {
			throw new InsufficientPointException(amount, user.getPointBalance());
		}

		// 차감 로직 구현
		user.setPointBalance(user.getPointBalance() - amount);

		PointHistory history = new PointHistory(null, user, null, amount, PointType.WITHDRAW, user.getPointBalance(), LocalDateTime.now());
		pointHistoryRepository.save(history);
	}
}
