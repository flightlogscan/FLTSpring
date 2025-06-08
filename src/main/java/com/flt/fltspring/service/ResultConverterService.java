package com.flt.fltspring.service;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.DocumentTable;
import com.flt.fltspring.model.bizlogic.TableCell;
import com.flt.fltspring.model.bizlogic.TableStructure;
import com.flt.fltspring.model.dependency.DocumentTableCellAdapter;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResultConverterService {

    /**
     * Convert Azure AnalyzeResult into application-specific TableStructure objects.
     */
    public List<TableStructure> convertToTable(final AnalyzeResult analyzeResult) {
        List<DocumentTable> detectedTables = analyzeResult.getTables();

        if (detectedTables == null || detectedTables.isEmpty()) {
            return Collections.emptyList();
        }

        return detectedTables.stream()
                .map(this::convertSingleTable)
                .collect(Collectors.toList());
    }

    /**
     * Handles conversion of a single Azure DocumentTable into a TableStructure.
     */
    private TableStructure convertSingleTable(DocumentTable sourceTable) {
        int columnCount = sourceTable.getColumnCount();

        // Adapt each DocumentTableCell into our TableCell model
        List<TableCell> adaptedCells = sourceTable.getCells().stream()
                .map(DocumentTableCellAdapter::new)
                .collect(Collectors.toList());

        // Determine page number from bounding regions, defaulting to 0
        int pageNumber = extractPageNumber(sourceTable);

        return new TableStructure(columnCount, adaptedCells, pageNumber);
    }

    /**
     * Safely extracts the page number from the table's bounding regions.
     * @return page number of first region, or 0 if none available.
     */
    private int extractPageNumber(DocumentTable table) {
        if (table.getBoundingRegions() != null && !table.getBoundingRegions().isEmpty()) {
            return table.getBoundingRegions().getFirst().getPageNumber();
        }
        return 0;
    }
}