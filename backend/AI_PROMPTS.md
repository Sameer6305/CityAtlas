# AI Prompt Engineering Documentation

## 📋 Overview

This document describes the **production-ready AI prompt templates** used in CityAtlas for generating city summaries. The system follows a **structured template approach** ensuring deterministic, safe, and auditable AI outputs.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          PROMPT ENGINEERING PIPELINE                             │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ City Entity  │───▶│PromptBuilder │───▶│FeatureInput  │───▶│PromptTemplate│
│   (raw data) │    │  (transform) │    │ (structured) │    │   (render)   │
└──────────────┘    └──────────────┘    └──────────────┘    └──────┬───────┘
                                                                    │
                                                                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ AI Summary   │◀───│ Rule-based   │◀───│ Constraints  │◀───│ Rendered     │
│   Output     │    │  Generator   │    │  Validation  │    │   Prompt     │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

---

## 📝 Template Structure

Every prompt follows a **fixed 4-section structure**:

### Section 1: CONTEXT
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ • City identification (name, country, population)                                │
│ • Task description (what AI should generate)                                     │
│ • Role definition (perspective to take)                                          │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Section 2: FEATURE SUMMARY
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ • Economy score, tier, components                                                │
│ • Livability score, tier, components                                             │
│ • Sustainability score, tier, components                                         │
│ • Growth score, tier, components                                                 │
│ • Overall assessment                                                             │
│ • Data quality metrics                                                           │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Section 3: CONSTRAINTS
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ • Determinism rules (same input → same output)                                   │
│ • Safety rules (forbidden topics)                                                │
│ • Output boundaries (min/max items)                                              │
│ • Attribution requirements                                                       │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Section 4: OUTPUT FORMAT
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ • Expected JSON/text structure                                                   │
│ • Field descriptions                                                             │
│ • Example output                                                                 │
│ • Validation rules                                                               │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📚 Available Templates

### 1. CITY_PERSONALITY (ID: `CITY_PERSONALITY_001`)

**Purpose**: Generate 2-4 sentence personality description

**Input Requirements**:
| Field | Type | Required |
|-------|------|----------|
| cityName | String | ✅ |
| country | String | ✅ |
| population | Long | ⚠️ |
| economyScore | Double (0-100) | ⚠️ |
| livabilityScore | Double (0-100) | ⚠️ |
| sustainabilityScore | Double (0-100) | ⚠️ |
| growthScore | Double (0-100) | ⚠️ |

**Output Schema**:
```text
"[City] is a [primary characteristic based on highest score]. 
[1-2 supporting characteristics]. 
[Trade-offs or considerations if any low scores]."
```

**Example Output**:
```text
"San Francisco is a thriving economic hub with exceptional job opportunities 
in the tech sector. The city offers excellent livability with diverse cultural 
attractions, though its high cost of living presents challenges for newcomers. 
Environmental conditions are generally good with moderate air quality."
```

---

### 2. CITY_STRENGTHS (ID: `CITY_STRENGTHS_001`)

**Purpose**: Generate 2-6 strengths for scores ≥ 60

**Inclusion Rules**:
| Score Range | Tier | Include Strength? |
|-------------|------|-------------------|
| 80-100 | Excellent | ✅ Yes (prioritize) |
| 60-79 | Good | ✅ Yes |
| 40-59 | Average | ❌ No |
| 20-39 | Below Average | ❌ No |
| 0-19 | Poor | ❌ No |

**Output Schema**:
```json
[
  "Strong economy with diverse job market (score: 78/100)",
  "Excellent environmental quality (score: 85/100)",
  "Good livability with cultural amenities (score: 65/100)"
]
```

---

### 3. CITY_WEAKNESSES (ID: `CITY_WEAKNESSES_001`)

**Purpose**: Generate 1-5 challenges for scores < 40

**Inclusion Rules**:
| Score Range | Tier | Include Weakness? |
|-------------|------|-------------------|
| 80-100 | Excellent | ❌ No |
| 60-79 | Good | ❌ No |
| 40-59 | Average | ❌ No |
| 20-39 | Below Average | ✅ Yes |
| 0-19 | Poor | ✅ Yes (prioritize) |

