package com.flt.fltspring.model.bizlogic;

public interface TableCell {
    int getRowIndex();
    int getColumnIndex();
    String getContent();
    default int getColumnSpan() {
        return 1;
    }
}
