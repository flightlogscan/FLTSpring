package com.flt.fltspring.service.transform;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for character replacements used in various transformations.
 * This centralizes all the character replacement mappings.
 */
@Component
public class CharacterReplacementConfig {
    
    // Characters commonly misidentified as numbers
    private final Map<String, String> numericReplacements = new HashMap<>();
    
    // Characters commonly misidentified as letters
    private final Map<String, String> airportCodeReplacements = new HashMap<>();
    
    @PostConstruct
    public void initialize() {
        // Initialize numeric replacements
        numericReplacements.put("O", "0");
        numericReplacements.put("o", "0");
        numericReplacements.put("l", "1");
        numericReplacements.put("I", "1");
        numericReplacements.put("i", "1");
        numericReplacements.put("/", "1");
        numericReplacements.put("\\", "1");
        numericReplacements.put("S", "5");
        numericReplacements.put("s", "5");
        numericReplacements.put("Z", "2");
        numericReplacements.put("z", "2");
        numericReplacements.put("b", "6");
        numericReplacements.put("B", "8");
        numericReplacements.put("G", "6");
        numericReplacements.put("g", "9");
        numericReplacements.put("q", "9");
        // Common OCR errors with special characters
        numericReplacements.put(".", "");  // Sometimes periods appear in numbers
        numericReplacements.put(" ", "");  // Remove spaces in numbers
        numericReplacements.put(",", "");  // Remove commas that might appear in larger numbers
        numericReplacements.put("-", "");  // Remove hyphens that might break up numbers
        
        // Initialize airport code replacements
        airportCodeReplacements.put("0", "O");
        airportCodeReplacements.put("1", "I");
        airportCodeReplacements.put("2", "Z");
        airportCodeReplacements.put("5", "S");
        airportCodeReplacements.put("8", "B");
        airportCodeReplacements.put("6", "G");
        airportCodeReplacements.put("9", "G");
        airportCodeReplacements.put(" ", "");  // Remove spaces in airport codes
        airportCodeReplacements.put("-", "");  // Remove hyphens
        airportCodeReplacements.put(".", "");  // Remove periods
    }
    
    public Map<String, String> getNumericReplacements() {
        return numericReplacements;
    }
    
    public Map<String, String> getAirportCodeReplacements() {
        return airportCodeReplacements;
    }
}