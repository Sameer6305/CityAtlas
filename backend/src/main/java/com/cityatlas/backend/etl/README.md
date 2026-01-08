# ETL Package - CityAtlas Analytics Pipeline

This package implements the data pipeline that transforms raw operational data into analytics-ready formats for the star schema.

## Architecture Overview

CityAtlas uses a **Lambda-inspired dual-path architecture** with clear separation between streaming and batch processing:

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                  CityAtlas ETL Architecture              │
                    └─────────────────────────────────────────────────────────┘

     ┌───────────────────────────────────────────────────────────────────────────┐
     │                         STREAMING PATH (Hot)                              │
     │                                                                           │
     │    ┌─────────┐      ┌─────────┐      ┌─────────────────┐      ┌─────────┐ │
     │    │ User    │ ──── │  Kafka  │ ──── │ EtlKafkaListener│ ──── │analytics│ │
     │    │ Action  │      │ Topics  │      │ (micro-batch)   │      │_events  │ │
     │    └─────────┘      └─────────┘      └─────────────────┘      └─────────┘ │
     │                                                                           │
     │    Latency: SECONDS       Trigger: EVENT-DRIVEN       Output: RAW EVENTS  │
     └───────────────────────────────────────────────────────────────────────────┘

     ┌───────────────────────────────────────────────────────────────────────────┐
     │                           BATCH PATH (Cold)                               │
     │                                                                           │
     │    ┌─────────┐      ┌─────────┐      ┌─────────────────┐      ┌─────────┐ │
     │    │  OLTP   │ ──── │  ETL    │ ──── │   Transform     │ ──── │  Star   │ │
     │    │ Tables  │      │Scheduler│      │   Services      │      │ Schema  │ │
     │    └─────────┘      └─────────┘      └─────────────────┘      └─────────┘ │
     │                                                                           │
     │    Latency: MINUTES/HOURS  Trigger: CRON SCHEDULE     Output: AGGREGATES  │
     └───────────────────────────────────────────────────────────────────────────┘
```

---

## Streaming Path

### Purpose
Capture user interactions with **minimal latency** for real-time analytics dashboards.

### Implementation
- **Class:** `EtlKafkaListener.java`
- **Trigger:** Kafka consumer (event-driven)
- **Strategy:** Micro-batching (100 events OR 10 seconds)
- **Output:** `analytics_events` table

### Topics Consumed
| Topic | Event Type | Description |
|-------|------------|-------------|
| `cityatlas.analytics.city-searched` | CITY_SEARCHED | User searched for a city |
| `cityatlas.analytics.section-viewed` | SECTION_VIEWED | User viewed a section |
| `cityatlas.analytics.time-spent` | TIME_SPENT | Duration tracking |

### Use Cases
- Live user counters ("X users online")
- Real-time search trending
- Session tracking
- Immediate event logging

### Configuration
```properties
# application.properties
kafka.enabled=true
kafka.bootstrap-servers=localhost:9092
kafka.consumer.group-id=cityatlas-etl-consumer
```

---

## Batch Path

### Purpose
Transform raw data into **pre-aggregated analytics** for historical analysis and fast querying.

### Implementation
- **Class:** `EtlScheduler.java`
- **Trigger:** Spring `@Scheduled` (cron expressions)
- **Strategy:** Time-window based extraction → Transform → Load
- **Output:** Star schema tables (`dim_*`, `fact_*`)

### Scheduled Jobs
| Job | Schedule | Target Tables |
|-----|----------|---------------|
| Dimension Refresh | Daily 2:00 AM | `dim_city` |
| Metrics Snapshot | Hourly :00 | `fact_city_metrics` |
| Events Aggregation | Every 15 min | `fact_user_events_daily` |
| Full Rebuild | Weekly Sun 3AM | All tables |

### Use Cases
- Historical trend analysis
- Cross-city comparisons
- Daily/weekly/monthly reports
- Complex aggregations and joins

### Configuration
```properties
# application.properties
etl.batch.enabled=true
etl.batch.dimension-refresh-cron=0 0 2 * * *
etl.batch.metrics-snapshot-cron=0 0 * * * *
etl.batch.events-aggregation-cron=0 */15 * * * *
```

---

## When to Use Each Path

| Requirement | Use Streaming Path | Use Batch Path |
|-------------|-------------------|----------------|
| Latency requirement | < 30 seconds | Minutes to hours acceptable |
| Data volume | Individual events | Large datasets |
| Query pattern | Live counters, current state | Historical analysis |
| Transformation | Minimal (validation only) | Complex (joins, aggregations) |
| Example | "Show online users" | "Monthly trend report" |

---

## Package Structure

```
etl/
├── package-info.java              # Architecture documentation
├── README.md                      # This file
│
├── STREAMING PATH:
│   └── EtlKafkaListener.java      # Kafka consumer with micro-batching
│
├── BATCH PATH:
│   └── EtlScheduler.java          # Spring @Scheduled orchestrator
│
└── SHARED SERVICES:
    ├── DataCleaningService.java       # Validation, outlier detection
    ├── MetricNormalizationService.java # Scaling, percentiles
    ├── DataAggregationService.java    # Grouping, rollups
    └── DimensionLoaderService.java    # SCD Type 2 handling
```

---

## Transformation Pipeline

Both paths share the same transformation services:

```
  ┌──────────┐    ┌──────────────────┐    ┌─────────────────────┐    ┌──────────┐
  │ EXTRACT  │ ── │      CLEAN       │ ── │     TRANSFORM       │ ── │   LOAD   │
  └──────────┘    └──────────────────┘    └─────────────────────┘    └──────────┘
       │                  │                        │                       │
       v                  v                        v                       v
  Read source       DataCleaningService     MetricNormalizationService  Write to
  tables with      - Filter null/invalid   - Min-max scaling (0-100)   target
  time window      - Detect outliers       - Percentile ranking        tables
                   - Deduplicate           - Compute derived fields
                                           
                                           DataAggregationService
                                           - Group by grain
                                           - Compute aggregates
                                           - Apply window functions
```

---

## Output Tables

### Streaming Path Output
**`analytics_events`** - Raw event storage
- Minimal transformation
- Sub-second writes
- Source for batch aggregation

### Batch Path Output
**`dim_city`** - City dimension
- SCD Type 2 (slowly changing)
- Updated daily

**`fact_city_metrics`** - Daily city metrics
- GDP, population, AQI, cost of living
- Normalized 0-100 scores
- Percentile rankings

**`fact_user_events_daily`** - Daily user events
- Event counts by city/type/date
- Unique users and sessions
- Duration aggregates

---

## Monitoring

Both paths emit structured logs with prefixes:

```
STREAMING: [ETL-KAFKA] Received CITY_SEARCHED event: city=new-york
BATCH:     [ETL-SCHED] Starting daily dimension refresh job
```

### Key Metrics
| Path | Metrics |
|------|---------|
| Streaming | Events/second, flush frequency, accumulator size |
| Batch | Job duration, records processed, error rate |

---

## Error Handling

### Streaming Path
- Invalid events logged and skipped
- Accumulator restored on flush failure
- Dead letter queue for repeated failures (TODO)

### Batch Path
- Transactional writes with rollback
- Idempotent operations (safe to retry)
- Batch lineage tracking via `etl_batch_id`
