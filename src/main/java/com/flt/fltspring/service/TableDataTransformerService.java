package com.flt.fltspring.service;

import com.flt.fltspring.config.ColumnConfig;
import com.flt.fltspring.model.TableRow;
import com.flt.fltspring.service.transform.AirportCodeTransformer;
import com.flt.fltspring.service.transform.ContextValidator;
import com.flt.fltspring.service.transform.HeaderMatcher;
import com.flt.fltspring.service.transform.TextTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableDataTransformerService {

    private final ColumnConfig[] columnConfigs;
    private final TextTransformer textTransformer;
    private final AirportCodeTransformer airportCodeTransformer;
    private final HeaderMatcher headerMatcher;
    private final ContextValidator contextValidator;

<<<<<<< Updated upstream
    public TableDataTransformerService(ColumnConfig[] columnConfigs) {
        this.columnConfigs = columnConfigs;
    }

    // Characters commonly misidentified as numbers
    private static final Map<String, String> NUMERIC_REPLACEMENTS = new HashMap<>() {{
        // Letter to number substitutions
        put("O", "0");
        put("o", "0");
        put("l", "1");
        put("I", "1");
        put("i", "1");
        put("/", "1");
        put("\\", "1");
        put("S", "5");
        put("s", "5");
        put("Z", "2");
        put("z", "2");
        put("b", "6");
        put("B", "8");
        put("G", "6");
        put("g", "9");
        put("q", "9");
        // Common OCR errors with special characters
        put(".", "");  // Sometimes periods appear in numbers
        put(" ", "");  // Remove spaces in numbers
        put(",", "");  // Remove commas that might appear in larger numbers
        put("-", "");  // Remove hyphens that might break up numbers
    }};

    // Characters commonly misidentified as letters
    private static final Map<String, String> AIRPORT_CODE_REPLACEMENTS = new HashMap<>() {{
        put("0", "O");
        put("1", "I");
        put("2", "Z");
        put("5", "S");
        put("8", "B");
        put("6", "G");
        put("9", "G");
        put(" ", "");  // Remove spaces in airport codes
        put("-", "");  // Remove hyphens
        put(".", "");  // Remove periods
    }};

    // Common airport code patterns - standard 3-letter IATA or 4-letter ICAO
    private static final Pattern AIRPORT_CODE_PATTERN = Pattern.compile("^[A-Z]{3,4}$");
    
    // Common expected headers for flight logbooks
    private static final List<String> COMMON_FLIGHT_HEADERS = List.of(
        "DATE", "FROM", "TO", "AIRCRAFT", "REGISTRATION", "AIRCRAFT TYPE", "REG",
        "PIC", "SIC", "DUAL GIVEN", "DUAL RECEIVED", "GROUND", "SIMULATOR",
        "TOTAL", "DAY", "NIGHT", "ACTUAL INSTRUMENT", "SIMULATED INSTRUMENT", 
        "CROSS COUNTRY", "NUMBER OF LANDINGS", "APPROACHES", "REMARKS"
    );
=======
    /**
     * Transform table data by cleaning and converting values based on 
     * column types and domain-specific rules for flight logbooks
     */
>>>>>>> Stashed changes

    public List<TableRow> transformData(List<TableRow> rows) {
        if (rows.isEmpty()) {
            return rows;
        }

        // The first row should be our header row with properly duplicated headers
        TableRow headerRow = rows.get(0);

        // Build type map from the headers we have
        Map<Integer, String> columnTypes = buildColumnTypeMap(headerRow);

        // Transform only the data rows, preserve header row
        List<TableRow> transformedRows = new ArrayList<>();
        transformedRows.add(headerRow);  // Keep header row as-is

        // Transform subsequent rows
        for (int i = 1; i < rows.size(); i++) {
            transformedRows.add(transformRow(rows.get(i), columnTypes));
        }

        return transformedRows;
    }

    private Map<Integer, String> buildColumnTypeMap(TableRow headerRow) {
        Map<Integer, String> typeMap = new HashMap<>();

        // For each header, including duplicated ones
        headerRow.getColumnData().forEach((index, headerValue) -> {
            // Clean and normalize the header first
<<<<<<< Updated upstream
            String normalizedHeader = normalizeHeaderValue(headerValue);
=======
            String normalizedHeader = textTransformer.normalizeHeaderValue(headerValue);
>>>>>>> Stashed changes
            
            // First try exact match with normalized header
            for (ColumnConfig config : columnConfigs) {
                if (normalizedHeader.equals(config.getFieldName()) || 
                    headerValue.equals(config.getFieldName())) {
                    typeMap.put(index, config.getType());
                    log.info("Mapped column {} ({}) to type {} [exact match]",
                            index, headerValue, config.getType());
                    return;  // Found exact match, no need to continue
                }
            }

            // Try fuzzy matching with common flight logbook headers
<<<<<<< Updated upstream
            String matchedCommonHeader = findClosestMatch(normalizedHeader, COMMON_FLIGHT_HEADERS);
=======
            String matchedCommonHeader = headerMatcher.findClosestMatch(normalizedHeader, null);
>>>>>>> Stashed changes
            if (matchedCommonHeader != null) {
                // If we matched a common header, look up its type in our configs
                for (ColumnConfig config : columnConfigs) {
                    if (matchedCommonHeader.equals(config.getFieldName())) {
                        typeMap.put(index, config.getType());
                        log.info("Mapped column {} ({}) to type {} [fuzzy match with {}]",
                                index, headerValue, config.getType(), matchedCommonHeader);
                        return;
                    }
                }
            }

            // If no exact match, find the best partial match
            String bestMatch = null;
            String bestType = "STRING";  // Default to STRING if no match found
            int bestMatchLength = 0;

            for (ColumnConfig config : columnConfigs) {
                // Try to match with normalized header first
                boolean normalizedContains = normalizedHeader.contains(config.getFieldName());
                boolean originalContains = headerValue.contains(config.getFieldName());
                
                // If header contains config field name and it's longer than our current best match
                if ((normalizedContains || originalContains) &&
                        config.getFieldName().length() > bestMatchLength) {
                    bestMatch = config.getFieldName();
                    bestType = config.getType();
                    bestMatchLength = config.getFieldName().length();
                }
            }

            if (bestMatch != null) {
                typeMap.put(index, bestType);
                log.info("Mapped column {} ({}) to type {} [partial match with {}]",
                        index, headerValue, bestType, bestMatch);
            } else {
                // Type inference based on content patterns
<<<<<<< Updated upstream
                if (inferAirportCodeType(headerValue)) {
                    typeMap.put(index, "AIRPORT_CODE");
                    log.info("Mapped column {} ({}) to AIRPORT_CODE [inferred from name]", 
                            index, headerValue);
                } else if (inferNumericType(headerValue)) {
=======
                if (headerMatcher.inferAirportCodeType(headerValue)) {
                    typeMap.put(index, "AIRPORT_CODE");
                    log.info("Mapped column {} ({}) to AIRPORT_CODE [inferred from name]", 
                            index, headerValue);
                } else if (headerMatcher.inferNumericType(headerValue)) {
>>>>>>> Stashed changes
                    typeMap.put(index, "INTEGER");
                    log.info("Mapped column {} ({}) to INTEGER [inferred from name]", 
                            index, headerValue);
                } else {
                    log.warn("No match found for header {}, defaulting to STRING", headerValue);
                    typeMap.put(index, "STRING");
                }
            }
        });

        return typeMap;
    }
    
    /**
     * Normalize a header value by removing special characters, converting to uppercase,
     * and applying common substitutions for OCR errors
     */
    private String normalizeHeaderValue(String headerValue) {
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
    
    /**
     * Find the closest matching header from a list of common headers
     * using a simple similarity metric
     */
    private String findClosestMatch(String headerValue, List<String> commonHeaders) {
        if (headerValue == null || headerValue.isEmpty()) {
            return null;
        }
        
        // Direct match check first
        for (String commonHeader : commonHeaders) {
            if (headerValue.equals(commonHeader)) {
                return commonHeader;
            }
        }
        
        String best = null;
        int bestScore = 0;
        
        for (String commonHeader : commonHeaders) {
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
    private boolean inferAirportCodeType(String headerValue) {
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
    private boolean inferNumericType(String headerValue) {
        if (headerValue == null) return false;
        
        String normalized = headerValue.toUpperCase();
        return normalized.contains("TOTAL") || 
               normalized.contains("NUMBER") || 
               normalized.contains("TIME") || 
               normalized.contains("HOUR") || 
               normalized.contains("COUNT") || 
               normalized.contains("LANDINGS");
    }

    private TableRow transformRow(TableRow row, Map<Integer, String> columnTypes) {
        if (row.isHeader()) {
            return row;
        }

        Map<Integer, String> transformedData = new HashMap<>();
        Map<Integer, String> originalData = row.getColumnData();
        
        // First pass: do standard transformations on all fields
        originalData.forEach((index, value) -> {
            String type = columnTypes.getOrDefault(index, "STRING");
            String transformedValue = transformValue(value, type);
            transformedData.put(index, transformedValue);
            
            if (log.isDebugEnabled() && !value.equals(transformedValue)) {
                log.debug("Transformed value at index {} from '{}' to '{}' using type {}",
                        index, value, transformedValue, type);
            }
        });
        
        // Second pass: context-aware transformations for special cases
<<<<<<< Updated upstream
        transformSpecialCases(transformedData, columnTypes);
=======
        contextValidator.validateRowContext(transformedData, columnTypes);
>>>>>>> Stashed changes

        // Preserve parent headers from original row
        return TableRow.builder()
            .rowIndex(row.getRowIndex())
            .columnData(transformedData)
            .isHeader(false)
            .parentHeaders(row.getParentHeaders())
            .build();
    }
    
    /**
     * Apply context-aware transformations based on relationships
     * between different columns (e.g., FROM/TO airport codes,
     * time format consistency, etc.)
     */
    private void transformSpecialCases(Map<Integer, String> rowData, Map<Integer, String> columnTypes) {
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
                        if (isLikelyTimeField(index.toString()) && numericValue > 2400) {
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
    
    /**
     * Check if a column is likely to contain time data
     */
    private boolean isLikelyTimeField(String columnName) {
        String normalized = columnName.toUpperCase();
        return normalized.contains("TIME") || 
               normalized.contains("HOUR") || 
               normalized.contains("DURATION");
    }

    private String transformValue(String value, String columnType) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        value = value.trim();

        switch (columnType) {
            case "INTEGER":
                return textTransformer.transformInteger(value);
            case "AIRPORT_CODE":
                return airportCodeTransformer.transformAirportCode(value);
            case "STRING":
            default:
                return value;
        }
    }
<<<<<<< Updated upstream

    private String transformInteger(String value) {
        return transformWithReplacements(value, NUMERIC_REPLACEMENTS, "[^0-9]", "0");
    }

    private String transformAirportCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        
        // First convert to uppercase and trim
        String transformed = value.toUpperCase().trim();
        
        // Apply replacements
        transformed = transformWithReplacements(transformed, AIRPORT_CODE_REPLACEMENTS, null, null);
        
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
    
    /**
     * Applies character replacements and filtering to transform values
     * @param value Original string value
     * @param replacements Map of character replacements to apply
     * @param filterPattern Regular expression pattern to filter out characters (can be null)
     * @param defaultValue Default value if result is empty (can be null)
     * @return Transformed string
     */
    private String transformWithReplacements(String value, Map<String, String> replacements, 
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
=======
>>>>>>> Stashed changes
}