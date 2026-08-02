package ro.mobilissimo.adsensetracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Compact revenue trend chart with an optional previous-period comparison.
 *
 * <p>The view deliberately owns no loading or business logic. Call {@link #setSeries(List,
 * List, List, String)} whenever a complete chart snapshot is ready. Invalid numeric values are
 * retained as gaps, so a partial API response never causes drawing failures or misleading lines.
 */
public final class EarningsTrendView extends View {
    private static final int CURRENT_COLOR = 0xFF2563EB;
    private static final int PREVIOUS_COLOR = 0xFF64748B;
    private static final int GRID_COLOR = 0xFFE7ECF3;
    private static final int LABEL_COLOR = 0xFF64748B;
    private static final int EMPTY_COLOR = 0xFF64748B;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint previousLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint endpointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF plotBounds = new RectF();
    private final RectF tooltipBounds = new RectF();

    private List<Double> currentSeries = new ArrayList<>();
    private List<Double> previousSeries = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private String currencyCode = "";
    private boolean loading;
    private int selectedIndex = -1;
    private double rangeMin = 0d;
    private double rangeMax = 1d;
    private float touchDownX;
    private float touchDownY;

    private final float density;
    private final int touchSlop;

    public EarningsTrendView(Context context) {
        this(context, null);
    }

