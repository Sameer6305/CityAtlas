package com.cityatlas.backend.ai;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * PRODUCTION-READY PROMPT TEMPLATES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Provides pre-built, tested prompt templates for all AI generation tasks.
 * Each template follows the fixed structure: Context → Features → Constraints → Output.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * AVAILABLE TEMPLATES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. CITY_PERSONALITY - Generates city personality description
 * 2. CITY_STRENGTHS - Generates list of city strengths
 * 3. CITY_WEAKNESSES - Generates list of city weaknesses
 * 4. CITY_AUDIENCE - Generates best-suited-for audience list
 * 5. CITY_COMPLETE_SUMMARY - Full AI summary combining all elements
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * USAGE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * <pre>
 * // Build structured input from city data
 * CityFeatureInput input = CityFeatureInput.builder()
 *     .cityIdentifier(...)
 *     .economyFeatures(...)
 *     .build();
 * 
 * // Render the prompt
 * String prompt = PromptTemplates.CITY_PERSONALITY.render(input);
 * 
 * // Prompt is now ready for:
 * // 1. Rule-based generation (current approach)
 * // 2. LLM API call (future integration)
 * </pre>
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * INTERVIEW TALKING POINTS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * - "Templates are defined as immutable singletons for performance"
 * - "Each template has a unique ID for audit logging"
 * - "Templates can be A/B tested by using different versions"
 * - "The fixed structure ensures consistency across all AI outputs"
 * - "Templates are production-ready and can be used with any LLM backend"
 * 
 * @see PromptTemplate
 * @see CityFeatureInput
 * @see PromptConstraints
 */
public final class PromptTemplates {
    
