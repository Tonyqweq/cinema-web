package org.tonyqwe.cinemaweb.domain.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class MovieImportResult {

    private int successCount;
    private int failCount;
    private List<String> errors = new ArrayList<>();
}
