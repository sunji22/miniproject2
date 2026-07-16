package com.mycom.myapp.challenge.service;

import org.springframework.stereotype.Service;

import com.mycom.myapp.challenge.repository.ChallengeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChallengeImpl implements ChallengeService {

	private final ChallengeRepository challengeRepository;
	
	
}