**Output Schema**:
```json
[
  "Economic challenges with limited job opportunities (score: 35/100)",
  "Environmental concerns with air quality issues (score: 28/100)"
]
```

**Framing Guidelines**:
- ✅ Use: "challenges", "areas for improvement", "considerations"
- ❌ Avoid: "terrible", "awful", "worst", "failing"

---

### 4. CITY_AUDIENCE (ID: `CITY_AUDIENCE_001`)

**Purpose**: Generate 2-6 audience segments best suited for the city

**Mapping Rules**:
| Condition | Audience Segment |
|-----------|------------------|
| Economy ≥ 60 | Career-focused professionals |
| Economy ≥ 60 AND Cost < 100 | Young professionals seeking affordability |
| Livability ≥ 60 | Families seeking quality of life |
| Sustainability ≥ 70 | Environmentally conscious residents |
| Growth ≥ 60 | Entrepreneurs and startup founders |
| Economy ≥ 60 AND Livability ≥ 60 | Remote workers seeking balance |
| Cost of Living < 80 | Retirees on fixed income |

**Output Schema**:
```json
[
  "Career-focused professionals seeking strong job markets",
  "Families looking for good quality of life and amenities",
  "Environmentally conscious individuals valuing clean air"
]
```

---

### 5. CITY_COMPLETE_SUMMARY (ID: `CITY_COMPLETE_SUMMARY_001`)

**Purpose**: Generate complete AI summary with all elements

**Output Schema**:
```json
{
  "personality": "2-4 sentence description...",
  "strengths": ["...", "..."],
  "weaknesses": ["..."],
  "bestSuitedFor": ["...", "..."],
  "dataConfidence": 0.85,
  "generatedAt": "2024-01-15T10:30:00Z",
  "templateVersion": "1.0.0"
}
```

---

## 🔒 Constraint Definitions

### Determinism Rules

```java
public record DeterminismRules(
    boolean useFixedSeed,      // true
    int randomSeed,            // 42
    boolean useTemperatureZero,// true (no randomness)
    boolean disableSampling,   // true
    String hashAlgorithm       // "SHA-256"
) {}
```

**Guarantee**: Same CityFeatureInput → Byte-identical output

---

### Safety Rules

```java
public static final Set<String> FORBIDDEN_TOPICS = Set.of(
    "political opinions",
    "religious views",
    "racial stereotypes",
    "gender discrimination",
    "income discrimination",
    "crime rate assumptions",
    "neighborhood profiling",
    "speculation without data",
    "future predictions beyond data",
    "comparative rankings without basis"
);
```

---

### Output Boundaries

```java
public record OutputBoundaries(
    int maxPersonalityLength,  // 500 chars
    int minStrengths,          // 2
    int maxStrengths,          // 6
    int minWeaknesses,         // 1
    int maxWeaknesses,         // 5
    int minAudienceItems,      // 2
    int maxAudienceItems,      // 6
    int maxSentenceLength,     // 30 words
    int maxTotalTokens         // 2048
) {}
```

---

### Score Thresholds

```java
public record ScoreThresholds(
    int excellentThreshold,     // 80
    int goodThreshold,          // 60
    int averageThreshold,       // 40
    int belowAverageThreshold   // 20
) {}
```

**Tier Classification**:
| Score | Tier |
|-------|------|
| ≥ 80 | Excellent |
| ≥ 60 | Good |
| ≥ 40 | Average |
| ≥ 20 | Below Average |
| < 20 | Poor |

---

## 📥 Structured Input Schema

### CityFeatureInput

```java
@Data
@Builder
public class CityFeatureInput {
    private CityIdentifier cityIdentifier;
    private EconomyFeatures economyFeatures;
    private LivabilityFeatures livabilityFeatures;
    private SustainabilityFeatures sustainabilityFeatures;
    private GrowthFeatures growthFeatures;
    private OverallAssessment overallAssessment;
    private DataQualityMetadata dataQuality;
}
```

### Nested Structures

