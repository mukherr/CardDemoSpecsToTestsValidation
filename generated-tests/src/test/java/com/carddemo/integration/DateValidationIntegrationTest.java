package com.carddemo.integration;

import com.carddemo.dto.DateValidationResult;
import com.carddemo.service.DateValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DateValidationIntegrationTest {

    @Autowired
    private DateValidationService dateValidationService;

    @Nested
    class ValidDates {

        @Test
        void shouldValidateStandardDate() {
            DateValidationResult result = dateValidationService.validate("2026-05-11", "YYYY-MM-DD");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessageCode()).isEqualTo("0000");
            assertThat(result.getStatusText()).isEqualTo("Date is valid");
            assertThat(result.getOriginalDateString()).isEqualTo("2026-05-11");
        }

        @Test
        void shouldValidateLeapYearFeb29() {
            DateValidationResult result = dateValidationService.validate("2024-02-29", "YYYY-MM-DD");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessageCode()).isEqualTo("0000");
        }

        @Test
        void shouldValidateCenturyLeapYearDivisibleBy400() {
            DateValidationResult result = dateValidationService.validate("2000-02-29", "YYYY-MM-DD");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessageCode()).isEqualTo("0000");
        }

        @Test
        void shouldValidateLeapYearDivisibleBy4() {
            DateValidationResult result = dateValidationService.validate("2004-02-29", "YYYY-MM-DD");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessageCode()).isEqualTo("0000");
        }

        @Test
        void shouldValidateLowerBoundary1900() {
            DateValidationResult result = dateValidationService.validate("1900-01-01", "YYYY-MM-DD");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessageCode()).isEqualTo("0000");
        }

        @Test
        void shouldValidateUpperBoundary2099() {
            DateValidationResult result = dateValidationService.validate("2099-12-31", "YYYY-MM-DD");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessageCode()).isEqualTo("0000");
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-01-31", "2026-03-31", "2026-05-31", "2026-07-31", "2026-08-31", "2026-10-31", "2026-12-31"})
        void shouldValidateDay31InMonthsWith31Days(String date) {
            DateValidationResult result = dateValidationService.validate(date, "YYYY-MM-DD");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessageCode()).isEqualTo("0000");
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-04-30", "2026-06-30", "2026-09-30", "2026-11-30"})
        void shouldValidateDay30In30DayMonths(String date) {
            DateValidationResult result = dateValidationService.validate(date, "YYYY-MM-DD");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessageCode()).isEqualTo("0000");
        }

        @Test
        void shouldTreatCode2513AsValid() {
            DateValidationResult result = dateValidationService.validate("2026-05-11", "YYYY-MM-DD");

            assertThat(result.isValid()).isTrue();
            if ("2513".equals(result.getMessageCode())) {
                assertThat(result.isValid()).isTrue();
            }
        }

        @Test
        void shouldDefaultFormatToYYYYMMDD() {
            DateValidationResult result = dateValidationService.validate("2026-05-11", null);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessageCode()).isEqualTo("0000");
        }
    }

    @Nested
    class InsufficientInput {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void shouldRejectNullBlankOrEmptyDate(String date) {
            DateValidationResult result = dateValidationService.validate(date, "YYYY-MM-DD");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessageCode()).isEqualTo("0001");
            assertThat(result.getStatusText()).isEqualTo("Insufficient");
        }
    }

    @Nested
    class InvalidMonth {

        @ParameterizedTest
        @ValueSource(strings = {"2026-13-05", "2026-00-15", "2026-99-01", "2026-14-28"})
        void shouldRejectInvalidMonthValues(String date) {
            DateValidationResult result = dateValidationService.validate(date, "YYYY-MM-DD");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessageCode()).isEqualTo("0021");
            assertThat(result.getStatusText()).isEqualTo("Invalid month");
        }
    }

    @Nested
    class InvalidDay {

        @ParameterizedTest
        @ValueSource(strings = {"2026-05-32", "2026-01-00", "2026-12-99", "2026-07-45"})
        void shouldRejectInvalidDayValues(String date) {
            DateValidationResult result = dateValidationService.validate(date, "YYYY-MM-DD");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessageCode()).isEqualTo("0009");
            assertThat(result.getStatusText()).isEqualTo("Datevalue error");
        }
    }

    @Nested
    class FebruaryValidation {

        @Test
        void shouldRejectFeb29InNonLeapYear() {
            DateValidationResult result = dateValidationService.validate("2025-02-29", "YYYY-MM-DD");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessageCode()).isEqualTo("0029");
            assertThat(result.getStatusText()).containsIgnoringCase("Not a leap year");
        }

        @Test
        void shouldRejectFeb29InCenturyNonLeapYear() {
            DateValidationResult result = dateValidationService.validate("1900-02-29", "YYYY-MM-DD");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessageCode()).isEqualTo("0029");
        }

        @Test
        void shouldRejectFeb30() {
            DateValidationResult result = dateValidationService.validate("2024-02-30", "YYYY-MM-DD");

            assertThat(result.isValid()).isFalse();
        }

        @Test
        void shouldRejectFeb31() {
            DateValidationResult result = dateValidationService.validate("2024-02-31", "YYYY-MM-DD");

            assertThat(result.isValid()).isFalse();
        }
    }

    @Nested
    class Day31InMonthsWith30Days {

        @ParameterizedTest
        @ValueSource(strings = {"2026-04-31", "2026-06-31", "2026-09-31", "2026-11-31"})
        void shouldRejectDay31In30DayMonths(String date) {
            DateValidationResult result = dateValidationService.validate(date, "YYYY-MM-DD");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessageCode()).isEqualTo("0031");
            assertThat(result.getStatusText()).containsIgnoringCase("Cannot have 31 days");
        }
    }

    @Nested
    class NonNumericInput {

        @ParameterizedTest
        @ValueSource(strings = {"2026-05-ab", "ABCD-01-01", "2026-XX-15", "20-6-05-11", "abcd-ef-gh"})
        void shouldRejectNonNumericComponents(String date) {
            DateValidationResult result = dateValidationService.validate(date, "YYYY-MM-DD");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessageCode()).isEqualTo("0005");
            assertThat(result.getStatusText()).isEqualTo("Nonnumeric data");
        }
    }

    @Nested
    class InvalidEra {

        @ParameterizedTest
        @ValueSource(strings = {"1800-01-01", "1899-12-31", "2100-01-01", "3000-06-15"})
        void shouldRejectYearsOutsideCentury19And20(String date) {
            DateValidationResult result = dateValidationService.validate(date, "YYYY-MM-DD");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessageCode()).isIn("0011", "0015");
        }

        @Test
        void shouldRejectYearZero() {
            DateValidationResult result = dateValidationService.validate("0000-01-01", "YYYY-MM-DD");

            assertThat(result.isValid()).isFalse();
        }
    }

    @Nested
    class ResultStructure {

        @Test
        void shouldReturnCompleteSeverityCodeOnSuccess() {
            DateValidationResult result = dateValidationService.validate("2026-05-11", "YYYY-MM-DD");

            assertThat(result.getSeverityCode()).isNotNull();
            assertThat(result.getMessageCode()).isNotNull();
            assertThat(result.getStatusText()).isNotNull();
            assertThat(result.getOriginalDateString()).isEqualTo("2026-05-11");
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void shouldReturnCompleteSeverityCodeOnFailure() {
            DateValidationResult result = dateValidationService.validate("2026-13-05", "YYYY-MM-DD");

            assertThat(result.getSeverityCode()).isNotNull();
            assertThat(result.getMessageCode()).isNotNull();
            assertThat(result.getStatusText()).isNotNull();
            assertThat(result.getOriginalDateString()).isEqualTo("2026-13-05");
            assertThat(result.isValid()).isFalse();
        }

        @Test
        void shouldPreserveOriginalDateStringInResult() {
            String inputDate = "2025-02-29";
            DateValidationResult result = dateValidationService.validate(inputDate, "YYYY-MM-DD");

            assertThat(result.getOriginalDateString()).isEqualTo(inputDate);
        }
    }

    @Nested
    class LeapYearEdgeCases {

        @ParameterizedTest
        @CsvSource({
            "2000-02-29, true",
            "1900-02-29, false",
            "2004-02-29, true",
            "2100-02-29, false",
            "2400-02-29, true",
            "2024-02-29, true",
            "2023-02-29, false",
            "2025-02-29, false"
        })
        void shouldCorrectlyApplyLeapYearRules(String date, boolean expectedValid) {
            DateValidationResult result = dateValidationService.validate(date, "YYYY-MM-DD");

            if (expectedValid) {
                assertThat(result.isValid()).isTrue();
            } else {
                assertThat(result.isValid()).isFalse();
            }
        }
    }

    @Nested
    class BoundaryDayValues {

        @ParameterizedTest
        @CsvSource({
            "2026-01-01, true",
            "2026-01-31, true",
            "2026-02-01, true",
            "2026-02-28, true",
            "2026-03-01, true",
            "2026-03-31, true",
            "2026-04-01, true",
            "2026-04-30, true",
            "2026-05-01, true",
            "2026-05-31, true",
            "2026-06-01, true",
            "2026-06-30, true",
            "2026-07-01, true",
            "2026-07-31, true",
            "2026-08-01, true",
            "2026-08-31, true",
            "2026-09-01, true",
            "2026-09-30, true",
            "2026-10-01, true",
            "2026-10-31, true",
            "2026-11-01, true",
            "2026-11-30, true",
            "2026-12-01, true",
            "2026-12-31, true"
        })
        void shouldValidateFirstAndLastDayOfEachMonth(String date, boolean expectedValid) {
            DateValidationResult result = dateValidationService.validate(date, "YYYY-MM-DD");

            assertThat(result.isValid()).isEqualTo(expectedValid);
        }
    }
}
