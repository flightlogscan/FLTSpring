package com.flt.fltspring;

import com.flt.fltspring.model.AnalyzeImageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ImageAnalyzerRestController {

    @RequestMapping(method = RequestMethod.POST, path = "/api/analyze")
    public ResponseEntity<String> submitAnalyzeImage(final HttpServletRequest request) {
        // TODO: Call Azure and get real result ID
        final String resultId = "lanceResultId";

        // This result ID is used to poll for results
        return ResponseEntity.ok(resultId);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/api/analyze/results/{resultId}")
    public ResponseEntity<AnalyzeImageResponse> getAnalysisResults(@PathVariable final String resultId) {
        // TODO: Call Azure and get real result status and data
        final String status = "RUNNING";
        final String rawResults = "{}";

        final AnalyzeImageResponse response = AnalyzeImageResponse.builder()
                                                                  .status(status)
                                                                  .rawResults(rawResults)
                                                                  .build();
;
        // This result ID is used to poll for results
        return ResponseEntity.ok(response);
    }

}
