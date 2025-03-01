package com.flt.fltspring.service.transform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles context-aware validation and correction across different fields
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ContextValidator {

    private final HeaderMatcher headerMatcher;
    
    /**
     * Apply context-aware transformations based on relationships
     * between different columns (e.g., FROM/TO airport codes,
     * time format consistency, etc.)
     */
    public void validateRowContext(Map<Integer, String> rowData, Map<Integer, String> columnTypes) {
        validateAirportCodes(rowData, columnTypes);
        validateTimeValues(rowData, columnTypes);
    }
    
    /**
     * Validate and normalize FROM/TO airport codes
     */
    private void validateAirportCodes(Map<Integer, String> rowData, Map<Integer, String> columnTypes) {
        // Find FROM and TO columns for airport code cross-referencing
        Integer fromIndex = null;
        Integer toIndex = null;
        
        for (Map.Entry<Integer, String> entry : columnTypes.entrySet()) {
            if (!"AIRPORT_CODE".equals(entry.getValue())) {
                continue;
            }
            
            // Check if this is a FROM or TO column
            String columnName = entry.getKey().toString();
            if (columnName.toUpperCase().contains("FROM")) {
                fromIndex = entry.getKey();
            } else if (columnName.toUpperCase().contains("TO")) {
                toIndex = entry.getKey();
            }
        }
        
        // If we found both FROM and TO columns, perform validation
        // but be careful not to modify multi-airport entries
        if (fromIndex != null && toIndex != null) {
            String fromCode = rowData.get(fromIndex);
            String toCode = rowData.get(toIndex);
            
            // Skip validation for entries that contain multiple codes
            boolean isMultiFrom = fromCode != null && (fromCode.contains("-") || fromCode.length() > 4);
            boolean isMultiTo = toCode != null && (toCode.contains("-") || toCode.length() > 4);
            
            // Only validate simple single-code entries
            if (!isMultiFrom && !isMultiTo && 
                fromCode != null && toCode != null && 
                !fromCode.isEmpty() && !toCode.isEmpty() && 
                fromCode.equals(toCode)) {
                
                // Log a warning but don't change anything
                log.warn("FROM and TO airport codes are identical: {}", fromCode);
            }
        }
    }
    
    /**
     * Validate and normalize time field values
     */
    private void validateTimeValues(Map<Integer, String> rowData, Map<Integer, String> columnTypes) {
        // Time field normalization - ensure time fields use consistent formats
        // and values make sense (e.g., flight time doesn't exceed 24 hours)
        for (Map.Entry<Integer, String> entry : columnTypes.entrySet()) {
            if ("INTEGER".equals(entry.getValue())) {
                Integer index = entry.getKey();
                String value = rowData.get(index);
                
                if (value != null && !value.isEmpty()) {
                    try {
                        // For numbers that appear to be times, ensure they're reasonable
                        int numericValue = Integer.parseInt(value);
                        if (headerMatcher.isLikelyTimeField(index.toString()) && numericValue > 2400) {
                            // Probably a malformed time - truncate to a reasonable value
                            String corrected = String.valueOf(Math.min(numericValue, 2400));
                            rowData.put(index, corrected);
                            log.info("Corrected likely time value from {} to {}", value, corrected);
                        }
                    } catch (NumberFormatException e) {
                        // Not a number - leave as is
                    }
                }
            }
        }
    }
}