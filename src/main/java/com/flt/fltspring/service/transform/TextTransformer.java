package com.flt.fltspring.service.transform;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Core text transformation utilities for handling common OCR issues
 * and standardizing text across the application.
 */
@Component
@RequiredArgsConstructor
public class TextTransformer {
    
    private final CharacterReplacementConfig replacementConfig;
    
    /**
     * Transform a value that should be an integer
     * @param value The value to transform
     * @return The transformed integer as a string
     */
    public String transformInteger(String value) {
        return transformWithReplacements(
            value, 
            replacementConfig.getNumericReplacements(), 
            "[^0-9]", 
            "0"
        );
    }
    
    /**
     * Applies character replacements and filtering to transform values
     * @param value Original string value
     * @param replacements Map of character replacements to apply
     * @param filterPattern Regular expression pattern to filter out characters (can be null)
     * @param defaultValue Default value if result is empty (can be null)
     * @return Transformed string
     */
    public String transformWithReplacements(String value, Map<String, String> replacements, 
                                          String filterPattern, String defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue != null ? defaultValue : "";
        }
        
        // Apply character replacements
        String transformed = value;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            transformed = transformed.replace(replacement.getKey(), replacement.getValue());
        }
        
        // Apply filter pattern if provided
        if (filterPattern != null) {
            transformed = transformed.replaceAll(filterPattern, "");
            
            // Return default if empty
            if (transformed.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
        }
        
        return transformed;
    }
    
    /**
     * Normalize a header value by removing special characters, converting to uppercase,
     * and applying common substitutions for OCR errors
     */
    public String normalizeHeaderValue(String headerValue) {
        if (headerValue == null) {
            return "";
        }
        
        // Convert to uppercase and trim
        String normalized = headerValue.toUpperCase().trim();
        
        // Remove common punctuation and spaces
        normalized = normalized.replaceAll("[^A-Z0-9]", " ")
                              .replaceAll("\\s+", " ")
                              .trim();

        // Substitute common OCR errors in header names
        normalized = normalized.replace("0", "O")
                              .replace("1", "I")
                              .replace("5", "S")
                              .replace("8", "B");
        
        return normalized;
    }
}