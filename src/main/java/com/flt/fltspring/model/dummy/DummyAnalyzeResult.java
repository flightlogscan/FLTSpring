package com.flt.fltspring.model.dummy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DummyAnalyzeResult {
    private List<DummyTable> tables;
}
