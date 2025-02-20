package com.flt.fltspring.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnConfig {
    private String fieldName;
    private String type;

    public boolean isIntegerType() {
        return "INTEGER".equals(type);
    }

    public boolean isAirportCodeType() {
        return "AIRPORT_CODE".equals(type);
    }
}