    public EarningsTrendView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EarningsTrendView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        configurePaints();
        setClickable(true);
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        updateContentDescription();
    }

    private void configurePaints() {
        gridPaint.setColor(GRID_COLOR);
        gridPaint.setStrokeWidth(dp(1f));
        gridPaint.setStyle(Paint.Style.STROKE);

        currentFillPaint.setStyle(Paint.Style.FILL);

        currentLinePaint.setColor(CURRENT_COLOR);
        currentLinePaint.setStrokeWidth(dp(2.6f));
        currentLinePaint.setStrokeCap(Paint.Cap.ROUND);
        currentLinePaint.setStrokeJoin(Paint.Join.ROUND);
        currentLinePaint.setStyle(Paint.Style.STROKE);

        previousLinePaint.setColor(PREVIOUS_COLOR);
        previousLinePaint.setStrokeWidth(dp(1.8f));
        previousLinePaint.setStrokeCap(Paint.Cap.ROUND);
        previousLinePaint.setStrokeJoin(Paint.Join.ROUND);
        previousLinePaint.setStyle(Paint.Style.STROKE);
        previousLinePaint.setPathEffect(new DashPathEffect(
                new float[]{dp(6f), dp(5f)}, 0f));

        endpointPaint.setColor(CURRENT_COLOR);
        endpointPaint.setStyle(Paint.Style.FILL);

        axisLabelPaint.setColor(LABEL_COLOR);
        axisLabelPaint.setTextSize(sp(11f));

        emptyTitlePaint.setColor(LABEL_COLOR);
        emptyTitlePaint.setTextSize(sp(14f));
        emptyTitlePaint.setFakeBoldText(true);
        emptyTitlePaint.setTextAlign(Paint.Align.CENTER);

        emptyBodyPaint.setColor(EMPTY_COLOR);
        emptyBodyPaint.setTextSize(sp(12f));
        emptyBodyPaint.setTextAlign(Paint.Align.CENTER);

        selectionGuidePaint.setColor(Color.argb(80, 100, 116, 139));
        selectionGuidePaint.setStrokeWidth(dp(1f));
        selectionGuidePaint.setStyle(Paint.Style.STROKE);

        tooltipBackgroundPaint.setColor(0xFF172033);
        tooltipBackgroundPaint.setStyle(Paint.Style.FILL);

        tooltipLabelPaint.setColor(0xFFCBD5E1);
        tooltipLabelPaint.setTextSize(sp(10.5f));

        tooltipValuePaint.setColor(Color.WHITE);
        tooltipValuePaint.setTextSize(sp(13f));
        tooltipValuePaint.setFakeBoldText(true);
    }

    /**
     * Replaces the complete chart snapshot.
     *
     * @param current current-period values, ordered from oldest to newest
     * @param previous previous-period values, ordered from oldest to newest
     * @param labels optional labels by index; missing labels are simply omitted
     * @param currencyCode ISO 4217 currency code used by the touch tooltip
     */
    public void setSeries(List<Double> current, List<Double> previous,
                          List<String> labels, String currencyCode) {
        currentSeries = sanitizeValues(current);
        previousSeries = sanitizeValues(previous);
        this.labels = sanitizeLabels(labels);
        this.currencyCode = currencyCode == null
                ? ""
                : currencyCode.trim().toUpperCase(Locale.ROOT);
        selectedIndex = -1;
        computeRange();
        updateContentDescription();
        requestLayout();
        invalidate();
    }

    public void setLoading(boolean loading) {
        if (this.loading == loading) {
            return;
        }
        this.loading = loading;
        updateContentDescription();
        invalidate();
    }

    private static List<Double> sanitizeValues(List<Double> source) {
        List<Double> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (Double value : source) {
            if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
                result.add(null);
            } else {
                result.add(value);
            }
        }
        return result;
    }

    private static List<String> sanitizeLabels(List<String> source) {
        List<String> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (String label : source) {
            result.add(label == null ? "" : label.trim());
        }
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = Math.round(dp(320f)) + getPaddingLeft() + getPaddingRight();
        int desiredHeight = Math.round(dp(210f)) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(
                resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        updatePlotBounds();

        if (!hasAnyFiniteValue()) {
            drawEmptyState(canvas);
            return;
        }

        drawGrid(canvas);
        drawCurrentFill(canvas);
        drawSeries(canvas, previousSeries, previousLinePaint);
        drawSeries(canvas, currentSeries, currentLinePaint);
        drawCurrentEndpoint(canvas);
        drawAxisLabels(canvas);

        if (isFiniteAt(currentSeries, selectedIndex)) {
            drawSelection(canvas, selectedIndex, currentSeries.get(selectedIndex));
        }
    }

    private void updatePlotBounds() {
        float left = getPaddingLeft() + dp(8f);
        float right = getWidth() - getPaddingRight() - dp(8f);
        float top = getPaddingTop() + dp(12f);
        float labelSpace = hasUsableLabel() ? dp(26f) : dp(10f);
        float bottom = getHeight() - getPaddingBottom() - labelSpace;

        if (right <= left) {
            right = left + 1f;
        }
        if (bottom <= top) {
            bottom = top + 1f;
        }
        plotBounds.set(left, top, right, bottom);
    }

    private void drawGrid(Canvas canvas) {
        final int horizontalLines = 4;
        for (int index = 0; index < horizontalLines; index++) {
            float fraction = index / (float) (horizontalLines - 1);
            float y = plotBounds.top + fraction * plotBounds.height();
            canvas.drawLine(plotBounds.left, y, plotBounds.right, y, gridPaint);
        }
    }

    private void drawCurrentFill(Canvas canvas) {
        currentFillPaint.setShader(new LinearGradient(
                0f,
                plotBounds.top,
                0f,
                plotBounds.bottom,
                Color.argb(52, 37, 99, 235),
                Color.argb(0, 37, 99, 235),
                Shader.TileMode.CLAMP));

        int index = 0;
        while (index < currentSeries.size()) {
            while (index < currentSeries.size() && !isFiniteAt(currentSeries, index)) {
                index++;
            }
            if (index >= currentSeries.size()) {
                break;
            }
            int start = index;
            while (index + 1 < currentSeries.size()
                    && isFiniteAt(currentSeries, index + 1)) {
                index++;
            }
            int end = index;
            Path fill = buildSegmentPath(currentSeries, start, end);
            float baselineY = yForValue(0d);
            fill.lineTo(xForIndex(end, currentSeries.size()), baselineY);
            fill.lineTo(xForIndex(start, currentSeries.size()), baselineY);
            fill.close();
            canvas.drawPath(fill, currentFillPaint);
            index++;
        }
        currentFillPaint.setShader(null);
    }

    private void drawSeries(Canvas canvas, List<Double> series, Paint paint) {
        int index = 0;
        while (index < series.size()) {
            while (index < series.size() && !isFiniteAt(series, index)) {
                index++;
            }
            if (index >= series.size()) {
                break;
            }
            int start = index;
            while (index + 1 < series.size() && isFiniteAt(series, index + 1)) {
                index++;
            }
            int end = index;
            if (start == end) {
                canvas.drawCircle(
                        xForIndex(start, series.size()),
                        yForValue(series.get(start)),
                        dp(2f),
                        paint);
            } else {
                canvas.drawPath(buildSegmentPath(series, start, end), paint);
            }
            index++;
        }
    }

    private Path buildSegmentPath(List<Double> series, int start, int end) {
        Path path = new Path();
        float previousX = xForIndex(start, series.size());
        float previousY = yForValue(series.get(start));
        path.moveTo(previousX, previousY);

        for (int index = start + 1; index <= end; index++) {
            float x = xForIndex(index, series.size());
            float y = yForValue(series.get(index));
            float middleX = (previousX + x) / 2f;
            path.cubicTo(middleX, previousY, middleX, y, x, y);
            previousX = x;
            previousY = y;
        }
        return path;
    }

    private void drawCurrentEndpoint(Canvas canvas) {
        int index = lastFiniteIndex(currentSeries);
        if (index < 0) {
            return;
        }
        float x = xForIndex(index, currentSeries.size());
        float y = yForValue(currentSeries.get(index));
        endpointPaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, dp(4.5f), endpointPaint);
        endpointPaint.setColor(CURRENT_COLOR);
        canvas.drawCircle(x, y, dp(2.5f), endpointPaint);
    }

    private void drawAxisLabels(Canvas canvas) {
        int count = pointCount();
        if (count <= 0 || !hasUsableLabel()) {
            return;
        }
        float y = plotBounds.bottom + dp(19f);
        if (count == 1) {
            drawAxisLabel(canvas, 0, plotBounds.centerX(), y, Paint.Align.CENTER);
            return;
        }

        drawAxisLabel(canvas, 0, plotBounds.left, y, Paint.Align.LEFT);
        if (count > 2) {
            int middle = (count - 1) / 2;
            drawAxisLabel(canvas, middle, xForIndex(middle, count), y, Paint.Align.CENTER);
        }
        drawAxisLabel(canvas, count - 1, plotBounds.right, y, Paint.Align.RIGHT);
    }

    private void drawAxisLabel(Canvas canvas, int index, float x, float y, Paint.Align align) {
        String label = labelAt(index);
        if (label.isEmpty()) {
            return;
        }
        axisLabelPaint.setTextAlign(align);
        canvas.drawText(label, x, y, axisLabelPaint);
    }

    private void drawSelection(Canvas canvas, int index, double value) {
        float x = xForIndex(index, currentSeries.size());
        float y = yForValue(value);
        canvas.drawLine(x, plotBounds.top, x, plotBounds.bottom, selectionGuidePaint);

        endpointPaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, dp(6f), endpointPaint);
        endpointPaint.setColor(CURRENT_COLOR);
        canvas.drawCircle(x, y, dp(3.5f), endpointPaint);

        String label = labelAt(index);
        String formattedValue = formatMoney(value);
        if (label.isEmpty()) {
            label = "Selected point";
        }

        float horizontalPadding = dp(12f);
        float width = Math.max(
                tooltipLabelPaint.measureText(label),
                tooltipValuePaint.measureText(formattedValue)) + horizontalPadding * 2f;
        float height = dp(48f);
        float left = x - width / 2f;
        left = Math.max(dp(4f), Math.min(left, getWidth() - dp(4f) - width));
        float top = y - height - dp(12f);
        if (top < dp(4f)) {
            top = y + dp(12f);
        }
        top = Math.min(top, getHeight() - dp(4f) - height);
        tooltipBounds.set(left, top, left + width, top + height);
        canvas.drawRoundRect(tooltipBounds, dp(10f), dp(10f), tooltipBackgroundPaint);
        canvas.drawText(
                label,
                tooltipBounds.left + horizontalPadding,
                tooltipBounds.top + dp(17f),
                tooltipLabelPaint);
        canvas.drawText(
                formattedValue,
                tooltipBounds.left + horizontalPadding,
                tooltipBounds.top + dp(36f),
                tooltipValuePaint);
    }

    private void drawEmptyState(Canvas canvas) {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        canvas.drawText(loading ? "Updating earnings" : "No trend data yet", centerX, centerY - dp(4f), emptyTitlePaint);
        canvas.drawText(
                loading ? "Loading the daily trend…" : "Refresh to load daily earnings",
                centerX,
                centerY + dp(18f),
                emptyBodyPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || !hasFiniteValue(currentSeries)) {
            return super.onTouchEvent(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                touchDownY = event.getY();
                selectNearestPoint(event.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                float horizontalDistance = Math.abs(event.getX() - touchDownX);
                float verticalDistance = Math.abs(event.getY() - touchDownY);
                if (horizontalDistance > touchSlop
                        && horizontalDistance > verticalDistance
                        && getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                selectNearestPoint(event.getX());
                return true;
            case MotionEvent.ACTION_UP:
                selectNearestPoint(event.getX());
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                selectedIndex = -1;
                updateContentDescription();
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (!hasFiniteValue(currentSeries)) {
            return true;
        }
        if (!isFiniteAt(currentSeries, selectedIndex)) {
            selectedIndex = lastFiniteIndex(currentSeries);
            updateContentDescription();
            invalidate();
        }
        String label = labelAt(selectedIndex);
        StringBuilder announcement = new StringBuilder("Selected earnings");
        if (!label.isEmpty()) {
            announcement.append(" for ").append(label);
        }
        announcement.append(": ").append(formatMoney(currentSeries.get(selectedIndex)));
        announceForAccessibility(announcement.toString());
        return true;
    }

    private void selectNearestPoint(float touchX) {
        int nearestIndex = -1;
        float nearestDistance = Float.MAX_VALUE;
        for (int index = 0; index < currentSeries.size(); index++) {
            if (!isFiniteAt(currentSeries, index)) {
                continue;
            }
            float distance = Math.abs(touchX - xForIndex(index, currentSeries.size()));
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = index;
            }
        }
        if (nearestIndex != selectedIndex) {
            selectedIndex = nearestIndex;
            updateContentDescription();
            invalidate();
        }
    }

    private void computeRange() {
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (Double value : currentSeries) {
            if (value != null) {
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
            }
        }
        for (Double value : previousSeries) {
            if (value != null) {
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
            }
        }

        if (minimum == Double.POSITIVE_INFINITY) {
            rangeMin = 0d;
            rangeMax = 1d;
            return;
        }
        rangeMin = Math.min(0d, minimum);
        rangeMax = Math.max(0d, maximum);
        double span = rangeMax - rangeMin;
        if (span <= 0d) {
            rangeMin = 0d;
            rangeMax = 1d;
            return;
        }
        rangeMax += span * 0.08d;
        if (rangeMin < 0d) {
            rangeMin -= span * 0.04d;
        }
    }

    private float xForIndex(int index, int seriesSize) {
        if (seriesSize <= 1) {
            return plotBounds.centerX();
        }
        return plotBounds.left + (index / (float) (seriesSize - 1)) * plotBounds.width();
    }

    private float yForValue(double value) {
        double range = rangeMax - rangeMin;
        if (range <= 0d) {
            return plotBounds.centerY();
        }
        double fraction = (value - rangeMin) / range;
        fraction = Math.max(0d, Math.min(1d, fraction));
        return (float) (plotBounds.bottom - fraction * plotBounds.height());
    }

    private int pointCount() {
        return Math.max(Math.max(currentSeries.size(), previousSeries.size()), labels.size());
    }

    private boolean hasAnyFiniteValue() {
        return hasFiniteValue(currentSeries) || hasFiniteValue(previousSeries);
    }

    private static boolean hasFiniteValue(List<Double> values) {
        return lastFiniteIndex(values) >= 0;
    }

    private static int lastFiniteIndex(List<Double> values) {
        for (int index = values.size() - 1; index >= 0; index--) {
            if (isFiniteAt(values, index)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isFiniteAt(List<Double> values, int index) {
        return index >= 0 && index < values.size() && values.get(index) != null;
    }

    private boolean hasUsableLabel() {
        for (String label : labels) {
            if (label != null && !label.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private String labelAt(int index) {
        if (index < 0 || index >= labels.size()) {
            return "";
        }
        return labels.get(index);
    }

    private String formatMoney(double value) {
        try {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
            formatter.setCurrency(Currency.getInstance(currencyCode));
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            return formatter.format(value);
        } catch (IllegalArgumentException exception) {
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            String amount = formatter.format(value);
            return currencyCode.isEmpty() ? amount : amount + " " + currencyCode;
        }
    }

    private void updateContentDescription() {
        if (!hasAnyFiniteValue()) {
            setContentDescription(loading
                    ? "Revenue trend is loading."
                    : "Revenue trend. No data available.");
            return;
        }

        int currentPointCount = 0;
        double currentTotal = 0d;
        for (Double value : currentSeries) {
            if (value != null) {
                currentPointCount++;
                currentTotal += value;
            }
        }
        StringBuilder description = new StringBuilder("Revenue trend. ");
        description.append(currentPointCount)
                .append(currentPointCount == 1 ? " current data point, " : " current data points, ")
                .append(formatMoney(currentTotal));

        if (hasFiniteValue(previousSeries)) {
            double previousTotal = 0d;
            for (Double value : previousSeries) {
                if (value != null) {
                    previousTotal += value;
                }
            }
            description.append(". Previous period ").append(formatMoney(previousTotal));
        }
        if (isFiniteAt(currentSeries, selectedIndex)) {
            String label = labelAt(selectedIndex);
            description.append(". Selected ");
            if (!label.isEmpty()) {
                description.append(label).append(", ");
            }
            description.append(formatMoney(currentSeries.get(selectedIndex)));
        }
        description.append('.');
        setContentDescription(description.toString());
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
