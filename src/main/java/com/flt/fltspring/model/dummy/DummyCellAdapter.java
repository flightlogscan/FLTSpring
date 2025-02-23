package com.flt.fltspring.model.dummy;

import com.flt.fltspring.model.TableCell;

public class DummyCellAdapter implements TableCell {
    private final DummyCell cell;

    public DummyCellAdapter(DummyCell cell) {
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
