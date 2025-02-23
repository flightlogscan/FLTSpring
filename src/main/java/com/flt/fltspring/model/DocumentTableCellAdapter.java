package com.flt.fltspring.model;

import com.azure.ai.documentintelligence.models.DocumentTableCell;

public class DocumentTableCellAdapter implements TableCell {
    private final DocumentTableCell cell;

    public DocumentTableCellAdapter(DocumentTableCell cell) {
        this.cell = cell;
    }

    @Override
    public int getRowIndex() {
        return cell.getRowIndex();
    }

    @Override
    public int getColumnIndex() {
        return cell.getColumnIndex();
    }

    @Override
    public String getContent() {
        return cell.getContent();
    }

    @Override
    public String toString() {
        return String.format("TableCell(rowIndex=%d, columnIndex=%d, content='%s')",
                getRowIndex(), getColumnIndex(), getContent());
    }
}
