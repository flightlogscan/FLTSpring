package com.flt.fltspring.service.transform;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Specialized transformer for handling airport codes with specific
 * logic for IATA/ICAO formats and multi-airport entries.
 */
@Component
@RequiredArgsConstructor
public class AirportCodeTransformer {
    
    private final TextTransformer textTransformer;
    private final CharacterReplacementConfig replacementConfig;
    
    // Common airport code patterns - standard 3-letter IATA or 4-letter ICAO
    private static final Pattern AIRPORT_CODE_PATTERN = Pattern.compile("^[A-Z]{3,4}$");
    
    /**
     * Transform a value that should be an airport code or codes
     * @param value The value to transform
     * @return The transformed airport code(s) as a string
     */
    public String transformAirportCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        
        // First convert to uppercase and trim
        String transformed = value.toUpperCase().trim();
        
        // Apply replacements
        transformed = textTransformer.transformWithReplacements(
            transformed, 
            replacementConfig.getAirportCodeReplacements(), 
            null, 
            null
        );
        
        // If it already matches a single airport code pattern, return as is
        if (AIRPORT_CODE_PATTERN.matcher(transformed).matches()) {
            return transformed;
        }
        
        // For multi-airport scenarios, try to identify distinct airport codes
        if (transformed.contains(" ") || transformed.contains("-") || 
            transformed.contains(",") || transformed.contains("/")) {
            
            // This field might contain multiple airport codes
            return handleMultipleAirportCodes(transformed);
        }
        
        // Check for common airport code prefixes and remove them
        String[] prefixesToRemove = {"AIRPORT ", "CODE ", "AP ", "APT "};
        for (String prefix : prefixesToRemove) {
            if (transformed.startsWith(prefix)) {
                transformed = transformed.substring(prefix.length());
                break;
            }
        }
        
        // Clean up: keep only letters
        transformed = transformed.replaceAll("[^A-Z]", "");
        
        // If it's exactly 3 or 4 letters after cleaning, it's likely a single code
        if (transformed.length() == 3 || transformed.length() == 4) {
            return transformed;
        }
        
        // For longer strings, don't try to aggressively extract a single code
        // Just preserve the cleaned string if it's reasonably short (up to 10 chars)
        if (transformed.length() <= 10) {
            return transformed;
        }
        
        // For very long strings, truncate to a reasonable length
        return transformed.substring(0, 10);
    }
    
    /**
     * Handle cases where a field might contain multiple airport codes
     */
    private String handleMultipleAirportCodes(String value) {
        // Split by common separators
        String[] parts = value.split("[\\s,/-]+");
        StringBuilder result = new StringBuilder();
        
        // Process each part as a potential airport code
        for (String part : parts) {
            // Ignore empty parts
            if (part.trim().isEmpty()) {
                continue;
            }
            
            // Keep parts that look like airport codes (3-4 letters)
            if (part.matches("[A-Z]{3,4}")) {
                if (result.length() > 0) {
                    result.append("-");
                }
                result.append(part);
            }
            // For parts that don't match exactly, clean them up
            else if (part.length() >= 2) {
                String cleaned = part.replaceAll("[^A-Z]", "");
                // If after cleaning we have something that looks like a code
                if (cleaned.length() >= 2 && cleaned.length() <= 4) {
                    if (result.length() > 0) {
                        result.append("-");
                    }
                    result.append(cleaned);
                }
            }
        }
        
        // If we found some airport codes, return them
        if (result.length() > 0) {
            return result.toString();
        }
        
        // Otherwise, just clean up the original value and return it
        return value.replaceAll("[^A-Z]", "");
    }
}