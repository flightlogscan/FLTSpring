package com.flt.fltspring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogbookConfiguration {

    @Bean
    public ColumnConfig[] columnConfigs() {
        return new ColumnConfig[] {
                new ColumnConfig("DATE", "STRING"),
                new ColumnConfig("AIRCRAFT TYPE", "STRING"),
                new ColumnConfig("AIRCRAFT IDENT", "STRING"),
                new ColumnConfig("FROM", "AIRPORT_CODE"),
                new ColumnConfig("TO", "AIRPORT_CODE"),
                new ColumnConfig("NR INST. APP.", "INTEGER"),
                new ColumnConfig("REMARKS AND ENDORSEMENTS", "STRING"),
                new ColumnConfig("NR T/O", "INTEGER"),
                new ColumnConfig("NR LDG", "INTEGER"),
                new ColumnConfig("SINGLE-ENGINE LAND", "INTEGER"),
                new ColumnConfig("MULTI-ENGINE LAND", "INTEGER"),
                new ColumnConfig("INT APR", "STRING"),
                new ColumnConfig("INST APR", "STRING"),
                new ColumnConfig("NIGHT", "INTEGER"),
                new ColumnConfig("ACTUAL INSTRUMENT", "INTEGER"),
                new ColumnConfig("SIMULATED INSTRUMENT (HOOD)", "INTEGER"),
                new ColumnConfig("FLIGHT SIMULATOR", "INTEGER"),
                new ColumnConfig("CROSS COUNTRY", "INTEGER"),
                new ColumnConfig("AS FLIGHT INSTRUCTOR", "INTEGER"),
                new ColumnConfig("DUAL RECEIVED", "INTEGER"),
                new ColumnConfig("PILOT IN COMMAND (INCL. SOLO)", "INTEGER"),
                new ColumnConfig("TOTAL DURATION OF FLIGHT", "INTEGER")
        };
    }
}