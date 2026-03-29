package com.cityatlas.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cityatlas.backend.entity.analytics.DimCity;

@Repository
public interface DimCityRepository extends JpaRepository<DimCity, Long> {

    List<DimCity> findByIsCurrentTrue();
}
