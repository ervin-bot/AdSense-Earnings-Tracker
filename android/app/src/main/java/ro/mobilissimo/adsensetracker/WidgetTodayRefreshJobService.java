package ro.mobilissimo.adsensetracker;

import android.accounts.Account;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WidgetTodayRefreshJobService extends JobService {
    private static final String TAG = "AdSenseWidgetRefresh";
    private static final String ADSENSE_API_BASE = "https://adsense.googleapis.com/v2";
    private static final String ADSENSE_SCOPE = "https://www.googleapis.com/auth/adsense.readonly";
    private static final int IMMEDIATE_JOB_ID = 7011;
    private static final int PERIODIC_JOB_ID = 7012;
    private static final long PERIODIC_REFRESH_MS = 15L * 60L * 1000L;
    private static final long IMMEDIATE_MIN_LATENCY_MS = 0L;

    private static final String PREFS = "adsense_tracker";
    private static final String PREF_CURRENCY = "currencyCode";
    private static final String PREF_ADSENSE_ACCOUNT_NAME = "adsenseAccountName";
    private static final String PREF_ADSENSE_ACCOUNT_DISPLAY_NAME = "adsenseAccountDisplayName";
    private static final String PREF_DEMO = "useDemoMode";
    private static final String PREF_WIDGET_TODAY_AMOUNT = "widgetTodayAmount";
    private static final String PREF_WIDGET_TODAY_CHANGE = "widgetTodayChange";
    private static final String PREF_WIDGET_PROJECTION_AMOUNT = "widgetProjectionAmount";
    private static final String PREF_WIDGET_PROJECTION_META = "widgetProjectionMeta";
    private static final String PREF_WIDGET_SOURCE = "widgetSource";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicReference<RunningJob> activeJob = new AtomicReference<>();

    @Override
    public boolean onStartJob(JobParameters params) {
        RunningJob job = new RunningJob(params);
        if (!activeJob.compareAndSet(null, job)) {
            Log.i(TAG, "Coalescing widget refresh because another refresh is already active.");
            return false;
        }

        try {
            job.future = executor.submit(() -> executeJob(job));
            if (job.stopped.get()) {
                job.future.cancel(true);
            }
            return true;
        } catch (RejectedExecutionException error) {
            activeJob.compareAndSet(job, null);
            Log.w(TAG, "Widget refresh ignored because the service is shutting down.", error);
            return false;
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        RunningJob job = activeJob.get();
        boolean shouldReschedule = false;
        if (job != null && job.params.getJobId() == params.getJobId()) {
            synchronized (job) {
                if (!job.completed) {
                    job.stopped.set(true);
                    shouldReschedule = hasWidgetsSafely(getApplicationContext());
                    HttpURLConnection connection = job.connection;
                    if (connection != null) {
                        connection.disconnect();
                    }
                    Future<?> future = job.future;
                    if (future != null) {
                        future.cancel(true);
                    }
                }
            }
            activeJob.compareAndSet(job, null);
        }
        return shouldReschedule;
    }

    @Override
    public void onDestroy() {
        RunningJob job = activeJob.getAndSet(null);
        if (job != null) {
            synchronized (job) {
                if (!job.completed) {
                    job.stopped.set(true);
                    HttpURLConnection connection = job.connection;
                    if (connection != null) {
                        connection.disconnect();
                    }
                    Future<?> future = job.future;
                    if (future != null) {
                        future.cancel(true);
                    }
                }
            }
        }
        executor.shutdownNow();
        super.onDestroy();
    }

    private void executeJob(RunningJob job) {
        boolean coordinatorAcquired = false;
        boolean publishWidgetState = false;
        try {
            if (job.stopped.get()) {
                return;
            }

            coordinatorAcquired = RefreshCoordinator.tryAcquireBackground();
            if (!coordinatorAcquired) {
                Log.i(TAG, "Skipping widget API refresh while the full app refresh is active.");
                return;
            }

            if (job.stopped.get()) {
                return;
            }

            try {
                refreshTodayPayload(getApplicationContext(), job);
            } catch (UserRecoverableAuthException error) {
                if (!job.stopped.get()) {
                    saveWidgetStatusSafely("Open app to reconnect", "Auth needs attention");
                }
            } catch (Exception error) {
                if (!job.stopped.get()) {
                    Log.w(TAG, "Background widget refresh failed.", error);
                    saveWidgetStatusSafely("Background refresh failed", shortMessage(error));
                }
            }
            publishWidgetState = !job.stopped.get();
        } catch (RuntimeException error) {
            if (!job.stopped.get()) {
                Log.e(TAG, "Unexpected widget refresh failure.", error);
                saveWidgetStatusSafely("Background refresh failed", shortMessage(error));
                publishWidgetState = true;
            }
        } finally {
            if (coordinatorAcquired) {
                RefreshCoordinator.release();
            }

            if (publishWidgetState && !job.stopped.get()) {
                updateWidgetsSafely();
            }

            completeJob(job);
        }
    }

    private void completeJob(RunningJob job) {
        synchronized (job) {
            if (job.stopped.get() || job.completed) {
                return;
            }
            job.completed = true;
            finishJobSafely(job.params);
        }
        activeJob.compareAndSet(job, null);
    }

    private void saveWidgetStatusSafely(String status, String source) {
        try {
            saveWidgetStatus(getApplicationContext(), status, source);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not save widget refresh status.", error);
        }
    }

    private void updateWidgetsSafely() {
        try {
            EarningsCardWidgetProvider.updateAll(getApplicationContext());
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not update the card widget.", error);
        }

        try {
            EarningsMiniWidgetProvider.updateAll(getApplicationContext());
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not update the mini widget.", error);
        }
    }

    private void finishJobSafely(JobParameters params) {
        try {
            jobFinished(params, false);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not finish widget refresh job " + params.getJobId() + ".", error);
        }
    }

    public static void enqueueRefresh(Context context) {
        try {
            if (!hasWidgets(context)) {
                return;
            }

            schedulePeriodic(context);
            JobScheduler jobScheduler = scheduler(context);
            if (jobScheduler == null || jobScheduler.getPendingJob(IMMEDIATE_JOB_ID) != null) {
                return;
            }

            ComponentName service = new ComponentName(context, WidgetTodayRefreshJobService.class);
            JobInfo job = new JobInfo.Builder(IMMEDIATE_JOB_ID, service)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(IMMEDIATE_MIN_LATENCY_MS)
                .build();
            if (jobScheduler.schedule(job) != JobScheduler.RESULT_SUCCESS) {
                Log.w(TAG, "Android rejected the immediate widget refresh job.");
            }
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not enqueue widget refresh.", error);
        }
    }

    public static void schedulePeriodic(Context context) {
        try {
            JobScheduler jobScheduler = scheduler(context);
            if (jobScheduler == null) {
                return;
            }

            if (!hasWidgets(context)) {
                jobScheduler.cancel(PERIODIC_JOB_ID);
                return;
            }

            if (jobScheduler.getPendingJob(PERIODIC_JOB_ID) != null) {
                return;
            }

            ComponentName service = new ComponentName(context, WidgetTodayRefreshJobService.class);
            JobInfo job = new JobInfo.Builder(PERIODIC_JOB_ID, service)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(PERIODIC_REFRESH_MS)
                .build();
            if (jobScheduler.schedule(job) != JobScheduler.RESULT_SUCCESS) {
                Log.w(TAG, "Android rejected the periodic widget refresh job.");
            }
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not schedule periodic widget refresh.", error);
        }
    }

    public static void cancelIfNoWidgets(Context context) {
        try {
            if (hasWidgets(context)) {
                return;
            }
            JobScheduler jobScheduler = scheduler(context);
            if (jobScheduler == null) {
                return;
            }
            jobScheduler.cancel(IMMEDIATE_JOB_ID);
            jobScheduler.cancel(PERIODIC_JOB_ID);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not cancel widget refresh jobs.", error);
        }
    }

    private static boolean hasWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int cardCount = manager.getAppWidgetIds(new ComponentName(context, EarningsCardWidgetProvider.class)).length;
        int miniCount = manager.getAppWidgetIds(new ComponentName(context, EarningsMiniWidgetProvider.class)).length;
        return cardCount > 0 || miniCount > 0;
    }

    private static boolean hasWidgetsSafely(Context context) {
        try {
            return hasWidgets(context);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not inspect configured widgets.", error);
            return false;
        }
    }

    private static JobScheduler scheduler(Context context) {
        return (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    private void refreshTodayPayload(Context context, RunningJob job) throws IOException, JSONException, GoogleAuthException {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String currency = normalizeCurrencyCode(prefs.getString(PREF_CURRENCY, "EUR"));

        if (prefs.getBoolean(PREF_DEMO, false)) {
            saveWidgetPayload(context, 45.32, 40.12, 892.10, currency, "Demo data");
            return;
        }

        GoogleSignInAccount signedInAccount = GoogleSignIn.getLastSignedInAccount(context);
        Account androidAccount = signedInAccount == null ? null : signedInAccount.getAccount();
        if (androidAccount == null) {
            saveWidgetStatus(context, "Open app to connect", "No Google account");
            return;
        }

        String accountName = prefs.getString(PREF_ADSENSE_ACCOUNT_NAME, "");
        String displayName = prefs.getString(PREF_ADSENSE_ACCOUNT_DISPLAY_NAME, accountName);
        String token = GoogleAuthUtil.getToken(context, androidAccount, "oauth2:" + ADSENSE_SCOPE);
        if (accountName == null || accountName.length() == 0) {
            JSONObject accountResponse = apiFetch(context, "/accounts?pageSize=100", token, job);
            JSONArray accounts = accountResponse.optJSONArray("accounts");
            if (accounts == null || accounts.length() == 0) {
                saveWidgetStatus(context, "No AdSense account", "Open app for details");
                return;
            }

            JSONObject accountJson = accounts.getJSONObject(0);
            accountName = accountJson.optString("name");
            displayName = accountJson.optString("displayName", accountName);
            prefs.edit()
                .putString(PREF_ADSENSE_ACCOUNT_NAME, accountName)
                .putString(PREF_ADSENSE_ACCOUNT_DISPLAY_NAME, displayName)
                .apply();
        }

        if (accountName.length() == 0) {
            saveWidgetStatus(context, "No AdSense account", "Open app for details");
            return;
        }
        if (displayName == null || displayName.length() == 0) {
            displayName = accountName;
        }

        LocalDate today = LocalDate.now();
        JSONObject todayReport = generateReport(context, token, accountName, new DateRange(today, today), currency, job);
        JSONObject yesterdayReport = generateReport(context, token, accountName, new DateRange(today.minusDays(1), today.minusDays(1)), currency, job);
        JSONObject monthReport = generateReport(context, token, accountName, new DateRange(today.withDayOfMonth(1), today), currency, job);

        String reportCurrency = firstCurrency(todayReport, yesterdayReport, monthReport);
        if (reportCurrency.length() > 0) {
            currency = reportCurrency;
            prefs.edit().putString(PREF_CURRENCY, currency).apply();
        }

        double todayAmount = extractReportTotal(todayReport);
        double yesterdayAmount = extractReportTotal(yesterdayReport);
        double monthAmount = extractReportTotal(monthReport);
        saveWidgetPayload(context, todayAmount, yesterdayAmount, monthAmount, currency, "AdSense: " + displayName);
    }

    private JSONObject generateReport(Context context, String token, String accountName, DateRange range, String currency, RunningJob job) throws IOException, JSONException {
        StringBuilder url = new StringBuilder(ADSENSE_API_BASE)
            .append("/")
            .append(accountName)
            .append("/reports:generate?");

        appendParam(url, "metrics", "ESTIMATED_EARNINGS");
        appendParam(url, "dateRange", "CUSTOM");
        appendParam(url, "startDate.year", String.valueOf(range.start.getYear()));
        appendParam(url, "startDate.month", String.valueOf(range.start.getMonthValue()));
        appendParam(url, "startDate.day", String.valueOf(range.start.getDayOfMonth()));
        appendParam(url, "endDate.year", String.valueOf(range.end.getYear()));
        appendParam(url, "endDate.month", String.valueOf(range.end.getMonthValue()));
        appendParam(url, "endDate.day", String.valueOf(range.end.getDayOfMonth()));
        appendParam(url, "currencyCode", currency);

        return apiFetch(context, url.toString(), token, job);
    }

    private JSONObject apiFetch(Context context, String pathOrUrl, String token, RunningJob job) throws IOException, JSONException {
        URL url = pathOrUrl.startsWith("https://")
            ? new URL(pathOrUrl)
            : new URL(ADSENSE_API_BASE + pathOrUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (job.stopped.get() || Thread.currentThread().isInterrupted()) {
            connection.disconnect();
            throw new IOException("Widget refresh was cancelled.");
        }
        job.connection = connection;
        if (job.stopped.get() || Thread.currentThread().isInterrupted()) {
            job.connection = null;
            connection.disconnect();
            throw new IOException("Widget refresh was cancelled.");
        }
        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            int status = connection.getResponseCode();
            String body = readStream(status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream());

            if (status == 401) {
                try {
                    GoogleAuthUtil.clearToken(context, token);
                } catch (GoogleAuthException ignored) {
                }
            }

            if (status < 200 || status >= 300) {
                throw new IOException(formatApiError(body, status));
            }

            return new JSONObject(body);
        } finally {
            if (job.connection == connection) {
                job.connection = null;
            }
            connection.disconnect();
        }
    }

    private static void appendParam(StringBuilder url, String key, String value) throws UnsupportedEncodingException {
        if (url.charAt(url.length() - 1) != '?' && url.charAt(url.length() - 1) != '&') {
            url.append("&");
        }
        url.append(URLEncoder.encode(key, "UTF-8"))
            .append("=")
            .append(URLEncoder.encode(value, "UTF-8"));
    }

    private static String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private static double extractReportTotal(JSONObject report) {
        JSONObject totals = report.optJSONObject("totals");
        JSONArray cells = totals == null ? null : totals.optJSONArray("cells");
        if (cells == null) {
            return 0;
        }

        for (int i = 0; i < cells.length(); i++) {
            JSONObject cell = cells.optJSONObject(i);
            if (cell == null) {
                continue;
            }
            try {
                return Double.parseDouble(cell.optString("value"));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private static String extractCurrency(JSONObject report) {
        JSONArray headers = report.optJSONArray("headers");
        if (headers == null) {
            return "";
        }

        for (int i = 0; i < headers.length(); i++) {
            JSONObject header = headers.optJSONObject(i);
            if (header == null) {
                continue;
            }
            String name = header.optString("name");
            String type = header.optString("type");
            String currencyCode = header.optString("currencyCode");
            if (currencyCode.length() > 0 && ("ESTIMATED_EARNINGS".equals(name) || "METRIC_CURRENCY".equals(type))) {
                return currencyCode;
            }
        }

        return "";
    }

    private static String firstCurrency(JSONObject... reports) {
        for (JSONObject report : reports) {
            String currency = extractCurrency(report);
            if (currency.length() > 0) {
                return currency;
            }
        }
        return "";
    }

    private static void saveWidgetPayload(
        Context context,
        double today,
        double yesterday,
        double monthAmount,
        String currency,
        String source
    ) {
        LocalDate now = LocalDate.now();
        int elapsedDays = Math.max(1, now.getDayOfMonth());
        int daysInMonth = YearMonth.from(now).lengthOfMonth();
        double dailyAverage = monthAmount / elapsedDays;
        double projected = dailyAverage * daysInMonth;

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(PREF_WIDGET_TODAY_AMOUNT, formatCurrency(today, currency))
            .putString(PREF_WIDGET_TODAY_CHANGE, formatDailyChange(today, yesterday, currency))
            .putString(PREF_WIDGET_PROJECTION_AMOUNT, formatCurrency(projected, currency))
            .putString(PREF_WIDGET_PROJECTION_META, "Based on " + elapsedDays + "/" + daysInMonth + " days, " + formatCurrency(dailyAverage, currency) + "/day")
            .putString(PREF_WIDGET_SOURCE, source == null ? "" : source)
            .apply();
    }

    private static void saveWidgetStatus(Context context, String status, String source) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_WIDGET_TODAY_CHANGE, status)
            .putString(PREF_WIDGET_SOURCE, source == null ? "" : source)
            .apply();
    }

    private static String formatDailyChange(double today, double yesterday, String currency) {
        if (yesterday == 0) {
            return "No comparison yet";
        }

        double change = today - yesterday;
        double percent = Math.abs((change / yesterday) * 100);
        String prefix = change > 0 ? "+" : change < 0 ? "-" : "";
        if (change == 0) {
            return "No change vs yesterday";
        }
        return prefix + formatCurrency(Math.abs(change), currency) + " (" + prefix + String.format(Locale.US, "%.1f", percent) + "%) vs yesterday";
    }

    private static String formatCurrency(double amount, String currencyCode) {
        try {
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
            format.setCurrency(Currency.getInstance(currencyCode));
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            return format.format(amount);
        } catch (IllegalArgumentException error) {
            return String.format(Locale.US, "%.2f %s", amount, currencyCode);
        }
    }

    private static String normalizeCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.length() != 3) {
            return "EUR";
        }
        try {
            Currency.getInstance(currencyCode);
            return currencyCode;
        } catch (IllegalArgumentException error) {
            return "EUR";
        }
    }

    private static String formatApiError(String body, int status) {
        try {
            JSONObject json = new JSONObject(body);
            JSONObject error = json.optJSONObject("error");
            String message = error == null ? "" : error.optString("message");
            return message.length() > 0 ? message : "AdSense API request failed with HTTP " + status + ".";
        } catch (JSONException ignored) {
            return body.length() > 0 ? body : "AdSense API request failed with HTTP " + status + ".";
        }
    }

    private static String shortMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.length() == 0) {
            return "Open app for details";
        }
        return message.length() > 80 ? message.substring(0, 77) + "..." : message;
    }

    private static final class RunningJob {
        final JobParameters params;
        final AtomicBoolean stopped = new AtomicBoolean(false);
        volatile Future<?> future;
        volatile HttpURLConnection connection;
        boolean completed;

        RunningJob(JobParameters params) {
            this.params = params;
        }
    }

    private static class DateRange {
        final LocalDate start;
        final LocalDate end;

        DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }
}
