package com.flt.fltspring.service;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import org.springframework.stereotype.Service;
import com.flt.fltspring.model.DocumentTableCellAdapter;
import com.flt.fltspring.model.TableStructure;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResultConverterService {

    public List<TableStructure> convertToTable(final AnalyzeResult analyzeResult) {
        if (analyzeResult.getTables() == null) {
            return Collections.emptyList();
        }
        return analyzeResult.getTables().stream()
                .map(table -> new TableStructure(
                        table.getColumnCount(),
                        table.getCells().stream()
                                .map(DocumentTableCellAdapter::new)
                                .collect(Collectors.toList()),
                        table.getBoundingRegions() != null && !table.getBoundingRegions().isEmpty()
                                ? table.getBoundingRegions().get(0).getPageNumber()
                                : 0
                ))
                .collect(Collectors.toList());
    }
}