```java
// City Identification
record CityIdentifier(
    String slug,
    String name,
    String state,
    String country,
    Long population,
    String sizeCategory  // "major", "mid-sized", "small"
)

// Feature Score (used for each category)
record FeatureScore(
    Double score,        // 0-100
    String tier,         // "excellent", "good", etc.
    String explanation,
    List<String> components
)

// Data Quality
record DataQualityMetadata(
    Double completenessPercentage,
    List<String> missingFields,
    String freshnessCategory,
    Double confidenceScore
)
```

---

## 🔧 Usage Examples

### Example 1: Render Personality Prompt

```java
@Autowired
private PromptBuilder promptBuilder;

public String generatePrompt(City city) {
    // Build structured input
    CityFeatureInput input = promptBuilder.buildFeatureInput(city);
    
    // Render using template
    return PromptTemplates.CITY_PERSONALITY.render(input);
}
```

### Example 2: Full Pipeline

```java
// Step 1: Fetch city
City city = cityRepository.findBySlug("san-francisco").orElseThrow();

// Step 2: Build feature input
CityFeatureInput input = promptBuilder.buildFeatureInput(city);

// Step 3: Render prompt
String prompt = PromptTemplates.CITY_COMPLETE_SUMMARY.render(input);

// Step 4: Validate
ValidationResult validation = promptBuilder.validatePrompt(prompt);
if (!validation.valid()) {
    log.warn("Prompt validation failed: {}", validation.errors());
}

// Step 5: Create audit log
PromptAuditLog log = promptBuilder.createAuditLog(
    city, 
    PromptTemplates.CITY_COMPLETE_SUMMARY, 
    prompt
);
```

---

## 📊 Rendered Prompt Example

```
═══════════════════════════════════════════════════════════════════════════════
PROMPT ID: CITY_PERSONALITY_001 | VERSION: 1.0.0
PURPOSE: Generate a 2-4 sentence personality description for a city
═══════════════════════════════════════════════════════════════════════════════

▓▓▓ CONTEXT ▓▓▓
You are analyzing San Francisco, located in USA.
Population: 874,961
Size Category: major

Your task is to generate a concise personality description that captures
the essence of this city based on its economic, livability, sustainability,
and growth characteristics.

Role: Act as an objective urban analyst providing factual, data-driven descriptions.

▓▓▓ FEATURE SUMMARY ▓▓▓
ECONOMY:
- Score: 82.5/100 (excellent)
- GDP per Capita: $95,000
- Unemployment Rate: 3.2%
- Cost of Living Index: 180
- Analysis: Strong tech-driven economy with high wages
- Contributing Factors: High GDP, Low unemployment

LIVABILITY:
- Score: 68.0/100 (good)
- Analysis: Good quality of life with cultural amenities
- Contributing Factors: Diverse entertainment, Public transit

SUSTAINABILITY:
- Score: 55.0/100 (average)
- Air Quality Index: 65 (moderate)
- Analysis: Moderate environmental conditions
- Contributing Factors: Some air quality concerns

GROWTH:
- Score: 45.0/100 (average)
- Analysis: Moderate growth trajectory
- Contributing Factors: Limited data available

OVERALL ASSESSMENT:
- Score: 67.5/100 (good)
- Analysis: Strong economic center with good livability

DATA QUALITY:
- Completeness: 75.0%
- Confidence: 72.0/100
- Missing Data: populationGrowthRate, gdpGrowthRate

▓▓▓ CONSTRAINTS ▓▓▓
DETERMINISM RULES:
- Same input features MUST produce the same personality description
- Use temperature=0 if using LLM
- No random or creative embellishments

SAFETY RULES:
- Do NOT include: political opinions, religious views, stereotypes
- Do NOT speculate beyond provided data
- All claims must be traceable to the feature scores provided

OUTPUT BOUNDARIES:
- Length: 2-4 sentences (max 500 characters)
- Tone: Professional, objective, informative
- Focus on the highest-impact features (excellent or poor tiers)

▓▓▓ OUTPUT FORMAT ▓▓▓
Return a plain text string containing 2-4 sentences.

STRUCTURE:
1. Opening: Identify the city's primary characteristic (strongest feature)
2. Middle: Describe 1-2 supporting characteristics
3. Closing: Note any significant trade-offs or considerations
```

---

