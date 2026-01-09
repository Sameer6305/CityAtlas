package com.cityatlas.backend.ai;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * PROMPT TEMPLATE - Fixed Structure for AI Generation
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Provides a fixed, immutable template structure for AI prompts.
 * Guarantees consistent format: CONTEXT → FEATURES → CONSTRAINTS → OUTPUT FORMAT
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * TEMPLATE STRUCTURE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ SECTION 1: CONTEXT                                                          │
 * │ - City identification (name, country, population)                           │
 * │ - Task description (what AI should generate)                                │
 * │ - Role definition (what perspective to take)                                │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *                                    ↓
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ SECTION 2: FEATURE SUMMARY                                                  │
 * │ - Economy score and components                                              │
 * │ - Livability score and components                                           │
 * │ - Sustainability score and components                                       │
 * │ - Growth score and components                                               │
 * │ - Overall assessment                                                        │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *                                    ↓
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ SECTION 3: CONSTRAINTS                                                      │
 * │ - Determinism rules                                                         │
 * │ - Safety rules (forbidden topics)                                           │
 * │ - Output boundaries (min/max items)                                         │
 * │ - Attribution requirements                                                  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *                                    ↓
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ SECTION 4: OUTPUT FORMAT                                                    │
 * │ - Expected JSON structure                                                   │
 * │ - Field descriptions                                                        │
 * │ - Example output                                                            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * INTERVIEW TALKING POINTS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * - "Templates use immutable records for type safety and thread safety"
 * - "Variable interpolation uses {{mustache}} syntax for clarity"
 * - "Each section serves a specific purpose in the prompt structure"
 * - "Templates can be versioned and audited for compliance"
 * 
 * @see CityFeatureInput
 * @see PromptConstraints
 */
public final class PromptTemplate {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEMPLATE METADATA
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final String templateId;
    private final String version;
    private final String purpose;
    private final TemplateSection context;
    private final TemplateSection featureSummary;
    private final TemplateSection constraints;
    private final TemplateSection outputFormat;
    
