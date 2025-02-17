package com.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.entity.TrafficEntity;
import com.backend.services.TrafficServices;

@CrossOrigin("http://localhost:5173/")
@RestController
@RequestMapping("/traffic")
public class TrafficController {

    @Autowired
    private TrafficServices trafficServices;

    // Add a new Traffic Entity
    @PostMapping("/add")
    public ResponseEntity<TrafficEntity> addTrafficData(@RequestBody TrafficEntity trafficEntity) {
        TrafficEntity savedEntity = trafficServices.addTrafficData(trafficEntity);
        return ResponseEntity.ok(savedEntity);
    }

    // Get all Traffic Entities
    @GetMapping("/all")
    public ResponseEntity<List<TrafficEntity>> getAllTrafficData() {
        return ResponseEntity.ok(trafficServices.getAllTrafficData());
    }
}