    private PromptTemplates() {
        // Utility class - prevent instantiation
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEMPLATE 1: CITY PERSONALITY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Template for generating city personality description.
     * Output: 2-4 sentence personality summary.
     */
    public static final PromptTemplate CITY_PERSONALITY = PromptTemplate.builder()
        .templateId("CITY_PERSONALITY_001")
        .version("1.0.0")
        .purpose("Generate a 2-4 sentence personality description for a city based on its feature scores")
        .context("""
            You are analyzing {{cityName}}, located in {{country}}.
            Population: {{population}}
            Size Category: {{sizeCategory}}
            
            Your task is to generate a concise personality description that captures
            the essence of this city based on its economic, livability, sustainability,
            and growth characteristics.
            
            Role: Act as an objective urban analyst providing factual, data-driven descriptions.
            """)
        .featureSummary("""
            ECONOMY:
            - Score: {{economyScore}} ({{economyTier}})
            - GDP per Capita: {{gdpPerCapita}}
            - Unemployment Rate: {{unemploymentRate}}
            - Cost of Living Index: {{costOfLiving}}
            - Analysis: {{economyExplanation}}
            - Contributing Factors: {{economyComponents}}
            
            LIVABILITY:
            - Score: {{livabilityScore}} ({{livabilityTier}})
            - Analysis: {{livabilityExplanation}}
            - Contributing Factors: {{livabilityComponents}}
            
            SUSTAINABILITY:
            - Score: {{sustainabilityScore}} ({{sustainabilityTier}})
            - Air Quality Index: {{aqiIndex}} ({{aqiCategory}})
            - Analysis: {{sustainabilityExplanation}}
            - Contributing Factors: {{sustainabilityComponents}}
            
            GROWTH:
            - Score: {{growthScore}} ({{growthTier}})
            - Analysis: {{growthExplanation}}
            - Contributing Factors: {{growthComponents}}
            
            OVERALL ASSESSMENT:
            - Score: {{overallScore}} ({{overallTier}})
            - Analysis: {{overallExplanation}}
            
            DATA QUALITY:
            - Completeness: {{dataCompleteness}}
            - Confidence: {{confidenceScore}}
            - Missing Data: {{missingFields}}
            """)
        .constraints("""
            DETERMINISM RULES:
            - Same input features MUST produce the same personality description
            - Use temperature=0 if using LLM
            - No random or creative embellishments
            
            SAFETY RULES:
            - Do NOT include: political opinions, religious views, stereotypes
            - Do NOT speculate beyond provided data
            - Do NOT compare to other cities unless data supports it
            - All claims must be traceable to the feature scores provided
            
            OUTPUT BOUNDARIES:
            - Length: 2-4 sentences (max 500 characters)
            - Tone: Professional, objective, informative
            - Focus on the highest-impact features (excellent or poor tiers)
            
            ATTRIBUTION:
            - Each claim should reference the relevant score tier
            - Use hedging language ("tends to", "generally") for uncertain data
            - Acknowledge data limitations if confidence < 50%
            """)
        .outputFormat("""
            Return a plain text string containing 2-4 sentences.
            
            STRUCTURE:
            1. Opening: Identify the city's primary characteristic (strongest feature)
            2. Middle: Describe 1-2 supporting characteristics
            3. Closing: Note any significant trade-offs or considerations
            
            EXAMPLE OUTPUT:
            "San Francisco is a thriving economic hub with exceptional job opportunities
            in the tech sector. The city offers excellent livability with diverse cultural
            attractions, though its high cost of living presents challenges for newcomers.
            Environmental conditions are generally good with moderate air quality."
            
            FORBIDDEN:
            - Starting with "This city..."
            - Using superlatives without data support
            - Including specific dollar amounts or percentages in the personality text
            """)
        .build();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEMPLATE 2: CITY STRENGTHS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Template for generating city strengths list.
     * Output: 2-6 strengths with explanations.
     */
    public static final PromptTemplate CITY_STRENGTHS = PromptTemplate.builder()
        .templateId("CITY_STRENGTHS_001")
        .version("1.0.0")
        .purpose("Generate a list of 2-6 strengths for a city based on features scoring GOOD (60+) or EXCELLENT (80+)")
        .context("""
            You are identifying the key strengths of {{cityName}}, {{country}}.
            Population: {{population}}
            
            Your task is to generate a list of 2-6 strengths based on features that
            score GOOD (60+) or EXCELLENT (80+). Each strength must be directly
            traceable to a specific score.
            
            Role: Act as an objective city analyst highlighting positive attributes.
            """)
        .featureSummary("""
            SCORES AND TIERS:
            - Economy: {{economyScore}} ({{economyTier}}) - {{economyExplanation}}
            - Livability: {{livabilityScore}} ({{livabilityTier}}) - {{livabilityExplanation}}
            - Sustainability: {{sustainabilityScore}} ({{sustainabilityTier}}) - {{sustainabilityExplanation}}
            - Growth: {{growthScore}} ({{growthTier}}) - {{growthExplanation}}
            - Overall: {{overallScore}} ({{overallTier}}) - {{overallExplanation}}
            
            DETAIL COMPONENTS:
            - Economy factors: {{economyComponents}}
            - Livability factors: {{livabilityComponents}}
            - Sustainability factors: {{sustainabilityComponents}}
            - Growth factors: {{growthComponents}}
            
            DATA QUALITY: {{dataCompleteness}} complete, confidence: {{confidenceScore}}
            """)
        .constraints("""
            DETERMINISM RULES:
            - Only include strengths for scores >= 60 (GOOD tier or above)
            - Order by score descending (highest first)
            - Same input MUST produce same strengths list
            
            SAFETY RULES:
            - Do NOT exaggerate positive attributes
            - Do NOT claim strength for scores < 60
            - All strengths must reference the score that supports them
            
            OUTPUT BOUNDARIES:
            - Minimum: 2 strengths
            - Maximum: 6 strengths
            - Each strength: 1 line with score reference
            
            ATTRIBUTION:
            - Format: "Strength description (score: X/100)"
            - Include tier name in description
            """)
        .outputFormat("""
            Return a JSON array of strings, each representing one strength.
            
            STRUCTURE PER STRENGTH:
            "[Category]: [Description] (score: [X]/100)"
            
            EXAMPLE OUTPUT:
            [
              "Strong economy with diverse job market (score: 78/100)",
              "Excellent environmental quality with good air conditions (score: 85/100)",
              "Good livability with cultural amenities (score: 65/100)"
            ]
            
            INCLUSION RULES:
            - Economy >= 60: Include "Strong economy..." or "Thriving job market..."
            - Livability >= 60: Include "Good quality of life..." or "Excellent amenities..."
            - Sustainability >= 60: Include "Good environmental quality..." 
            - Growth >= 60: Include "Strong growth trajectory..."
            
            FORBIDDEN:
            - Including strengths for scores < 60
            - Generic statements without score attribution
            - Comparative claims ("better than...")
            """)
        .build();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEMPLATE 3: CITY WEAKNESSES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Template for generating city weaknesses/challenges list.
     * Output: 1-5 weaknesses with explanations.
     */
    public static final PromptTemplate CITY_WEAKNESSES = PromptTemplate.builder()
        .templateId("CITY_WEAKNESSES_001")
        .version("1.0.0")
        .purpose("Generate a list of 1-5 challenges for a city based on features scoring BELOW-AVERAGE (<40) or POOR (<20)")
        .context("""
            You are identifying the key challenges of {{cityName}}, {{country}}.
            Population: {{population}}
            
            Your task is to generate a list of 1-5 challenges/areas for improvement based
            on features that score BELOW-AVERAGE (<40) or POOR (<20). Frame challenges
            constructively as "areas for improvement" rather than harsh criticisms.
            
            Role: Act as an objective city analyst noting areas needing attention.
            """)
        .featureSummary("""
            SCORES AND TIERS:
            - Economy: {{economyScore}} ({{economyTier}}) - {{economyExplanation}}
            - Livability: {{livabilityScore}} ({{livabilityTier}}) - {{livabilityExplanation}}
            - Sustainability: {{sustainabilityScore}} ({{sustainabilityTier}}) - {{sustainabilityExplanation}}
            - Growth: {{growthScore}} ({{growthTier}}) - {{growthExplanation}}
            - Overall: {{overallScore}} ({{overallTier}}) - {{overallExplanation}}
            
            DETAIL COMPONENTS:
            - Economy factors: {{economyComponents}}
            - Livability factors: {{livabilityComponents}}
            - Sustainability factors: {{sustainabilityComponents}}
            - Growth factors: {{growthComponents}}
            
            DATA QUALITY: {{dataCompleteness}} complete
            MISSING DATA: {{missingFields}}
            """)
        .constraints("""
            DETERMINISM RULES:
            - Only include weaknesses for scores < 40 (BELOW-AVERAGE or POOR)
            - Order by score ascending (lowest/worst first)
            - Same input MUST produce same weaknesses list
            
            SAFETY RULES:
            - Use constructive framing ("area for improvement", "challenge")
            - Do NOT use harsh language ("terrible", "awful", "worst")
            - Do NOT claim weakness for scores >= 40
            - Note missing data as a limitation, not a weakness
            
            OUTPUT BOUNDARIES:
            - Minimum: 1 weakness (if any score < 40)
            - Maximum: 5 weaknesses
            - Each weakness: 1 line with score reference
            
            SPECIAL CASE:
            - If no scores < 40: Return empty array []
            - If data is missing: Note "Limited data" but don't fabricate weaknesses
            """)
        .outputFormat("""
            Return a JSON array of strings, each representing one challenge.
            
            STRUCTURE PER WEAKNESS:
            "[Category] challenge: [Description] (score: [X]/100)"
            
            EXAMPLE OUTPUT:
            [
              "Economic challenges with limited job opportunities (score: 35/100)",
              "Environmental concerns with air quality issues (score: 28/100)"
            ]
            
            INCLUSION RULES:
            - Economy < 40: Include "Economic challenges..."
            - Livability < 40: Include "Quality of life concerns..."
            - Sustainability < 40: Include "Environmental challenges..."
            - Growth < 40: Include "Limited growth prospects..."
            
            FORBIDDEN:
            - Including weaknesses for scores >= 40
            - Harsh or judgmental language
            - Speculating about causes not in the data
            """)
        .build();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEMPLATE 4: CITY AUDIENCE (Best Suited For)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Template for generating best-suited-for audience recommendations.
     * Output: 2-6 audience segments.
     */
    public static final PromptTemplate CITY_AUDIENCE = PromptTemplate.builder()
        .templateId("CITY_AUDIENCE_001")
        .version("1.0.0")
        .purpose("Generate a list of 2-6 audience segments best suited for this city based on feature score combinations")
        .context("""
            You are recommending ideal resident profiles for {{cityName}}, {{country}}.
            Population: {{population}}
            Size Category: {{sizeCategory}}
            
            Your task is to identify 2-6 demographic or lifestyle segments that would
            benefit most from this city's characteristics, based on the feature score
            combinations.
            
            Role: Act as a relocation advisor matching people to cities.
            """)
        .featureSummary("""
            SCORE SUMMARY:
            - Economy: {{economyScore}} ({{economyTier}})
            - Livability: {{livabilityScore}} ({{livabilityTier}})
            - Sustainability: {{sustainabilityScore}} ({{sustainabilityTier}})
            - Growth: {{growthScore}} ({{growthTier}})
            - Overall: {{overallScore}} ({{overallTier}})
            
            KEY ECONOMIC METRICS:
            - GDP per Capita: {{gdpPerCapita}}
            - Unemployment: {{unemploymentRate}}
            - Cost of Living: {{costOfLiving}}
            
            DATA COMPLETENESS: {{dataCompleteness}}
            """)
        .constraints("""
            DETERMINISM RULES:
            - Use score thresholds to determine audience fit
            - Same input scores MUST produce same audience list
            - Apply rules in consistent order
            
            AUDIENCE MAPPING RULES:
            - Economy >= 60: "Career-focused professionals"
            - Economy >= 60 AND Cost of Living < 100: "Young professionals seeking affordability"
            - Livability >= 60: "Families seeking quality of life"
            - Sustainability >= 70: "Environmentally conscious residents"
            - Growth >= 60: "Entrepreneurs and startup founders"
            - Economy >= 60 AND Livability >= 60: "Remote workers seeking balance"
            - Low cost of living (<80): "Retirees on fixed income"
            
            SAFETY RULES:
            - Do NOT exclude based on protected characteristics
            - Frame positively (who it's good for, not who should avoid)
            - All recommendations must be traceable to scores
            
            OUTPUT BOUNDARIES:
            - Minimum: 2 audience segments
            - Maximum: 6 audience segments
            """)
        .outputFormat("""
            Return a JSON array of strings, each representing one audience segment.
            
            STRUCTURE PER SEGMENT:
            "[Segment name] - [Reason based on scores]"
            
            EXAMPLE OUTPUT:
            [
              "Career-focused professionals seeking strong job markets",
              "Families looking for good quality of life and amenities",
              "Environmentally conscious individuals valuing clean air",
              "Remote workers seeking work-life balance"
            ]
            
            FORBIDDEN:
            - Mentioning income levels explicitly
            - Age-based exclusions
            - Any protected class references
            - Recommending without score justification
            """)
        .build();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEMPLATE 5: COMPLETE CITY SUMMARY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Template for generating complete AI city summary.
     * Output: Full summary with personality, strengths, weaknesses, and audience.
     */
    public static final PromptTemplate CITY_COMPLETE_SUMMARY = PromptTemplate.builder()
        .templateId("CITY_COMPLETE_SUMMARY_001")
        .version("1.0.0")
        .purpose("Generate a complete AI city summary including personality, strengths, weaknesses, and audience recommendations")
        .context("""
            You are generating a comprehensive AI-powered summary for {{cityName}}, {{country}}.
            Population: {{population}}
            Size Category: {{sizeCategory}}
            
            This summary will be displayed on the city's profile page and must be:
            1. Factual and data-driven
            2. Balanced (showing both strengths and areas for improvement)
            3. Actionable (helping users decide if this city fits their needs)
            
            Role: Act as an authoritative city analyst providing a complete assessment.
            """)
        .featureSummary("""
            ═══════════════════════════════════════════════════════════════════════════════
            COMPREHENSIVE FEATURE ANALYSIS
            ═══════════════════════════════════════════════════════════════════════════════
            
            ECONOMY (Weight: 30%):
            - Score: {{economyScore}} ({{economyTier}})
            - GDP per Capita: {{gdpPerCapita}}
            - Unemployment Rate: {{unemploymentRate}}
            - Cost of Living Index: {{costOfLiving}}
            - Analysis: {{economyExplanation}}
            - Components: {{economyComponents}}
            
            LIVABILITY (Weight: 35%):
            - Score: {{livabilityScore}} ({{livabilityTier}})
            - Analysis: {{livabilityExplanation}}
            - Components: {{livabilityComponents}}
            
            SUSTAINABILITY (Weight: 20%):
            - Score: {{sustainabilityScore}} ({{sustainabilityTier}})
            - Air Quality Index: {{aqiIndex}} ({{aqiCategory}})
            - Analysis: {{sustainabilityExplanation}}
            - Components: {{sustainabilityComponents}}
            
            GROWTH (Weight: 15%):
            - Score: {{growthScore}} ({{growthTier}})
            - Analysis: {{growthExplanation}}
            - Components: {{growthComponents}}
            
            ═══════════════════════════════════════════════════════════════════════════════
            OVERALL ASSESSMENT
            ═══════════════════════════════════════════════════════════════════════════════
            
            - Final Score: {{overallScore}} ({{overallTier}})
            - Summary: {{overallExplanation}}
            
            DATA QUALITY:
            - Completeness: {{dataCompleteness}}
            - Confidence: {{confidenceScore}}
            - Missing Fields: {{missingFields}}
            """)
        .constraints("""
            ═══════════════════════════════════════════════════════════════════════════════
            STRICT CONSTRAINTS
            ═══════════════════════════════════════════════════════════════════════════════
            
            DETERMINISM:
            - Temperature: 0 (no randomness)
            - Fixed seed: 42
            - Same input MUST produce byte-identical output
            
            SAFETY (ABSOLUTE RULES - NEVER VIOLATE):
            ✗ No political content or opinions
            ✗ No religious references
            ✗ No stereotypes (racial, ethnic, gender, age)
            ✗ No speculation beyond provided data
            ✗ No comparative rankings unless data-supported
            ✗ No income-based discrimination
            
            OUTPUT LIMITS:
            - Personality: 2-4 sentences (max 500 chars)
            - Strengths: 2-6 items (scores >= 60 only)
            - Weaknesses: 1-5 items (scores < 40 only)
            - Audience: 2-6 items (score-derived only)
            
            ATTRIBUTION REQUIREMENTS:
            - Every claim must reference a score
            - Confidence < 50%: Add uncertainty language
            - Missing data: Acknowledge explicitly
            """)
        .outputFormat("""
            Return a JSON object with the following structure:
            
            {
              "personality": "2-4 sentence description...",
              "strengths": [
                "Strength 1 with score attribution",
                "Strength 2 with score attribution"
              ],
              "weaknesses": [
                "Weakness 1 with score attribution"
              ],
              "bestSuitedFor": [
                "Audience segment 1",
                "Audience segment 2"
              ],
              "dataConfidence": 0.85,
              "generatedAt": "2024-01-15T10:30:00Z",
              "templateVersion": "1.0.0"
            }
            
            VALIDATION RULES:
            - personality.length <= 500
            - strengths.length >= 2 && strengths.length <= 6
            - weaknesses.length >= 1 && weaknesses.length <= 5
            - bestSuitedFor.length >= 2 && bestSuitedFor.length <= 6
            - All arrays are non-null
            """)
        .build();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEMPLATE REGISTRY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get all available templates.
     */
    public static PromptTemplate[] getAllTemplates() {
        return new PromptTemplate[] {
            CITY_PERSONALITY,
            CITY_STRENGTHS,
            CITY_WEAKNESSES,
            CITY_AUDIENCE,
            CITY_COMPLETE_SUMMARY
        };
    }
    
    /**
     * Get template by ID.
     */
    public static PromptTemplate getById(String templateId) {
        for (PromptTemplate template : getAllTemplates()) {
            if (template.getTemplateId().equals(templateId)) {
                return template;
            }
        }
        throw new IllegalArgumentException("Unknown template ID: " + templateId);
    }
}
