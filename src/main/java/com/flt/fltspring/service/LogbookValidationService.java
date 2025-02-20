package com.flt.fltspring.service;

import com.flt.fltspring.model.LogbookType;
import com.flt.fltspring.model.TableRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LogbookValidationService {
    private Map<String, String> headerMap;

    @Autowired
    public void init() {

        this.headerMap = new HashMap<>();
        headerMap.put("engine land", "SINGLE-ENGINE LAND");
        headerMap.put("multi- engine land", "MULTI-ENGINE LAND");
        headerMap.put("date", "DATE");
        headerMap.put("aircraft type", "AIRCRAFT TYPE");
        headerMap.put("aircraft ident", "AIRCRAFT IDENT");
        headerMap.put("from", "FROM");
        headerMap.put("to", "TO");
        headerMap.put("nr inst. app.", "NR INST. APP.");
        headerMap.put("remarks and endorsements", "REMARKS AND ENDORSEMENTS");
        headerMap.put("nr t/o", "NR T/O");
        headerMap.put("nr ldg", "NR LDG");

    }

    public List<TableRow> validateAndCorrect(List<TableRow> scannedRows, LogbookType type) {
        log.info("Starting validation for {} rows with type {}", scannedRows.size(), type);

        TableRow headerRow = scannedRows.stream()
                .filter(TableRow::isHeader)
                .findFirst()
                .orElse(null);

        final TableRow finalHeaderRow = headerRow;
        final List<TableRow> dataRows = scannedRows.stream()
                .filter(row -> !row.equals(finalHeaderRow))
                .map(row -> new TableRow(row.getRowIndex(), row.getColumnData(), false))
                .collect(Collectors.toList());

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