    // Regex pattern for variable interpolation: {{variableName}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    private PromptTemplate(Builder builder) {
        this.templateId = builder.templateId;
        this.version = builder.version;
        this.purpose = builder.purpose;
        this.context = builder.context;
        this.featureSummary = builder.featureSummary;
        this.constraints = builder.constraints;
        this.outputFormat = builder.outputFormat;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEMPLATE SECTION RECORD
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Represents a section of the prompt template.
     */
    public record TemplateSection(
        String sectionName,
        String content,
        boolean required
    ) {
        /**
         * Render this section with variable substitution.
         */
        public String render(Map<String, String> variables) {
            if (content == null) return "";
            
            Matcher matcher = VARIABLE_PATTERN.matcher(content);
            StringBuilder result = new StringBuilder();
            
            while (matcher.find()) {
                String variableName = matcher.group(1);
                String replacement = variables.getOrDefault(variableName, "{{" + variableName + "}}");
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(result);
            
            return result.toString();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDERING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Render the complete prompt with variable substitution.
     * 
     * @param variables Map of variable names to values
     * @return Fully rendered prompt string
     */
    public String render(Map<String, String> variables) {
        StringBuilder prompt = new StringBuilder();
        
        // Header with metadata
        prompt.append("═══════════════════════════════════════════════════════════════════════════════\n");
        prompt.append("PROMPT ID: ").append(templateId).append(" | VERSION: ").append(version).append("\n");
        prompt.append("PURPOSE: ").append(purpose).append("\n");
        prompt.append("═══════════════════════════════════════════════════════════════════════════════\n\n");
        
        // Section 1: Context
        if (context != null) {
            prompt.append("▓▓▓ CONTEXT ▓▓▓\n");
            prompt.append(context.render(variables)).append("\n\n");
        }
        
        // Section 2: Feature Summary
        if (featureSummary != null) {
            prompt.append("▓▓▓ FEATURE SUMMARY ▓▓▓\n");
            prompt.append(featureSummary.render(variables)).append("\n\n");
        }
        
        // Section 3: Constraints
        if (constraints != null) {
            prompt.append("▓▓▓ CONSTRAINTS ▓▓▓\n");
            prompt.append(constraints.render(variables)).append("\n\n");
        }
        
        // Section 4: Output Format
        if (outputFormat != null) {
            prompt.append("▓▓▓ OUTPUT FORMAT ▓▓▓\n");
            prompt.append(outputFormat.render(variables)).append("\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * Render the prompt from a CityFeatureInput object.
     * 
     * @param input Structured city feature input
     * @return Fully rendered prompt string
     */
    public String render(CityFeatureInput input) {
        return render(buildVariableMap(input));
    }
    
    /**
     * Build variable map from CityFeatureInput.
     */
    private Map<String, String> buildVariableMap(CityFeatureInput input) {
        java.util.Map<String, String> vars = new java.util.HashMap<>();
        
        // City identification
        if (input.getCityIdentifier() != null) {
            CityFeatureInput.CityIdentifier city = input.getCityIdentifier();
            vars.put("cityName", nullSafe(city.getName()));
            vars.put("citySlug", nullSafe(city.getSlug()));
            vars.put("country", nullSafe(city.getCountry()));
            vars.put("state", nullSafe(city.getState()));
            vars.put("population", formatNumber(city.getPopulation()));
            vars.put("sizeCategory", nullSafe(city.getSizeCategory()));
        }
        
        // Economy features
        if (input.getEconomyFeatures() != null) {
            CityFeatureInput.EconomyFeatures eco = input.getEconomyFeatures();
            vars.put("economyScore", formatScore(eco.getEconomyScore()));
            vars.put("economyTier", nullSafe(eco.getEconomyTier()));
            vars.put("gdpPerCapita", formatCurrency(eco.getGdpPerCapita()));
            vars.put("unemploymentRate", formatPercent(eco.getUnemploymentRate()));
            vars.put("costOfLiving", formatIndex(eco.getCostOfLivingIndex()));
            vars.put("economyExplanation", nullSafe(eco.getExplanation()));
            vars.put("economyComponents", formatList(eco.getComponents()));
        }
        
        // Livability features
        if (input.getLivabilityFeatures() != null) {
            CityFeatureInput.LivabilityFeatures liv = input.getLivabilityFeatures();
            vars.put("livabilityScore", formatScore(liv.getLivabilityScore()));
            vars.put("livabilityTier", nullSafe(liv.getLivabilityTier()));
            vars.put("livabilityExplanation", nullSafe(liv.getExplanation()));
            vars.put("livabilityComponents", formatList(liv.getComponents()));
        }
        
        // Sustainability features
        if (input.getSustainabilityFeatures() != null) {
            CityFeatureInput.SustainabilityFeatures sus = input.getSustainabilityFeatures();
            vars.put("sustainabilityScore", formatScore(sus.getSustainabilityScore()));
            vars.put("sustainabilityTier", nullSafe(sus.getSustainabilityTier()));
            vars.put("aqiIndex", formatIndex(sus.getAqiIndex()));
            vars.put("aqiCategory", nullSafe(sus.getAqiCategory()));
            vars.put("sustainabilityExplanation", nullSafe(sus.getExplanation()));
            vars.put("sustainabilityComponents", formatList(sus.getComponents()));
        }
        
        // Growth features
        if (input.getGrowthFeatures() != null) {
            CityFeatureInput.GrowthFeatures growth = input.getGrowthFeatures();
            vars.put("growthScore", formatScore(growth.getGrowthScore()));
            vars.put("growthTier", nullSafe(growth.getGrowthTier()));
            vars.put("growthExplanation", nullSafe(growth.getExplanation()));
            vars.put("growthComponents", formatList(growth.getComponents()));
        }
        
        // Overall assessment
        if (input.getOverallAssessment() != null) {
            CityFeatureInput.OverallAssessment overall = input.getOverallAssessment();
            vars.put("overallScore", formatScore(overall.getOverallScore()));
            vars.put("overallTier", nullSafe(overall.getOverallTier()));
            vars.put("overallExplanation", nullSafe(overall.getExplanation()));
        }
        
        // Data quality
        vars.put("dataCompleteness", String.format("%.1f%%", input.getDataCompleteness()));
        if (input.getDataQuality() != null) {
            vars.put("confidenceScore", formatScore(input.getDataQuality().getConfidenceScore() != null 
                ? input.getDataQuality().getConfidenceScore() * 100 : null));
            vars.put("missingFields", formatList(input.getDataQuality().getMissingFields()));
        }
        
        // Metadata
        vars.put("generatedAt", Instant.now().toString());
        vars.put("templateId", templateId);
        vars.put("templateVersion", version);
        
        return vars;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FORMATTING HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static String nullSafe(String value) {
        return value != null ? value : "N/A";
    }
    
    private static String formatNumber(Long value) {
        if (value == null) return "N/A";
        return String.format("%,d", value);
    }
    
    private static String formatScore(Double value) {
        if (value == null) return "N/A";
        return String.format("%.1f/100", value);
    }
    
    private static String formatCurrency(Double value) {
        if (value == null) return "N/A";
        return String.format("$%,.0f", value);
    }
    
    private static String formatPercent(Double value) {
        if (value == null) return "N/A";
        return String.format("%.1f%%", value);
    }
    
    private static String formatIndex(Integer value) {
        if (value == null) return "N/A";
        return String.valueOf(value);
    }
    
    private static String formatList(java.util.List<String> items) {
        if (items == null || items.isEmpty()) return "None";
        return String.join(", ", items);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public String getTemplateId() { return templateId; }
    public String getVersion() { return version; }
    public String getPurpose() { return purpose; }
    public TemplateSection getContext() { return context; }
    public TemplateSection getFeatureSummary() { return featureSummary; }
    public TemplateSection getConstraints() { return constraints; }
    public TemplateSection getOutputFormat() { return outputFormat; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String templateId;
        private String version = "1.0.0";
        private String purpose;
        private TemplateSection context;
        private TemplateSection featureSummary;
        private TemplateSection constraints;
        private TemplateSection outputFormat;
        
        public Builder templateId(String templateId) {
            this.templateId = templateId;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder purpose(String purpose) {
            this.purpose = purpose;
            return this;
        }
        
        public Builder context(String content) {
            this.context = new TemplateSection("CONTEXT", content, true);
            return this;
        }
        
        public Builder featureSummary(String content) {
            this.featureSummary = new TemplateSection("FEATURE_SUMMARY", content, true);
            return this;
        }
        
        public Builder constraints(String content) {
            this.constraints = new TemplateSection("CONSTRAINTS", content, true);
            return this;
        }
        
        public Builder outputFormat(String content) {
            this.outputFormat = new TemplateSection("OUTPUT_FORMAT", content, true);
            return this;
        }
        
        public PromptTemplate build() {
            if (templateId == null) {
                throw new IllegalStateException("Template ID is required");
            }
            return new PromptTemplate(this);
        }
    }
}
