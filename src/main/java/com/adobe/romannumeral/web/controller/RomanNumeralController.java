package com.adobe.romannumeral.web.controller;

import com.adobe.romannumeral.application.usecase.ConvertRangeUseCase;
import com.adobe.romannumeral.application.usecase.ConvertSingleNumberUseCase;
import com.adobe.romannumeral.domain.exception.InvalidInputException;
import com.adobe.romannumeral.domain.exception.InvalidRangeException;
import com.adobe.romannumeral.domain.model.RomanNumeralRange;
import com.adobe.romannumeral.domain.service.RomanNumeralConstants;
import com.adobe.romannumeral.infrastructure.config.AppProperties;
import com.adobe.romannumeral.web.dto.ErrorResponse;
import com.adobe.romannumeral.web.dto.RangeConversionResponse;
import com.adobe.romannumeral.web.dto.SingleConversionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 *   <li>{@code ?query={integer}} — single conversion (uses CachedConverter, O(1))</li>
 *   <li>{@code ?min={integer}&max={integer}} — range conversion (uses StandardConverter + parallel)</li>
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
@Tag(name = "Roman Numeral Conversion", description = "Convert integers to Roman numerals — single values or parallel ranges")
public class RomanNumeralController {

    private final ConvertSingleNumberUseCase convertSingleNumberUseCase;
    private final ConvertRangeUseCase convertRangeUseCase;
    private final AppProperties appProperties;

    /**
     * Converts an integer to a Roman numeral, or a range of integers.
     *
     * @param query the integer to convert (single query mode)
     * @param min   the range minimum (range query mode)
     * @param max   the range maximum (range query mode)
     * @return JSON with the conversion result(s)
     */
    @GetMapping("/romannumeral")
    @Operation(
            summary = "Convert integer(s) to Roman numerals",
            description = "Single conversion: provide 'query' parameter. "
                    + "Range conversion: provide both 'min' and 'max' parameters. "
                    + "If 'query' is present, it takes precedence over min/max."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful conversion",
                    content = @Content(schema = @Schema(oneOf = {
                            SingleConversionResponse.class,
                            RangeConversionResponse.class
                    }))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input — non-integer, out of range (1-3999), or invalid range",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid API key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<?> convert(
            @Parameter(description = "Integer to convert (single query mode, 1-3999)",
                    example = "1994")
            @RequestParam(required = false) String query,

            @Parameter(description = "Range minimum (range query mode, 1-3999)",
                    example = "1")
            @RequestParam(required = false) String min,

            @Parameter(description = "Range maximum (range query mode, 1-3999)",
                    example = "3999")
            @RequestParam(required = false) String max) {

        // Precedence: query param takes priority over min/max
        if (query != null) {
            return handleSingleConversion(query);
        }

        // Range conversion — both min and max must be provided
        if (min != null && max != null) {
            return handleRangeConversion(min, max);
        }

        // No valid parameter combination provided
        throw new InvalidInputException(
                "Required: either 'query' parameter for single conversion, "
                        + "or both 'min' and 'max' parameters for range conversion");
    }

    private ResponseEntity<SingleConversionResponse> handleSingleConversion(String query) {
        int number = parseInteger(query, "query");
        var result = convertSingleNumberUseCase.execute(number);
        return ResponseEntity.ok(SingleConversionResponse.from(result));
    }

    private ResponseEntity<RangeConversionResponse> handleRangeConversion(String minStr, String maxStr) {
        int min = parseInteger(minStr, "min");
        int max = parseInteger(maxStr, "max");

        // Validate range size against configured maximum
        int rangeSize = max - min + 1;
        if (rangeSize > appProperties.getMaxRangeSize()) {
            throw new InvalidRangeException(
                    "Range size " + rangeSize + " exceeds maximum allowed size of "
                            + appProperties.getMaxRangeSize());
        }

        // RomanNumeralRange self-validates: min < max, both in bounds
        var range = new RomanNumeralRange(min, max);
        var results = convertRangeUseCase.execute(range);
        return ResponseEntity.ok(RangeConversionResponse.from(results));
    }

    /**
     * Parses a string to an integer with clear error messages.
     * Accepts String instead of int to control parsing errors — Spring's default
     * MethodArgumentTypeMismatchException produces less helpful messages.
     */
    private int parseInteger(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("Parameter '" + paramName + "' must not be empty");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // Sanitize the input value in error messages — never reflect raw user input
            // to prevent XSS in clients that render error messages as HTML
            String sanitized = value.length() > 50 ? value.substring(0, 50) + "..." : value;
            sanitized = sanitized.replaceAll("[<>&\"']", "");
            throw new InvalidInputException(
                    "'" + sanitized + "' is not a valid integer for parameter '" + paramName + "'. "
                            + "Must be a whole number between " + RomanNumeralConstants.MIN_VALUE
                            + " and " + RomanNumeralConstants.MAX_VALUE);
        }
    }
}
