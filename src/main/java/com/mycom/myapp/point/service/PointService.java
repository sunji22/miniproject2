package com.mycom.myapp.point.service;

public interface PointService {

	void chargePoint(Long userId, int amount);

	int getPointBalance(Long userId);

	void lockPoint(Long userId, int amount);

	void withdrawPoint(Long userId, int amount);
}
