// src/main/java/com/flt/fltspring/model/bizlogic/DefaultTableCell.java
package com.flt.fltspring.model.bizlogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Jackson-friendly implementation of the TableCell interface.
 */
@Data
@NoArgsConstructor
public class DefaultTableCell implements TableCell {
    private String content;
    private int rowIndex;
    private int columnIndex;
    private int columnSpan;
    @JsonCreator
    public DefaultTableCell(
            @JsonProperty("content") String content,
            @JsonProperty("rowIndex") int rowIndex,
            @JsonProperty("columnIndex") int columnIndex,
            @JsonProperty("columnSpan") int columnSpan
    ) {
        this.content = content;
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.columnSpan = columnSpan;
    }
}