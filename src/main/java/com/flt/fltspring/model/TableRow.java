package com.flt.fltspring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
@Data
@Builder
@AllArgsConstructor
public class TableRow {
    int rowIndex;
    Map<Integer, String> columnData;
    boolean isHeader;
}
