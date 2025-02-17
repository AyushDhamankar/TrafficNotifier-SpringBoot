package com.backend.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.backend.entity.TrafficEntity;
import com.backend.repository.TrafficRepository;

@Service
public class TrafficServicesImpl implements TrafficServices {

    @Autowired
    private TrafficRepository trafficRepository;

    @Override
    public TrafficEntity addTrafficData(TrafficEntity trafficEntity) {
        return trafficRepository.save(trafficEntity);
    }

    @Override
    public List<TrafficEntity> getAllTrafficData() {
        return trafficRepository.findAll();
    }
}