## 🎤 Interview Talking Points

### Design Decisions

1. **Why fixed template structure?**
   > "The 4-section structure (Context → Features → Constraints → Output) ensures consistency across all prompts. It mirrors best practices in prompt engineering where context and constraints are explicit, reducing hallucination risk."

2. **Why typed inputs instead of raw strings?**
   > "CityFeatureInput uses strongly typed nested classes instead of String maps. This provides compile-time safety, IDE autocomplete, and prevents typos in field names that could silently fail."

3. **How is determinism guaranteed?**
   > "Three mechanisms: (1) Fixed seed=42 for any random operations, (2) Temperature=0 for LLM calls, (3) Input hashing for cache keying. Same CityFeatureInput always produces identical output."

4. **How are safety constraints enforced?**
   > "The PromptConstraints class defines FORBIDDEN_TOPICS as an immutable Set. Before any prompt is used, it's validated against this list. The rule-based generator also never produces content in these categories."

### Technical Deep Dives

5. **Variable interpolation system**
   > "Templates use `{{mustache}}` syntax with a regex pattern `\\{\\{(\\w+)\\}\\}`. The render() method builds a variable map from CityFeatureInput and performs substitution. Unresolved variables are kept as-is for debugging."

6. **Audit trail implementation**
   > "Every prompt generation creates a PromptAuditLog record containing: city slug, template ID, template version, SHA-256 hash of output, timestamp, and data confidence. This enables reproducibility audits."

7. **Output boundary enforcement**
   > "The PromptConstraints.validateOutput() method checks: personality length ≤ 500 chars, strengths count between 2-6, weaknesses count between 1-5, audience items between 2-6. Violations are logged."

---

## �️ AI Fallback System

The fallback system ensures **graceful degradation** when AI inference cannot complete normally. Users always receive useful output, never broken UX.

### Fallback Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           AI FALLBACK PIPELINE                                   │
└─────────────────────────────────────────────────────────────────────────────────┘

    PRIMARY INFERENCE
          │
          ▼
    ┌─────────────────┐
    │ Inference OK?   │──Yes──▶ Return Normal Response
    └────────┬────────┘
             │ No
             ▼
    ┌─────────────────┐
    │ TIER 1 FALLBACK │  ← Use available partial data (≥50% complete)
    │ (Partial Data)  │
    └────────┬────────┘
             │ Still failing?
             ▼
    ┌─────────────────┐
    │ TIER 2 FALLBACK │  ← Use city metadata only (name, country, population)
    │ (Metadata Only) │
    └────────┬────────┘
             │ Still failing?
             ▼
    ┌─────────────────┐
    │ TIER 3 FALLBACK │  ← Generic safe response (always succeeds)
    │ (Safe Default)  │
    └─────────────────┘
```

### Fallback Trigger Conditions

| Condition | Fallback Tier | User Experience |
|-----------|---------------|-----------------|
| Data < 50% complete | Tier 1 | Partial insights with caveats |
| Confidence < 40% | Tier 1 | Full insights with warnings |
| Only metadata available | Tier 2 | General city description |
| External APIs unavailable | Tier 2 | Cached/database-only response |
| Inference error | Tier 3 | Safe generic response |
| Unknown error | Tier 3 | "Try again later" message |

### Fallback Response Structure

Every fallback includes:

```java
FallbackResponse {
    // Fallback metadata
    FallbackTier tier;           // TIER_1, TIER_2, or TIER_3
    FallbackReason reason;       // Why fallback was triggered
    
    // Content (same structure as normal response)
    String personality;
    List<String> strengths;
    List<String> weaknesses;
    List<String> audienceSegments;
    
    // Quality indicators
    Double confidence;           // 0-100, lower for higher tiers
    List<String> caveats;        // Warnings for user
    Map<String, String> dataAvailability;  // What data was available
    
    // User communication
    String userMessage;          // Friendly explanation
}
```

### Design Principles

1. **NEVER RETURN NULL**: Always return a valid response object
2. **TRANSPARENCY**: Clearly indicate when fallback is used via caveats
3. **GRACEFUL DEGRADATION**: Each tier provides less detail but still useful info
4. **NO BROKEN UX**: Frontend can render any fallback response without errors
5. **SECURITY**: Never expose internal errors or sensitive data

### Example: Low Confidence Fallback

**Scenario**: City has 45% data completeness, confidence below threshold

**Fallback Response**:
```json
{
  "tier": "TIER_1_PARTIAL_DATA",
  "reason": "LOW_CONFIDENCE",
  "personality": "Berlin offers a distinctive urban experience. (Note: This assessment is based on limited data and should be considered preliminary.)",
  "strengths": ["Rich history", "Cultural hub"],
  "weaknesses": ["High cost of living"],
  "caveats": [
    "Some data fields are missing or incomplete",
    "Analysis based on limited information"
  ],
  "confidence": 35.0,
  "userMessage": "Our analysis has lower confidence due to limited data. Results should be verified."
}
```

### Integration with Inference Pipeline

```java
// In AiInferenceService.runInference()

