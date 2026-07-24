package com.mycom.myapp.challenge.scheduler;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.repository.ChallengeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChallengeStatusScheduler {

    private final ChallengeRepository challengeRepository;

    // 매일 자정(00:00:00)마다 실행
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void updateChallengeStatuses() {
        LocalDate now = LocalDate.now();

        // 1. 시작 시간이 지난 UPCOMING -> ONGOING 변경
        challengeRepository.updateStatusToOngoing(now);

        // 2. 종료 시간이 지난 ONGOING -> CLOSED 변경
        challengeRepository.updateStatusToClosed(now);
    }
}