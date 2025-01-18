package com.flt.fltspring.model;

import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.DocumentTable;
import com.azure.ai.documentintelligence.models.DocumentTableCell;
import com.azure.ai.documentintelligence.models.DocumentTableCellKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentAnalysisService {
    private final LogbookValidationService validationService;
    private final TableDataTransformer transformer;

    public TableResponseDTO analyzeDocument(AnalyzeResult analyzeResult, LogbookType logbookType) {
        // Convert Azure result to TableRows
        List<TableRow> tableRows = convertToTableRows(analyzeResult);

        // Transform data (clean/normalize)
        List<TableRow> transformedRows = transformer.transformData(tableRows);

        // Validate and correct using template
        List<TableRow> validatedRows = validationService.validateAndCorrect(transformedRows, logbookType);

        // Convert to API response DTO
        return mapToResponse(validatedRows);
    }

    private List<TableRow> convertToTableRows(AnalyzeResult analyzeResult) {
        final List<TableRow> tableRows = new ArrayList<>();
        final Map<Integer, Map<Integer, String>> rowData = new HashMap<>();
        final Map<Integer, Boolean> headerRows = new HashMap<>();

        final List<DocumentTable> documentTables = analyzeResult.getTables();
        for (final DocumentTable documentTable : documentTables) {
            for (final DocumentTableCell documentTableCell : documentTable.getCells()) {
                int rowIndex = documentTableCell.getRowIndex();
                int columnIndex = documentTableCell.getColumnIndex();
                String content = documentTableCell.getContent();
                boolean isHeader = DocumentTableCellKind.COLUMN_HEADER.equals(documentTableCell.getKind());

                rowData.putIfAbsent(rowIndex, new HashMap<>());
                rowData.get(rowIndex).put(columnIndex, content);

                if (isHeader) {
                    headerRows.put(rowIndex, true);
                }
            }

            rowData.forEach((rowIndex, columnData) -> {
                TableRow tableRow = new TableRow(
                        rowIndex,
                        columnData,
                        headerRows.getOrDefault(rowIndex, false)
                );
                tableRows.add(tableRow);
            });
        }
        return tableRows;
    }

    private TableResponseDTO mapToResponse(List<TableRow> rows) {
        TableResponseDTO response = new TableResponseDTO();
        response.setRows(rows.stream()
                .map(this::convertToRowDTO)
                .collect(Collectors.toList()));
        return response;
    }

    private RowDTO convertToRowDTO(TableRow row) {
        RowDTO dto = new RowDTO();
        dto.setRowIndex(row.getRowIndex());
        dto.setContent(row.getColumnData());
        dto.setHeader(row.isHeader());
        return dto;
    }
}