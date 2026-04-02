package com.adobe.romannumeral;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Roman Numeral Conversion Service.
 *
 * <p>A production-grade HTTP service that converts integers (1-3999) to Roman numerals,
 * following the standard form described at
 * <a href="https://en.wikipedia.org/wiki/Roman_numerals#Standard_form">Wikipedia: Roman Numerals</a>.
 *
 * <p>Supports single conversions ({@code ?query=42}) and parallel range conversions
 * ({@code ?min=1&max=3999}) using chunked virtual thread execution.
 */
@SpringBootApplication
public class RomanNumeralApplication {

    public static void main(String[] args) {
        SpringApplication.run(RomanNumeralApplication.class, args);
    }
}
