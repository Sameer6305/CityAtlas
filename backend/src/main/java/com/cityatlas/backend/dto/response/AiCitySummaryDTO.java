package com.cityatlas.backend.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI City Summary Data Transfer Object
 * 
 * Contains AI-generated insights and analysis about a city's character,
 * strengths, weaknesses, and ideal resident profiles.
 * 
 * This DTO is used to transfer AI-generated city personality analysis
 * from the backend to the frontend for display on the AI Summary page.
 * 
 * Example Usage:
 * <pre>
 * AiCitySummaryDTO summary = AiCitySummaryDTO.builder()
 *     .personality("A vibrant tech hub with a laid-back coastal vibe")
 *     .strengths(List.of("Innovation ecosystem", "Quality of life"))
 *     .weaknesses(List.of("High cost of living", "Traffic congestion"))
 *     .bestSuitedFor(List.of("Tech professionals", "Entrepreneurs"))
 *     .build();
 * </pre>
 * 
 * @see com.cityatlas.backend.entity.AISummary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCitySummaryDTO {
    
    /**
     * City's personality description
     * 
     * A concise, AI-generated description capturing the city's unique character,
     * culture, and overall vibe. This should be a single paragraph (2-4 sentences)
     * that gives readers an immediate sense of what the city is like.
     * 
     * Example: "A vibrant tech hub nestled in the Pacific Northwest, combining 
     * cutting-edge innovation with a laid-back, outdoor-focused lifestyle. Known 
     * for its coffee culture, progressive values, and thriving startup ecosystem."
     */
    private String personality;
    
    /**
     * List of city's key strengths
     * 
     * AI-identified advantages and positive attributes that make this city 
     * attractive to residents and businesses. Each strength should be concise
     * (2-5 words) and focus on tangible benefits.
     * 
     * Examples:
     * - "Thriving tech ecosystem"
     * - "Excellent public transportation"
     * - "World-class universities"
     * - "Low crime rates"
     * - "Diverse culinary scene"
     * 
     * Typical count: 5-8 strengths
     */
    private List<String> strengths;
    
    /**
     * List of city's key weaknesses or challenges
     * 
     * AI-identified drawbacks, challenges, or areas of concern that potential
     * residents should be aware of. Each weakness should be honest but fair,
     * focusing on objective issues rather than subjective opinions.
     * 
     * Examples:
     * - "High cost of living"
     * - "Limited job market diversity"
     * - "Harsh winter weather"
     * - "Air quality concerns"
     * - "Aging infrastructure"
     * 
     * Typical count: 3-5 weaknesses
     */
    private List<String> weaknesses;
    
    /**
     * List of ideal resident profiles or demographics
     * 
     * AI-generated descriptions of who would thrive in this city based on
     * lifestyle, career, values, and personal preferences. Each profile should
     * describe a specific type of person or demographic.
     * 
     * Examples:
     * - "Tech professionals and software engineers"
     * - "Young families seeking good schools"
     * - "Artists and creative entrepreneurs"
     * - "Remote workers prioritizing lifestyle"
     * - "Graduate students and researchers"
     * - "Outdoor enthusiasts and nature lovers"
     * 
     * Typical count: 4-6 profiles
     */
    private List<String> bestSuitedFor;
}
