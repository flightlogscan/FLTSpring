package com.flt.fltspring.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

// Template for logbook headers and validation
@Data
public class LogbookTemplate {
    private LogbookType type;
    private Map<Integer, HeaderDefinition> expectedHeaders;

    @Data
    @AllArgsConstructor
    public static class HeaderDefinition {
        private String expectedName;
        private DataType dataType;
        private boolean required;
    }

    public enum DataType {
        DATE, TEXT
    }
}
