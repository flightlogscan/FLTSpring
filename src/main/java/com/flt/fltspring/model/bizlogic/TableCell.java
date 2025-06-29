package com.flt.fltspring.model.bizlogic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableCell {
    private String content;
    private int rowIndex;
    private int columnIndex;
    private int columnSpan = 1;
}