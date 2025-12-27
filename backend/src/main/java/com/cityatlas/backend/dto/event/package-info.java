/**
 * Event Data Transfer Objects
 * 
 * DTOs specifically designed for event-driven architecture (Kafka, message queues).
 * These objects are serialized and transmitted between services or microservices.
 * 
 * Key Characteristics:
 * - Immutable when possible (use @Builder for construction)
 * - JSON serializable (Jackson annotations)
 * - Self-contained (no database entity references)
 * - Validation methods included for event integrity
 * 
 * Event Types:
 * - Analytics Events: User behavior tracking (searches, views, engagement)
 * - Data Events: System data updates and synchronization
 * - Command Events: Action requests between services
 * 
 * Usage:
 * <pre>
 * // Create and publish analytics event
 * AnalyticsEventPayload event = AnalyticsEventPayload.builder()
 *     .eventType("SECTION_VIEWED")
 *     .citySlug("san-francisco")
 *     .section("economy")
 *     .timestamp(LocalDateTime.now())
 *     .build();
 * 
 * if (event.isValid()) {
 *     kafkaProducer.send(KafkaTopics.SECTION_VIEWED, event);
 * }
 * </pre>
 * 
 * @see com.cityatlas.backend.config.KafkaTopics
 * @see com.cityatlas.backend.dto.event.AnalyticsEventPayload
 */
package com.cityatlas.backend.dto.event;
