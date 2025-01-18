package com.flt.fltspring.model;

import lombok.Data;

import java.util.Map;

@Data
public class RowDTO {
    private int rowIndex;
    private Map<Integer, String> content;
    private boolean isHeader;
}
