package com.flt.fltspring.service.transform;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Handles header matching and classification for flight logbook data
 */
@Component
@RequiredArgsConstructor
public class HeaderMatcher {

    private final TextTransformer textTransformer;
    
    // Common expected headers for flight logbooks
    private static final List<String> COMMON_FLIGHT_HEADERS = Arrays.asList(
        "DATE", "FROM", "TO", "AIRCRAFT", "REGISTRATION", "AIRCRAFT TYPE", "REG",
        "PIC", "SIC", "DUAL GIVEN", "DUAL RECEIVED", "GROUND", "SIMULATOR",
        "TOTAL", "DAY", "NIGHT", "ACTUAL INSTRUMENT", "SIMULATED INSTRUMENT", 
        "CROSS COUNTRY", "NUMBER OF LANDINGS", "APPROACHES", "REMARKS"
    );
    
    /**
     * Find the closest matching header from a list of common headers
     * using a simple similarity metric
     */
    public String findClosestMatch(String headerValue, List<String> possibleHeaders) {
        if (headerValue == null || headerValue.isEmpty()) {
            return null;
        }
        
        List<String> headersToUse = possibleHeaders != null && !possibleHeaders.isEmpty() 
            ? possibleHeaders 
            : COMMON_FLIGHT_HEADERS;
        
        // Direct match check first
        for (String commonHeader : headersToUse) {
            if (headerValue.equals(commonHeader)) {
                return commonHeader;
            }
        }
        
        String best = null;
        int bestScore = 0;
        
        for (String commonHeader : headersToUse) {
            int score = calculateSimilarity(headerValue, commonHeader);
            if (score > bestScore) {
                bestScore = score;
                best = commonHeader;
            }
        }
        
        // Only return if we have a reasonably good match
        return bestScore > headerValue.length() / 2 ? best : null;
    }
    
    /**
     * Calculate a simple similarity score between two strings
     * based on character overlap
     */
    private int calculateSimilarity(String s1, String s2) {
        // This is a simplified matching algorithm
        // Could be replaced with Levenshtein distance or other more sophisticated metrics
        String a = s1.replaceAll("\\s+", "");
        String b = s2.replaceAll("\\s+", "");
        
        int score = 0;
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                score++;
            }
        }
        
        return score;
    }
    
    /**
     * Infer if a header likely represents an airport code field
     */
    public boolean inferAirportCodeType(String headerValue) {
        if (headerValue == null) return false;
        
        String normalized = headerValue.toUpperCase();
        return normalized.contains("FROM") || 
               normalized.contains("TO") || 
               normalized.contains("AIRPORT") || 
               normalized.contains("ORIGIN") || 
               normalized.contains("DEST");
    }
    
    /**
     * Infer if a header likely represents a numeric field
     */
    public boolean inferNumericType(String headerValue) {
        if (headerValue == null) return false;
        
        String normalized = headerValue.toUpperCase();
        return normalized.contains("TOTAL") || 
               normalized.contains("NUMBER") || 
               normalized.contains("TIME") || 
               normalized.contains("HOUR") || 
               normalized.contains("COUNT") || 
               normalized.contains("LANDINGS");
    }
    
    /**
     * Check if a column is likely to contain time data
     */
    public boolean isLikelyTimeField(String columnName) {
        String normalized = columnName.toUpperCase();
        return normalized.contains("TIME") || 
               normalized.contains("HOUR") || 
               normalized.contains("DURATION");
    }
}