package com.flt.fltspring.service;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.flt.fltspring.model.DocumentTableCellAdapter;
import com.flt.fltspring.model.TableStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultConverterService {
    public List<TableStructure> convertToTable(final AnalyzeResult analyzeResult) {
        if (analyzeResult.getTables() == null) {
            log.warn("No tables found in analyze result");
            return new ArrayList<>();
        }

        return analyzeResult.getTables().stream()
                .map(table -> new TableStructure(
                        table.getColumnCount(),
                        table.getCells().stream()
                                .map(DocumentTableCellAdapter::new)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}