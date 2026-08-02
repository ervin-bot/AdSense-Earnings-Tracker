package ro.mobilissimo.adsensetracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TrendCalculatorTest {
    private static final double DELTA = 0.000_001d;

    @Test
    public void sumIsInclusiveAndTreatsMissingOrInvalidValuesAsZero() {
        LocalDate start = LocalDate.of(2025, 8, 1);
        Map<LocalDate, Double> values = new HashMap<>();
        values.put(start.minusDays(1), 100.0d);
        values.put(start, 2.5d);
        values.put(start.plusDays(1), null);
        values.put(start.plusDays(2), Double.NaN);
        values.put(start.plusDays(3), 3.5d);

        assertEquals(6.0d, TrendCalculator.sum(values, start, start.plusDays(3)), DELTA);
        assertThrows(
            IllegalArgumentException.class,
            () -> TrendCalculator.sum(values, start, start.minusDays(1))
        );
    }

    @Test
    public void weekToDateComparesTheSameWeekdaysFromPreviousWeek() {
        LocalDate today = LocalDate.of(2025, 8, 6); // Wednesday
        Map<LocalDate, Double> values = new HashMap<>();
        values.put(LocalDate.of(2025, 8, 4), 1.0d);
        values.put(LocalDate.of(2025, 8, 6), 3.0d);
        values.put(LocalDate.of(2025, 7, 28), 2.0d);
        values.put(LocalDate.of(2025, 7, 29), 4.0d);

        TrendCalculator.Comparison result = TrendCalculator.compare(
            values,
            TrendCalculator.RANGE_WEEK,
            today,
            DayOfWeek.MONDAY
        );

        assertEquals(LocalDate.of(2025, 8, 4), result.getCurrentStart());
        assertEquals(today, result.getCurrentEnd());
        assertEquals(LocalDate.of(2025, 7, 28), result.getPreviousStart());
        assertEquals(LocalDate.of(2025, 7, 30), result.getPreviousEnd());
        assertEquals(3, result.getCurrentValues().size());
        assertEquals(result.getCurrentValues().size(), result.getPreviousValues().size());
        assertEquals(1.0d, result.getCurrentValues().get(0), DELTA);
        assertEquals(0.0d, result.getCurrentValues().get(1), DELTA);
        assertEquals(3.0d, result.getCurrentValues().get(2), DELTA);
        assertEquals(2.0d, result.getPreviousValues().get(0), DELTA);
        assertEquals(4.0d, result.getPreviousValues().get(1), DELTA);
        assertEquals(0.0d, result.getPreviousValues().get(2), DELTA);
        assertEquals(4.0d, result.getCurrentTotal(), DELTA);
        assertEquals(6.0d, result.getPreviousTotal(), DELTA);
        assertEquals(-33.333_333d, result.getPercentChange(), DELTA);
        assertEquals("2025-08-04", result.getLabels().get(0));
        assertEquals("vs previous week", result.getComparisonLabel());
    }

    @Test
    public void monthToDateClampsPreviousMonthAndPadsNonexistentDays() {
        LocalDate today = LocalDate.of(2025, 3, 31);
        Map<LocalDate, Double> values = new HashMap<>();
        values.put(LocalDate.of(2025, 3, 31), 8.0d);
        values.put(LocalDate.of(2025, 2, 28), 5.0d);

        TrendCalculator.Comparison result = TrendCalculator.compare(
            values,
            TrendCalculator.RANGE_MONTH,
            today,
            DayOfWeek.MONDAY
        );

        assertEquals(LocalDate.of(2025, 3, 1), result.getCurrentStart());
        assertEquals(LocalDate.of(2025, 2, 1), result.getPreviousStart());
        assertEquals(LocalDate.of(2025, 2, 28), result.getPreviousEnd());
        assertEquals(31, result.getCurrentValues().size());
        assertEquals(31, result.getPreviousValues().size());
        assertEquals(5.0d, result.getPreviousValues().get(27), DELTA);
        assertEquals(0.0d, result.getPreviousValues().get(28), DELTA);
        assertEquals(0.0d, result.getPreviousValues().get(30), DELTA);
        assertEquals(8.0d, result.getCurrentTotal(), DELTA);
        assertEquals(5.0d, result.getPreviousTotal(), DELTA);
        assertEquals(60.0d, result.getPercentChange(), DELTA);
    }

    @Test
    public void lastThirtyDaysUsesTwoAdjacentEqualRangesAcrossMonthBoundary() {
        LocalDate today = LocalDate.of(2025, 3, 10);
        LocalDate currentStart = LocalDate.of(2025, 2, 9);
        LocalDate previousStart = LocalDate.of(2025, 1, 10);
        Map<LocalDate, Double> values = new HashMap<>();
        values.put(currentStart, 7.0d);
        values.put(previousStart, 2.0d);

        TrendCalculator.Comparison result = TrendCalculator.compare(
            values,
            TrendCalculator.RANGE_DAYS_30,
            today,
            DayOfWeek.SUNDAY
        );

        assertEquals(currentStart, result.getCurrentStart());
        assertEquals(LocalDate.of(2025, 2, 8), result.getPreviousEnd());
        assertEquals(previousStart, result.getPreviousStart());
        assertEquals(30, result.getCurrentValues().size());
        assertEquals(30, result.getPreviousValues().size());
        assertEquals(7.0d, result.getCurrentValues().get(0), DELTA);
        assertEquals(2.0d, result.getPreviousValues().get(0), DELTA);
    }

    @Test
    public void leapDayYearToDateClampsPreviousYearAndKeepsSeriesAligned() {
        LocalDate today = LocalDate.of(2024, 2, 29);
        Map<LocalDate, Double> values = new HashMap<>();
        values.put(LocalDate.of(2024, 2, 29), 10.0d);
        values.put(LocalDate.of(2023, 2, 28), 4.0d);

        TrendCalculator.Comparison result = TrendCalculator.compare(
            values,
            TrendCalculator.RANGE_YEAR,
            today,
            DayOfWeek.MONDAY
        );

        assertEquals(LocalDate.of(2024, 1, 1), result.getCurrentStart());
        assertEquals(today, result.getCurrentEnd());
        assertEquals(LocalDate.of(2023, 1, 1), result.getPreviousStart());
        assertEquals(LocalDate.of(2023, 2, 28), result.getPreviousEnd());
        assertEquals(60, result.getCurrentValues().size());
        assertEquals(60, result.getPreviousValues().size());
        assertEquals(10.0d, result.getCurrentValues().get(59), DELTA);
        assertEquals(0.0d, result.getPreviousValues().get(59), DELTA);
        assertEquals(4.0d, result.getPreviousTotal(), DELTA);
    }

    @Test
    public void zeroPreviousTotalHasNoPercentageChange() {
        LocalDate today = LocalDate.of(2025, 8, 3);
        Map<LocalDate, Double> values = new HashMap<>();
        values.put(today, 12.0d);

        TrendCalculator.Comparison result = TrendCalculator.compare(
            values,
            TrendCalculator.RANGE_WEEK,
            today,
            DayOfWeek.MONDAY
        );

        assertEquals(12.0d, result.getCurrentTotal(), DELTA);
        assertEquals(0.0d, result.getPreviousTotal(), DELTA);
        assertFalse(result.hasPercentChange());
        assertNull(result.getPercentChange());
    }

    @Test
    public void rejectsUnknownRangeAndReturnsImmutableSeries() {
        Map<LocalDate, Double> values = new HashMap<>();
        LocalDate today = LocalDate.of(2025, 8, 3);

        assertThrows(
            IllegalArgumentException.class,
            () -> TrendCalculator.compare(values, "quarter", today, DayOfWeek.MONDAY)
        );

        TrendCalculator.Comparison result = TrendCalculator.compare(
            values,
            TrendCalculator.RANGE_WEEK,
            today,
            DayOfWeek.MONDAY
        );
        assertThrows(UnsupportedOperationException.class, () -> result.getCurrentValues().add(1.0d));
        assertTrue(result.getLabels().size() > 0);
    }
}
