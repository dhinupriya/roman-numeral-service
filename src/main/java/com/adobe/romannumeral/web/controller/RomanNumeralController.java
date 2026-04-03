package com.adobe.romannumeral.web.controller;

import com.adobe.romannumeral.application.usecase.ConvertSingleNumberUseCase;
import com.adobe.romannumeral.domain.exception.InvalidInputException;
import com.adobe.romannumeral.domain.service.RomanNumeralConstants;
import com.adobe.romannumeral.web.dto.SingleConversionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Roman numeral conversions.
 *
 * <p>Single endpoint ({@code /romannumeral}) handles both query types:
 * <ul>
 *   <li>{@code ?query={integer}} — single conversion</li>
 *   <li>{@code ?min={integer}&max={integer}} — range conversion (Phase 2)</li>
 * </ul>
 *
 * <p>Precedence rule: if {@code query} param is present, it's a single conversion
 * (min/max are ignored). If only min/max are present, it's a range query.
 *
 * <p>All params are accepted as Strings (not ints) so we control the parsing
 * error messages instead of getting Spring's default NumberFormatException response.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RomanNumeralController {

    private final ConvertSingleNumberUseCase convertSingleNumberUseCase;

    /**
     * Converts an integer to a Roman numeral, or a range of integers (Phase 2).
     *
     * @param query the integer to convert (single query mode)
     * @param min   the range minimum (range query mode, Phase 2)
     * @param max   the range maximum (range query mode, Phase 2)
     * @return JSON with the conversion result(s)
     */
    @GetMapping("/romannumeral")
    public ResponseEntity<?> convert(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String min,
            @RequestParam(required = false) String max) {

        // Precedence: query param takes priority over min/max
        if (query != null) {
            return handleSingleConversion(query);
        }

        // Range conversion — both min and max must be provided
        if (min != null && max != null) {
            // Phase 2: range conversion will be added here
            throw new InvalidInputException("Range queries are not yet supported");
        }

        // No valid parameter combination provided
        throw new InvalidInputException(
                "Required: either 'query' parameter for single conversion, "
                        + "or both 'min' and 'max' parameters for range conversion");
    }

    private ResponseEntity<SingleConversionResponse> handleSingleConversion(String query) {
        int number = parseInteger(query);
        var result = convertSingleNumberUseCase.execute(number);
        return ResponseEntity.ok(SingleConversionResponse.from(result));
    }

    /**
     * Parses a string to an integer with clear error messages.
     * Accepts String instead of int to control parsing errors — Spring's default
     * MethodArgumentTypeMismatchException produces less helpful messages.
     */
    private int parseInteger(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("Parameter must not be empty");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // Sanitize the input value in error messages — never reflect raw user input
            // to prevent XSS in clients that render error messages as HTML
            String sanitized = value.length() > 50 ? value.substring(0, 50) + "..." : value;
            sanitized = sanitized.replaceAll("[<>&\"']", "");
            throw new InvalidInputException(
                    "'" + sanitized + "' is not a valid integer. "
                            + "Must be a whole number between " + RomanNumeralConstants.MIN_VALUE
                            + " and " + RomanNumeralConstants.MAX_VALUE);
        }
    }
}