// 1. Quality guard blocks → Incomplete data fallback
if (!guardResult.shouldProceed()) {
    return fallbackService.handleIncompleteData(city, input, dataQuality);
}

// 2. Low confidence detected → Low confidence fallback
if (confidence.overallConfidence() < LOW_CONFIDENCE_THRESHOLD) {
    return fallbackService.handleLowConfidence(city, insights, confidence);
}

// 3. Any exception → Error fallback (never throws to caller)
catch (Exception e) {
    return fallbackService.handleInferenceError(city, e);
}
```

### Interview Talking Points (Fallbacks)

1. **On graceful degradation**:
   > "Our tiered fallback system ensures users always get useful content. Tier 1 uses partial data, Tier 2 uses only metadata, and Tier 3 provides a safe generic response that never fails."

2. **On API resilience**:
   > "When external APIs (weather, AQI) are unavailable, we fall back to cached database data and clearly indicate that real-time information may be stale."

3. **On error handling**:
   > "We never expose internal errors to users. Errors are logged for debugging, but users see a friendly 'try again later' message."

4. **On UX consistency**:
   > "All fallback responses use the same structure as normal responses. The frontend doesn't need special handling - it can render any response, just with appropriate warnings."

---

## 📁 File Structure

```
backend/src/main/java/com/cityatlas/backend/ai/
├── CityFeatureInput.java      # Structured input schema
├── PromptConstraints.java     # Safety and determinism rules
├── PromptTemplate.java        # Template structure and rendering
├── PromptTemplates.java       # Pre-built production templates
├── PromptBuilder.java         # City → Input → Prompt pipeline
├── AiFallbackService.java     # Graceful degradation system
├── DataQualityChecker.java    # Validates data completeness
├── ConfidenceCalculator.java  # Calculates inference confidence
├── AiQualityGuard.java        # Pre-inference validation gate
└── AiDecisionLogger.java      # Audit logging for AI decisions
```

---

## 🔄 Integration with Existing Code

The prompt template system integrates with existing components:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ EXISTING COMPONENTS                    NEW PROMPT SYSTEM                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  CityFeatureComputer.java              CityFeatureInput.java                    │
│  (computes ScoreResult)    ───────▶    (structured input)                       │
│                                                                                  │
│  AiCitySummaryService.java             PromptBuilder.java                       │
│  (generates summary)       ◀───────    (builds & renders prompts)               │
│                                                                                  │
│  AiExplainabilityEngine.java           PromptTemplates.java                     │
│  (reasoning chains)        ───────▶    (uses same thresholds)                   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## ✅ Checklist: Production Readiness

- [x] Fixed template structure (Context/Features/Constraints/Output)
- [x] Strongly typed input schema (CityFeatureInput)
- [x] Determinism rules defined (seed, temperature, hashing)
- [x] Safety constraints (forbidden topics list)
- [x] Output boundaries (min/max for all arrays)
- [x] Score threshold definitions (excellent/good/average/below/poor)
- [x] Variable interpolation system ({{mustache}} syntax)
- [x] Validation methods (pre/post generation)
- [x] Audit logging (PromptAuditLog record)
- [x] Documentation with examples
- [x] Interview talking points

---

*Last Updated: 2024*
*Template Version: 1.0.0*
