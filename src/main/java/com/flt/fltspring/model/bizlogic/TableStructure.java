package com.flt.fltspring.model.bizlogic;

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
    private final int pageNumber;
    
    public TableStructure(int columnCount, List<TableCell> cells) {
        this(columnCount, cells, 0);
    }
}
