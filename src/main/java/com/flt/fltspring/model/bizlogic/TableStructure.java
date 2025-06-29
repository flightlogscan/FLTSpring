package com.flt.fltspring.model.bizlogic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TableStructure {
    private int columnCount;
    private List<TableCell> cells;
    private int pageNumber;
}
