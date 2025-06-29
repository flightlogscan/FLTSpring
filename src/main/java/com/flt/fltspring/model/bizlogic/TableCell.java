package com.flt.fltspring.model.bizlogic;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = DefaultTableCell.class)
public interface TableCell {
    int getRowIndex();
    int getColumnIndex();
    String getContent();
    default int getColumnSpan() {
        return 1;
    }
}