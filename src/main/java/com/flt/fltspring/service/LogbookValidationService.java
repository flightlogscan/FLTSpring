package com.flt.fltspring.service;

import com.flt.fltspring.model.TableRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LogbookValidationService {
    private Map<String, String> headerMap;

    @Autowired
    public void init() {

        this.headerMap = new HashMap<>();
        headerMap.put("single- engine land", "SINGLE-ENGINE LAND");
        headerMap.put("multi- engine land", "MULTI-ENGINE LAND");

        //Potential to mis-map single to multi or vice versa here. Not sure on solution yet.
        headerMap.put("engine land", "SINGLE-ENGINE LAND");
    }

    public List<TableRow> validateAndCorrect(List<TableRow> scannedRows) {
        log.info("Starting validation for {} rows", scannedRows.size());

        TableRow headerRow = scannedRows.stream()
                .filter(TableRow::isHeader)
                .findFirst()
                .orElse(null);

        final TableRow finalHeaderRow = headerRow;
        final List<TableRow> dataRows = scannedRows.stream()
                .filter(row -> !row.equals(finalHeaderRow))
                .map(row -> new TableRow(row.getRowIndex(), row.getColumnData(), false))
                .toList();

        if (headerRow != null) {
            Map<Integer, String> consolidatedHeaders = new HashMap<>();
            headerRow.getColumnData().forEach((column, value) -> {
                String normalized = value.toLowerCase().trim();
                String canonicalHeader = headerMap.getOrDefault(normalized, value);
                consolidatedHeaders.put(column, canonicalHeader);
            });

            headerRow = new TableRow(0, consolidatedHeaders, true);
        }

        List<TableRow> result = new ArrayList<>();
        if (headerRow != null) {
            result.add(headerRow);
        }
        result.addAll(dataRows);

        return result;
    }
}