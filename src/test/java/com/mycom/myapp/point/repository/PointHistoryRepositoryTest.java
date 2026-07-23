package com.mycom.myapp.point.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.point.entity.PointHistory;
import com.mycom.myapp.point.entity.PointType;
import com.mycom.myapp.user.entity.Role;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

@SpringBootTest							// 전체 애플리케이션 설정 로드해 테스트 환경 구성
@Transactional							// 테스트 실행 후 데이터 롤백해 DB 상태 깨끗하게 유지
class PointHistoryRepositoryTest {
	
	@Autowired
	private PointHistoryRepository pointHistoryRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Test
	@DisplayName("회원의 포인트 이력을 최신순으로 조회 검증")
	void findByUser_UserIdOrderByCreatedAtDesc_Test() {
		// 1. 테스트에 필요한 회원과 이력 데이터 준비
		User user = new User();
		user.setEmail("test@test.com");
		user.setPassword("1234");
		user.setRole(Role.USER);
		userRepository.save(user);
		
		// 포인트 이력 2건 저장  ->  시간 차 부여 시 테스트 성공
		PointHistory h1 = new PointHistory(null, user, null, 1000, PointType.CHARGE, 1000, java.time.LocalDateTime.now().minusSeconds(2));
		PointHistory h2 = new PointHistory(null, user, null, -500, PointType.WITHDRAW, 500, java.time.LocalDateTime.now().minusSeconds(1));
		
		pointHistoryRepository.save(h1);
		pointHistoryRepository.save(h2);
		
		// 2. 특정 회원의 포인트 이력을 최신순으로 조회
		List<PointHistory> result = pointHistoryRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
		
		
		// 3. 결과 검증
		// 이력 데이터 2개인지 확인
		assertEquals(2, result.size(), "조회된 이력의 개수가 맞아야 합니다.");
		// 가장 최근 데이터(h2)가 첫 번째로 오는지 확인
		assertEquals(-500, result.get(0).getAmount(), "첫 번째 항목은 가장 최근인 -500 이어야 합니다.");
		// 그 다음 데이터(h1)가 두 번째로 오는지 확인
		assertEquals(1000, result.get(1).getAmount(), "두 번째 항목은 1000이어야 합니다.");
	}
}
