package com.mycom.myapp.challenge.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycom.myapp.challenge.service.ChallengeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/challenge")
@RequiredArgsConstructor
public class ChallengeController {

	private final ChallengeService challengeService;
	
	 
}
