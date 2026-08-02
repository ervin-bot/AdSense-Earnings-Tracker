package ro.mobilissimo.adsensetracker;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Builds comparable daily revenue windows from potentially sparse API data.
 */
public final class TrendAnalytics {
    public static final int WINDOW_7_DAYS = 7;
    public static final int WINDOW_30_DAYS = 30;

    private TrendAnalytics() {
    }

    public static TrendSummary analyze7Days(
            Collection<DailyPoint> points,
            LocalDate anchorDate,
            boolean excludeAnchorDate
    ) {
        return analyze(points, anchorDate, WINDOW_7_DAYS, excludeAnchorDate);
    }

    public static TrendSummary analyze30Days(
            Collection<DailyPoint> points,
            LocalDate anchorDate,
            boolean excludeAnchorDate
    ) {
        return analyze(points, anchorDate, WINDOW_30_DAYS, excludeAnchorDate);
    }

    /**
     * Creates a current window and the equally sized window immediately before it.
     * When {@code excludeAnchorDate} is true, the current window ends one day before
     * {@code anchorDate}; this avoids comparing an incomplete current day with a full day.
     */
    public static TrendSummary analyze(
            Collection<DailyPoint> points,
            LocalDate anchorDate,
            int windowDays,
            boolean excludeAnchorDate
    ) {
        Objects.requireNonNull(points, "points");
        Objects.requireNonNull(anchorDate, "anchorDate");
        if (windowDays <= 0) {
            throw new IllegalArgumentException("windowDays must be greater than zero");
        }

        Map<LocalDate, Double> valuesByDate = aggregateByDate(points);
        LocalDate currentEnd = excludeAnchorDate ? anchorDate.minusDays(1) : anchorDate;
        LocalDate currentStart = currentEnd.minusDays(windowDays - 1L);
        LocalDate previousEnd = currentStart.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(windowDays - 1L);

        List<DailyPoint> currentSeries = createDenseSeries(
                valuesByDate,
                currentStart,
                windowDays
        );
        List<DailyPoint> previousSeries = createDenseSeries(
                valuesByDate,
                previousStart,
                windowDays
        );

        return new TrendSummary(
                windowDays,
                excludeAnchorDate,
                currentSeries,
                previousSeries
        );
    }

    private static Map<LocalDate, Double> aggregateByDate(Collection<DailyPoint> points) {
        Map<LocalDate, Double> valuesByDate = new HashMap<>();
        for (DailyPoint point : points) {
            Objects.requireNonNull(point, "points cannot contain null");
            double combinedValue = valuesByDate.getOrDefault(point.getDate(), 0.0d)
                    + point.getValue();
            if (!Double.isFinite(combinedValue)) {
                throw new IllegalArgumentException("Combined daily value must be finite");
            }
            valuesByDate.put(point.getDate(), combinedValue);
        }
        return valuesByDate;
    }

    private static List<DailyPoint> createDenseSeries(
            Map<LocalDate, Double> valuesByDate,
            LocalDate start,
            int dayCount
    ) {
        List<DailyPoint> result = new ArrayList<>(dayCount);
        for (int offset = 0; offset < dayCount; offset++) {
            LocalDate date = start.plusDays(offset);
            result.add(new DailyPoint(date, valuesByDate.getOrDefault(date, 0.0d)));
        }
        return result;
    }

    /** Immutable revenue value for one calendar day. */
    public static final class DailyPoint {
        private final LocalDate date;
        private final double value;

        public DailyPoint(LocalDate date, double value) {
            this.date = Objects.requireNonNull(date, "date");
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("value must be finite");
            }
            this.value = value;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getValue() {
            return value;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof DailyPoint)) {
                return false;
            }
            DailyPoint that = (DailyPoint) other;
            return Double.compare(value, that.value) == 0 && date.equals(that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date, value);
        }

        @Override
        public String toString() {
            return "DailyPoint{" + "date=" + date + ", value=" + value + '}';
        }
    }

    /** Immutable calculated values for two adjacent, equally sized periods. */
    public static final class TrendSummary {
        private final int windowDays;
        private final boolean anchorDateExcluded;
        private final List<DailyPoint> currentSeries;
        private final List<DailyPoint> previousSeries;
        private final double currentTotal;
        private final double previousTotal;
        private final double absoluteDelta;
        private final OptionalDouble percentageDelta;
        private final double dailyAverage;
        private final DailyPoint bestDay;

        private TrendSummary(
                int windowDays,
                boolean anchorDateExcluded,
                List<DailyPoint> currentSeries,
                List<DailyPoint> previousSeries
        ) {
            this.windowDays = windowDays;
            this.anchorDateExcluded = anchorDateExcluded;
            this.currentSeries = Collections.unmodifiableList(new ArrayList<>(currentSeries));
            this.previousSeries = Collections.unmodifiableList(new ArrayList<>(previousSeries));
            this.currentTotal = total(currentSeries);
            this.previousTotal = total(previousSeries);
            this.absoluteDelta = currentTotal - previousTotal;
            this.percentageDelta = calculatePercentageDelta(
                    currentTotal,
                    previousTotal,
                    absoluteDelta
            );
            this.dailyAverage = currentTotal / windowDays;
            this.bestDay = findBestDay(currentSeries);
        }

        public int getWindowDays() {
            return windowDays;
        }

        public boolean isAnchorDateExcluded() {
            return anchorDateExcluded;
        }

        public List<DailyPoint> getCurrentSeries() {
            return currentSeries;
        }

        public List<DailyPoint> getPreviousSeries() {
            return previousSeries;
        }

        public double getCurrentTotal() {
            return currentTotal;
        }

        public double getPreviousTotal() {
            return previousTotal;
        }

        public double getAbsoluteDelta() {
            return absoluteDelta;
        }

        /**
         * Returns an empty value when the previous total is zero and the current total
         * is non-zero, because no finite percentage comparison exists. Two zero totals
         * produce a meaningful zero percent delta.
         */
        public OptionalDouble getPercentageDelta() {
            return percentageDelta;
        }

        public double getDailyAverage() {
            return dailyAverage;
        }

        public DailyPoint getBestDay() {
            return bestDay;
        }

        private static double total(List<DailyPoint> points) {
            double result = 0.0d;
            for (DailyPoint point : points) {
                result += point.getValue();
            }
            if (!Double.isFinite(result)) {
                throw new IllegalArgumentException("Period total must be finite");
            }
            return result;
        }

        private static OptionalDouble calculatePercentageDelta(
                double currentTotal,
                double previousTotal,
                double absoluteDelta
        ) {
            if (previousTotal == 0.0d) {
                return currentTotal == 0.0d
                        ? OptionalDouble.of(0.0d)
                        : OptionalDouble.empty();
            }
            return OptionalDouble.of((absoluteDelta / Math.abs(previousTotal)) * 100.0d);
        }

        private static DailyPoint findBestDay(List<DailyPoint> points) {
            DailyPoint best = points.get(0);
            for (int index = 1; index < points.size(); index++) {
                DailyPoint candidate = points.get(index);
                if (candidate.getValue() > best.getValue()) {
                    best = candidate;
                }
            }
            return best;
        }
    }
}
