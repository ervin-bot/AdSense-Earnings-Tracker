package ro.mobilissimo.adsensetracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TrendAnalyticsTest {
    private static final double TOLERANCE = 0.000_001d;

    @Test
    public void sevenDayAnalysisFillsMissingDaysAndBuildsEqualPreviousWindow() {
        LocalDate anchor = LocalDate.of(2026, 8, 3);
        List<TrendAnalytics.DailyPoint> points = Arrays.asList(
                point("2026-07-21", 4.0d),
                point("2026-07-27", 6.0d),
                point("2026-07-28", 10.0d),
                point("2026-07-30", 5.0d),
                point("2026-08-03", 7.0d)
        );

        TrendAnalytics.TrendSummary summary = TrendAnalytics.analyze7Days(
                points,
                anchor,
                false
        );

        assertEquals(7, summary.getWindowDays());
        assertEquals(7, summary.getCurrentSeries().size());
        assertEquals(7, summary.getPreviousSeries().size());
        assertEquals(LocalDate.of(2026, 7, 28), summary.getCurrentSeries().get(0).getDate());
        assertEquals(anchor, summary.getCurrentSeries().get(6).getDate());
        assertEquals(LocalDate.of(2026, 7, 21), summary.getPreviousSeries().get(0).getDate());
        assertEquals(LocalDate.of(2026, 7, 27), summary.getPreviousSeries().get(6).getDate());
        assertEquals(0.0d, summary.getCurrentSeries().get(1).getValue(), TOLERANCE);
        assertEquals(22.0d, summary.getCurrentTotal(), TOLERANCE);
        assertEquals(10.0d, summary.getPreviousTotal(), TOLERANCE);
        assertEquals(12.0d, summary.getAbsoluteDelta(), TOLERANCE);
        assertTrue(summary.getPercentageDelta().isPresent());
        assertEquals(120.0d, summary.getPercentageDelta().getAsDouble(), TOLERANCE);
        assertEquals(22.0d / 7.0d, summary.getDailyAverage(), TOLERANCE);
        assertEquals(point("2026-07-28", 10.0d), summary.getBestDay());
    }

    @Test
    public void excludingAnchorDateOmitsIncompleteDayFromBothComparisonBoundaries() {
        LocalDate anchor = LocalDate.of(2026, 8, 3);
        List<TrendAnalytics.DailyPoint> points = Arrays.asList(
                point("2026-08-03", 1_000.0d),
                point("2026-08-02", 12.0d),
                point("2026-07-26", 3.0d)
        );

        TrendAnalytics.TrendSummary summary = TrendAnalytics.analyze7Days(
                points,
                anchor,
                true
        );

        assertTrue(summary.isAnchorDateExcluded());
        assertEquals(LocalDate.of(2026, 7, 27), summary.getCurrentSeries().get(0).getDate());
        assertEquals(LocalDate.of(2026, 8, 2), summary.getCurrentSeries().get(6).getDate());
        assertEquals(LocalDate.of(2026, 7, 20), summary.getPreviousSeries().get(0).getDate());
        assertEquals(LocalDate.of(2026, 7, 26), summary.getPreviousSeries().get(6).getDate());
        assertEquals(12.0d, summary.getCurrentTotal(), TOLERANCE);
        assertEquals(3.0d, summary.getPreviousTotal(), TOLERANCE);
    }

    @Test
    public void thirtyDayConvenienceMethodCreatesTwoThirtyDaySeries() {
        LocalDate anchor = LocalDate.of(2026, 8, 3);

        TrendAnalytics.TrendSummary summary = TrendAnalytics.analyze30Days(
                Collections.singletonList(point("2026-08-03", 8.0d)),
                anchor,
                false
        );

        assertEquals(30, summary.getWindowDays());
        assertEquals(30, summary.getCurrentSeries().size());
        assertEquals(30, summary.getPreviousSeries().size());
        assertEquals(LocalDate.of(2026, 7, 5), summary.getCurrentSeries().get(0).getDate());
        assertEquals(LocalDate.of(2026, 6, 5), summary.getPreviousSeries().get(0).getDate());
        assertEquals(LocalDate.of(2026, 7, 4), summary.getPreviousSeries().get(29).getDate());
    }

    @Test
    public void percentageDeltaHandlesZeroBaselineWithoutInfinityOrNaN() {
        LocalDate anchor = LocalDate.of(2026, 8, 3);

        TrendAnalytics.TrendSummary zeroToZero = TrendAnalytics.analyze7Days(
                Collections.emptyList(),
                anchor,
                false
        );
        assertTrue(zeroToZero.getPercentageDelta().isPresent());
        assertEquals(0.0d, zeroToZero.getPercentageDelta().getAsDouble(), TOLERANCE);

        TrendAnalytics.TrendSummary zeroToRevenue = TrendAnalytics.analyze7Days(
                Collections.singletonList(point("2026-08-03", 5.0d)),
                anchor,
                false
        );
        assertFalse(zeroToRevenue.getPercentageDelta().isPresent());
        assertEquals(5.0d, zeroToRevenue.getAbsoluteDelta(), TOLERANCE);
    }

    @Test
    public void negativeBaselineUsesMagnitudeSoASmallerLossIsAnImprovement() {
        LocalDate anchor = LocalDate.of(2026, 8, 3);
        List<TrendAnalytics.DailyPoint> points = Arrays.asList(
                point("2026-07-27", -10.0d),
                point("2026-08-03", -5.0d)
        );

        TrendAnalytics.TrendSummary summary = TrendAnalytics.analyze7Days(
                points,
                anchor,
                false
        );

        assertEquals(-5.0d, summary.getCurrentTotal(), TOLERANCE);
        assertEquals(-10.0d, summary.getPreviousTotal(), TOLERANCE);
        assertTrue(summary.getPercentageDelta().isPresent());
        assertEquals(50.0d, summary.getPercentageDelta().getAsDouble(), TOLERANCE);
    }

    @Test
    public void duplicateDatesAreAggregatedAndReturnedSeriesAreImmutable() {
        LocalDate anchor = LocalDate.of(2026, 8, 3);
        List<TrendAnalytics.DailyPoint> input = new ArrayList<>(Arrays.asList(
                point("2026-08-03", 2.5d),
                point("2026-08-03", 3.5d)
        ));

        TrendAnalytics.TrendSummary summary = TrendAnalytics.analyze7Days(
                input,
                anchor,
                false
        );
        input.clear();

        assertEquals(6.0d, summary.getCurrentTotal(), TOLERANCE);
        assertEquals(point("2026-08-03", 6.0d), summary.getBestDay());
        assertThrows(
                UnsupportedOperationException.class,
                () -> summary.getCurrentSeries().add(point("2026-08-04", 1.0d))
        );
    }

    @Test
    public void rejectsInvalidWindowAndNonFiniteValues() {
        LocalDate anchor = LocalDate.of(2026, 8, 3);

        assertThrows(
                IllegalArgumentException.class,
                () -> TrendAnalytics.analyze(Collections.emptyList(), anchor, 0, false)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new TrendAnalytics.DailyPoint(anchor, Double.NaN)
        );
        assertThrows(
                NullPointerException.class,
                () -> TrendAnalytics.analyze(
                        Collections.singletonList(null),
                        anchor,
                        TrendAnalytics.WINDOW_7_DAYS,
                        false
                )
        );
    }

    private static TrendAnalytics.DailyPoint point(String date, double value) {
        return new TrendAnalytics.DailyPoint(LocalDate.parse(date), value);
    }
}
