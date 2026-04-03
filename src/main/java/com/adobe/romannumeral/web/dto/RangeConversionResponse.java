package com.adobe.romannumeral.web.dto;

import com.adobe.romannumeral.domain.model.RomanNumeralResult;

import java.util.List;

/**
 * DTO for range conversion HTTP responses.
 *
 * <p>Matches the assessment-required JSON format exactly:
 * <pre>{@code
 * {
 *   "conversions": [
 *     {"input": "1", "output": "I"},
 *     {"input": "2", "output": "II"},
 *     {"input": "3", "output": "III"}
 *   ]
 * }
 * }</pre>
 *
 * <p><b>Critical:</b> The field name must be exactly {@code conversions}
 * — not "results", "data", or "items". The assessment specifies this name.
 *
 * @param conversions ordered list of input/output pairs
 */
public record RangeConversionResponse(List<SingleConversionResponse> conversions) {

    /**
     * Factory method — maps domain results to the response DTO.
     */
    public static RangeConversionResponse from(List<RomanNumeralResult> results) {
        List<SingleConversionResponse> conversions = results.stream()
                .map(SingleConversionResponse::from)
                .toList();
        return new RangeConversionResponse(conversions);
    }
}
