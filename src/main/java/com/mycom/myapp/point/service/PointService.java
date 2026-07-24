package com.mycom.myapp.point.service;

import com.mycom.myapp.challenge.entity.Participation;

public interface PointService {

	void chargePoint(Long userId, int amount);

	int getPointBalance(Long userId);

	void lockPoint(Long userId, Participation participation, int amount);

	void withdrawPoint(Long userId, int amount);
}
