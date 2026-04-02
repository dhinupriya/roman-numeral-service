package com.adobe.romannumeral.web.dto;

import com.adobe.romannumeral.domain.model.RomanNumeralResult;

/**
 * DTO for single conversion HTTP responses.
 *
 * <p>Matches the assessment-required JSON format exactly:
 * {@code {"input": "1", "output": "I"}}
 *
 * <p>Both fields are strings as specified by the assessment:
 * <em>"success responses must include a JSON payload with two string values"</em>
 *
 * @param input  the original integer as a string
 * @param output the Roman numeral representation
 */
public record SingleConversionResponse(String input, String output) {

    /**
     * Factory method — maps a domain result to a response DTO.
     * Keeps mapping logic co-located with the DTO, not scattered in controllers.
     */
    public static SingleConversionResponse from(RomanNumeralResult result) {
        return new SingleConversionResponse(result.input(), result.output());
    }
}
