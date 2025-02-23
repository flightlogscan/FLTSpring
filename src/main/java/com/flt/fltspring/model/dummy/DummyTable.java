package com.flt.fltspring.model.dummy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DummyTable {
    private int rowCount;
    private int columnCount;
    private List<DummyCell> cells;
}
