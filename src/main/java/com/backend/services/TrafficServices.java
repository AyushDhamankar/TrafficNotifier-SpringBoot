package com.backend.services;

import java.util.List;

import com.backend.entity.TrafficEntity;

public interface TrafficServices {
    TrafficEntity addTrafficData(TrafficEntity trafficEntity);
    List<TrafficEntity> getAllTrafficData();
}