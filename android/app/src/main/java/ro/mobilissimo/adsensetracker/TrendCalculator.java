package ro.mobilissimo.adsensetracker;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds like-for-like revenue comparisons without depending on Android APIs.
 * All date ranges are inclusive.
 */
public final class TrendCalculator {
    public static final String RANGE_WEEK = "week";
    public static final String RANGE_MONTH = "month";
    public static final String RANGE_DAYS_30 = "days30";
    public static final String RANGE_YEAR = "year";

    private TrendCalculator() {
    }

    /**
     * Sums the finite values in an inclusive range. Missing dates, null values and
     * non-finite values are treated as zero.
     */
    public static double sum(Map<LocalDate, Double> values, LocalDate start, LocalDate end) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must not be before start");
        }

        double total = 0.0d;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            total += valueOn(values, date);
        }
        return total;
    }

    /**
     * Creates an aligned comparison for week-to-date, month-to-date, the last 30
     * days, or year-to-date. Values are ordered from oldest to newest.
     */
    public static Comparison compare(
        Map<LocalDate, Double> values,
        String rangeKey,
        LocalDate today,
        DayOfWeek weekStart
    ) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(rangeKey, "rangeKey");
        Objects.requireNonNull(today, "today");
        Objects.requireNonNull(weekStart, "weekStart");

        Range range = rangeFor(rangeKey, today, weekStart);
        List<Double> currentValues = new ArrayList<>();
        List<Double> previousValues = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (LocalDate currentDate = range.currentStart;
             !currentDate.isAfter(range.currentEnd);
             currentDate = currentDate.plusDays(1)) {
            currentValues.add(valueOn(values, currentDate));
            labels.add(currentDate.toString());

            LocalDate previousDate = range.previousDateFor(currentDate);
            if (previousDate == null
                || previousDate.isBefore(range.previousStart)
                || previousDate.isAfter(range.previousEnd)) {
                previousValues.add(0.0d);
            } else {
                previousValues.add(valueOn(values, previousDate));
            }
        }

        double currentTotal = sumList(currentValues);
        double previousTotal = sumList(previousValues);
        Double percentChange = previousTotal == 0.0d
            ? null
            : ((currentTotal - previousTotal) / Math.abs(previousTotal)) * 100.0d;

        return new Comparison(
            currentValues,
            previousValues,
            labels,
            currentTotal,
            previousTotal,
            range.currentStart,
            range.currentEnd,
            range.previousStart,
            range.previousEnd,
            percentChange,
            range.comparisonLabel
        );
    }

    private static Range rangeFor(String rangeKey, LocalDate today, DayOfWeek weekStart) {
        switch (rangeKey) {
            case RANGE_WEEK: {
                LocalDate currentStart = today.with(TemporalAdjusters.previousOrSame(weekStart));
                LocalDate previousStart = currentStart.minusWeeks(1);
                LocalDate previousEnd = previousStart.plusDays(ChronoUnit.DAYS.between(currentStart, today));
                return Range.byOffset(
                    currentStart,
                    today,
                    previousStart,
                    previousEnd,
                    "vs previous week"
                );
            }
            case RANGE_MONTH: {
                LocalDate currentStart = today.withDayOfMonth(1);
                LocalDate previousStart = currentStart.minusMonths(1);
                int previousEndDay = Math.min(today.getDayOfMonth(), previousStart.lengthOfMonth());
                LocalDate previousEnd = previousStart.withDayOfMonth(previousEndDay);
                return Range.byCalendarDate(
                    currentStart,
                    today,
                    previousStart,
                    previousEnd,
                    "vs previous month"
                );
            }
            case RANGE_DAYS_30: {
                LocalDate currentStart = today.minusDays(29);
                LocalDate previousEnd = currentStart.minusDays(1);
                LocalDate previousStart = previousEnd.minusDays(29);
                return Range.byOffset(
                    currentStart,
                    today,
                    previousStart,
                    previousEnd,
                    "vs previous 30 days"
                );
            }
            case RANGE_YEAR: {
                LocalDate currentStart = today.withDayOfYear(1);
                LocalDate previousStart = currentStart.minusYears(1);
                LocalDate previousEnd = clampToYear(today, today.getYear() - 1);
                return Range.byCalendarDate(
                    currentStart,
                    today,
                    previousStart,
                    previousEnd,
                    "vs previous year"
                );
            }
            default:
                throw new IllegalArgumentException("Unsupported rangeKey: " + rangeKey);
        }
    }

    private static LocalDate clampToYear(LocalDate date, int year) {
        try {
            return date.withYear(year);
        } catch (DateTimeException ignored) {
            return LocalDate.of(year, date.getMonth(), 1)
                .withDayOfMonth(date.getMonth().length(java.time.Year.isLeap(year)));
        }
    }

    private static LocalDate sameCalendarDate(LocalDate currentDate, int targetYear, int targetMonth) {
        if (currentDate.getDayOfMonth() > java.time.YearMonth.of(targetYear, targetMonth).lengthOfMonth()) {
            return null;
        }
        return LocalDate.of(targetYear, targetMonth, currentDate.getDayOfMonth());
    }

    private static double valueOn(Map<LocalDate, Double> values, LocalDate date) {
        Double value = values.get(date);
        return value != null && Double.isFinite(value) ? value : 0.0d;
    }

    private static double sumList(List<Double> values) {
        double total = 0.0d;
        for (double value : values) {
            total += value;
        }
        return total;
    }

    private enum Alignment {
        OFFSET,
        CALENDAR_DATE
    }

    private static final class Range {
        final LocalDate currentStart;
        final LocalDate currentEnd;
        final LocalDate previousStart;
        final LocalDate previousEnd;
        final String comparisonLabel;
        final Alignment alignment;

        private Range(
            LocalDate currentStart,
            LocalDate currentEnd,
            LocalDate previousStart,
            LocalDate previousEnd,
            String comparisonLabel,
            Alignment alignment
        ) {
            this.currentStart = currentStart;
            this.currentEnd = currentEnd;
            this.previousStart = previousStart;
            this.previousEnd = previousEnd;
            this.comparisonLabel = comparisonLabel;
            this.alignment = alignment;
        }

        static Range byOffset(
            LocalDate currentStart,
            LocalDate currentEnd,
            LocalDate previousStart,
            LocalDate previousEnd,
            String comparisonLabel
        ) {
            return new Range(
                currentStart,
                currentEnd,
                previousStart,
                previousEnd,
                comparisonLabel,
                Alignment.OFFSET
            );
        }

        static Range byCalendarDate(
            LocalDate currentStart,
            LocalDate currentEnd,
            LocalDate previousStart,
            LocalDate previousEnd,
            String comparisonLabel
        ) {
            return new Range(
                currentStart,
                currentEnd,
                previousStart,
                previousEnd,
                comparisonLabel,
                Alignment.CALENDAR_DATE
            );
        }

        LocalDate previousDateFor(LocalDate currentDate) {
            if (alignment == Alignment.OFFSET) {
                return previousStart.plusDays(ChronoUnit.DAYS.between(currentStart, currentDate));
            }

            if (currentStart.getMonthValue() == currentEnd.getMonthValue()
                && currentStart.getYear() == currentEnd.getYear()) {
                return sameCalendarDate(
                    currentDate,
                    previousStart.getYear(),
                    previousStart.getMonthValue()
                );
            }
            return sameCalendarDate(
                currentDate,
                previousStart.getYear(),
                currentDate.getMonthValue()
            );
        }
    }

    public static final class Comparison {
        private final List<Double> currentValues;
        private final List<Double> previousValues;
        private final List<String> labels;
        private final double currentTotal;
        private final double previousTotal;
        private final LocalDate currentStart;
        private final LocalDate currentEnd;
        private final LocalDate previousStart;
        private final LocalDate previousEnd;
        private final Double percentChange;
        private final String comparisonLabel;

        private Comparison(
            List<Double> currentValues,
            List<Double> previousValues,
            List<String> labels,
            double currentTotal,
            double previousTotal,
            LocalDate currentStart,
            LocalDate currentEnd,
            LocalDate previousStart,
            LocalDate previousEnd,
            Double percentChange,
            String comparisonLabel
        ) {
            this.currentValues = Collections.unmodifiableList(new ArrayList<>(currentValues));
            this.previousValues = Collections.unmodifiableList(new ArrayList<>(previousValues));
            this.labels = Collections.unmodifiableList(new ArrayList<>(labels));
            this.currentTotal = currentTotal;
            this.previousTotal = previousTotal;
            this.currentStart = currentStart;
            this.currentEnd = currentEnd;
            this.previousStart = previousStart;
            this.previousEnd = previousEnd;
            this.percentChange = percentChange;
            this.comparisonLabel = comparisonLabel;
        }

        public List<Double> getCurrentValues() {
            return currentValues;
        }

        public List<Double> getPreviousValues() {
            return previousValues;
        }

        public List<String> getLabels() {
            return labels;
        }

        public double getCurrentTotal() {
            return currentTotal;
        }

        public double getPreviousTotal() {
            return previousTotal;
        }

        public LocalDate getCurrentStart() {
            return currentStart;
        }

        public LocalDate getCurrentEnd() {
            return currentEnd;
        }

        public LocalDate getPreviousStart() {
            return previousStart;
        }

        public LocalDate getPreviousEnd() {
            return previousEnd;
        }

        public Double getPercentChange() {
            return percentChange;
        }

        public boolean hasPercentChange() {
            return percentChange != null;
        }

        public String getComparisonLabel() {
            return comparisonLabel;
        }
    }
}
