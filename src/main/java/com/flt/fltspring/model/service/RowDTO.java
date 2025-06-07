package com.flt.fltspring.model.service;

import java.util.Map;

public record RowDTO(
    int rowIndex,
    Map<Integer, String> content,
    Map<Integer, String> parentHeaders,
    boolean header
) {}
