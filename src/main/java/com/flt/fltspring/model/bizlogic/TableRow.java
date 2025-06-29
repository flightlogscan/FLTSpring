package com.flt.fltspring.model.bizlogic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableRow {
    int rowIndex;
    Map<Integer, String> columnData;
    boolean isHeader;
    @Builder.Default
    Map<Integer, String> parentHeaders = new HashMap<>();
}
