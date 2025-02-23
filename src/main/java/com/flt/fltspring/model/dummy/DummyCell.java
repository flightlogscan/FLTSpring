package com.flt.fltspring.model.dummy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DummyCell {
    private String kind;
    private int rowIndex;
    private int columnIndex;
    private String content;
}
