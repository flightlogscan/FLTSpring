package com.flt.fltspring.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ColumnConfig {
    private final String fieldName;
    private final ColumnType type;
}