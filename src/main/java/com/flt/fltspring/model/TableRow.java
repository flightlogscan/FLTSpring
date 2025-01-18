package com.flt.fltspring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

// Business Objects
@Data
@Builder
@AllArgsConstructor
public class TableRow {  // Business logic object
    int rowIndex;
    Map<Integer, String> columnData;
    boolean isHeader;
}
