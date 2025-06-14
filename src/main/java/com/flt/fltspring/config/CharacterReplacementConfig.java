package com.flt.fltspring.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized OCR character-replacement mappings.
 * Supports numeric, airport-code, and generic string replacements.
 */
@Getter
@Component
public class CharacterReplacementConfig {

    // Characters commonly misidentified as numbers
    private final Map<String, String> numericReplacements = new HashMap<>();
    // Characters commonly misidentified as letters in airport codes
    private final Map<String, String> airportCodeReplacements = new HashMap<>();
    // Generic string replacements (e.g. strip or correct common OCR artifacts in text)
    private final Map<String, String> stringReplacements = new HashMap<>();
    // Dates are tricky because they might just be numbers ("6/9") with a slash or free text ("June 9th")
    private final Map<String, String> dateReplacements = new HashMap<>();

    @PostConstruct
    public void initialize() {
        // Numeric replacements
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
        numericReplacements.put(" ", "");
        numericReplacements.put(",", "");
        numericReplacements.put("-", "");

        // Airport-code replacements
        airportCodeReplacements.put("0", "O");
        airportCodeReplacements.put("1", "I");
        airportCodeReplacements.put("2", "Z");
        airportCodeReplacements.put("5", "S");
        airportCodeReplacements.put("8", "B");
        airportCodeReplacements.put("6", "G");
        airportCodeReplacements.put("9", "G");
        airportCodeReplacements.put(" ", "");
        airportCodeReplacements.put("-", "");
        airportCodeReplacements.put(".", "");

        // Generic string replacements: same as airport codes but keep spaces
        stringReplacements.remove(" ");
        stringReplacements.remove("-");
    }
}