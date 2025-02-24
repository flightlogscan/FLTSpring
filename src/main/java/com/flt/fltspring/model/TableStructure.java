package com.flt.fltspring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@Getter
public class TableStructure {
    private final int columnCount;
    private final List<TableCell> cells;
}
