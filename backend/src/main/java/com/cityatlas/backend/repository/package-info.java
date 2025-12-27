/**
 * Data Access Layer - Repository Interfaces
 * 
 * Responsibilities:
 * - Database operations (CRUD)
 * - Custom queries (JPQL, native SQL)
 * - Data persistence abstraction
 * 
 * Naming Convention: {Entity}Repository.java
 * Example: CityRepository.java, AnalyticsRepository.java
 * 
 * Extends JpaRepository<Entity, ID>
 * @Repository annotation (optional with Spring Data JPA)
 */
package com.cityatlas.backend.repository;
