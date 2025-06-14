package com.flt.fltspring.service;

import com.flt.fltspring.model.bizlogic.TableCell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableHeaderExtractor {

    public static Map<Integer, String> extractHeaders(
            List<Integer> headerRows,
            Map<Integer, List<TableCell>> rowsByIndex,
            int offset
    ) {
        Map<Integer, String> headers = new HashMap<>();

        for (int i = 0; i < headerRows.size(); i++) {
            for (TableCell cell : rowsByIndex.get(headerRows.get(i))) {
                if (cell.getContent() == null || cell.getContent().isBlank()) continue;
                String text = TableUtils.clean(cell.getContent());
                int startCol = cell.getColumnIndex() + offset;
                for (int span = 0; span < cell.getColumnSpan(); span++) {
                    headers.put(startCol + span, text);
                }
            }
        }

        return headers;
    }
}