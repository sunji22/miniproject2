package com.mycom.myapp.point.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.domain.SettlementStatus;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.point.service.SettlementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

	private final SettlementService settlementService;
	private final ChallengeRepository challengeRepository;
	
	// 매일 자정마다 7일 동안 정산이 되지 않은 챌린지가 있는 지 확인 (매일 자정마다 실행)
	@Scheduled(cron = "0 0 0 * * *")
	public void autoSettle() {
		// 종료일 이후 7일이 지났는지 확인
		LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
		
		// 정산 대상 챌린지 조회 (종료일 지난 + 진행 중 + 정산 대기 / 일단 정산이 되어야지만 챌린지 상태가 CLOSED 된 상태로 생각하고 진행했습니다.)
		List<Challenge> challenges = challengeRepository.findByEndDateBeforeAndStatusAndSettlementStatus(
						sevenDaysAgo, 
						ChallengeStatus.ONGOING, 
						SettlementStatus.PENDING
		);
		
		// 각 챌린지 자동 정산 처리
		for (Challenge challenge : challenges) {
			try {
				settlementService.settleChallenge(challenge.getId(), challenge.getHost().getUserId());
			} catch (Exception e) {
				log.error("챌린지 {} 자동 정산 실패", challenge.getId(), e);
			}
		}
	}
}
