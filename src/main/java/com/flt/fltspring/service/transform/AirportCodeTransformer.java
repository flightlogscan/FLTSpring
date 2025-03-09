package com.flt.fltspring.service.transform;

import org.springframework.stereotype.Component;

/**
 * Transformer for handling airport code input.
 */
@Component
public class AirportCodeTransformer {

    /**
     * Transform airport code input by converting to uppercase and removing non-letter characters.
     * Preserves original separators between codes if desired.
     * @param value The value to transform
     * @return The transformed airport code string
     */
    public String transformAirportCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        String transformed = value.toUpperCase().trim();

        // Remove all non-letter characters (allowing only A-Z)
        transformed = transformed.replaceAll("[^A-Z]", "");

        return transformed;
    }
}