package com.flt.fltspring.service;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.DocumentTable;
import com.flt.fltspring.model.bizlogic.TableCell;
import com.flt.fltspring.model.bizlogic.TableStructure;
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

        final List<TableCell> adaptedCells = sourceTable.getCells().stream()
                .map(cell -> new TableCell() {
                    @Override public int getRowIndex()    { return cell.getRowIndex(); }
                    @Override public int getColumnIndex() { return cell.getColumnIndex(); }
                    @Override public String getContent()  { return cell.getContent(); }
                    @Override public int getColumnSpan()  { return cell.getColumnSpan() != null ?
                            cell.getColumnSpan() : 1; }
                })
                .collect(Collectors.toList());

        final int pageNumber = sourceTable.getBoundingRegions() != null && !sourceTable.getBoundingRegions().isEmpty()
                ? sourceTable.getBoundingRegions().getFirst().getPageNumber() : 0;

        return new TableStructure(columnCount, adaptedCells, pageNumber);
    }
}