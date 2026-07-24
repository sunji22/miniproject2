package com.mycom.myapp.challenge.entity;

import java.time.LocalDateTime;

import com.mycom.myapp.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "participation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"challenge_id", "user_id"})
})
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "success_count", nullable = false)
    private int successCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipationStatus status = ParticipationStatus.JOINED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // 객체 생성 팩토리 메서드 -> 캡슐화
    // user 와 challenge 는 영속화 상태
    public static Participation createParticipation(User user, Challenge challenge) {
    	Participation participation = new Participation();
    	
    	participation.setUser(user);
    	participation.setChallenge(challenge);
    	
    	return participation;
    }
    
    // 참여 취소 이후 다시 참여할 때 상태 변경에 사용. 의미를 표현하기 위해 setter 대신 사용.
    public void rejoin() {
    	if (this.status == ParticipationStatus.JOINED) {
            throw new IllegalStateException("이미 참여 중인 상태입니다.");
        }
    	this.status = ParticipationStatus.JOINED;
    }
    
    // 참여 취소하기
    public void cancel() {
    	if (this.status != ParticipationStatus.JOINED) {
            throw new IllegalStateException("참여 중인 챌린지가 아닙니다.");
        }
    	this.status = ParticipationStatus.CANCLED;
    }
}
