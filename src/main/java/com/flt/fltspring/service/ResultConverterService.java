package com.flt.fltspring.service;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.flt.fltspring.model.bizlogic.TableStructure;
import com.flt.fltspring.model.dependency.DocumentTableCellAdapter;
import org.springframework.stereotype.Service;

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
                .map(azureTable -> new TableStructure(
                        azureTable.getColumnCount(),
                        azureTable.getCells().stream()
                                .map(DocumentTableCellAdapter::new)
                                .collect(Collectors.toList()),
                        // wthdyjstm??
                        azureTable.getBoundingRegions() != null && !azureTable.getBoundingRegions().isEmpty()
                                ? azureTable.getBoundingRegions().getFirst().getPageNumber()
                                : 0
                ))
                .collect(Collectors.toList());
    }
}