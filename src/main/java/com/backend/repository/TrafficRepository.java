package com.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.entity.TrafficEntity;

@Repository
public interface TrafficRepository extends JpaRepository<TrafficEntity, Long> {
}