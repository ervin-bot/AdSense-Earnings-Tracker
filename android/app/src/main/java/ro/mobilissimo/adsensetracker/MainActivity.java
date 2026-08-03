package ro.mobilissimo.adsensetracker;

import android.accounts.Account;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final String TAG = "AdSenseTracker";
    private static final String ADSENSE_API_BASE = "https://adsense.googleapis.com/v2";
    private static final String ADSENSE_SCOPE = "https://www.googleapis.com/auth/adsense.readonly";
    private static final int RC_SIGN_IN = 4201;
    private static final int RC_AUTH_RECOVERY = 4202;
    private static final int TOP_SITES_REPORT_LIMIT = 25;
    private static final DateTimeFormatter UPDATED_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TREND_LABEL_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.US);

    private static final String PREFS = "adsense_tracker";
    private static final String PREF_CURRENCY = "currencyCode";
    private static final String PREF_ADSENSE_ACCOUNT_NAME = "adsenseAccountName";
    private static final String PREF_ADSENSE_ACCOUNT_DISPLAY_NAME = "adsenseAccountDisplayName";
    private static final String PREF_ADSENSE_TIME_ZONE = "adsenseTimeZone";
    private static final String PREF_WEEK_START = "weekStartDay";
    private static final String PREF_REFRESH = "refreshInterval";
    private static final String PREF_DEMO = "useDemoMode";
    private static final String PREF_WIDGET_TODAY_AMOUNT = "widgetTodayAmount";
    private static final String PREF_WIDGET_TODAY_CHANGE = "widgetTodayChange";
    private static final String PREF_WIDGET_PROJECTION_AMOUNT = "widgetProjectionAmount";
    private static final String PREF_WIDGET_PROJECTION_META = "widgetProjectionMeta";
    private static final String PREF_WIDGET_SOURCE = "widgetSource";
    private static final int TEXT_PRIMARY = 0xFF172033;
    private static final int TEXT_SECONDARY = 0xFF64748B;
    private static final int BG_PRIMARY = 0xFFFFFFFF;
    private static final int BG_SECONDARY = 0xFFF6F8FB;
    private static final int BG_TERTIARY = 0xFFEEF2F7;
    private static final int BORDER = 0xFFDBE3EE;
    private static final int ACCENT = 0xFF2563EB;
    private static final int ACCENT_STRONG = 0xFF1D4ED8;
    private static final int SUCCESS = 0xFF15803D;
    private static final int SUCCESS_SOFT = 0xFFDCFCE7;
    private static final int WARNING_TEXT = 0xFF92400E;
    private static final int WARNING_SOFT = 0xFFFEF3C7;
    private static final int ERROR_TEXT = 0xFF991B1B;
    private static final int ERROR_SOFT = 0xFFFEF2F2;
    private static final int ERROR_BORDER = 0xFFFCA5A5;

    private static final CurrencyOption[] CURRENCIES = new CurrencyOption[] {
        new CurrencyOption("AED", "United Arab Emirates Dirham"),
        new CurrencyOption("AFN", "Afghan Afghani"),
        new CurrencyOption("ALL", "Albanian Lek"),
        new CurrencyOption("AMD", "Armenian Dram"),
        new CurrencyOption("ANG", "Netherlands Antillean Guilder"),
        new CurrencyOption("ARS", "Argentine Peso"),
        new CurrencyOption("AUD", "Australian Dollar"),
        new CurrencyOption("AWG", "Aruban Florin"),
        new CurrencyOption("AZN", "Azerbaijani Manat"),
        new CurrencyOption("BAM", "Bosnia-Herzegovina Convertible Mark"),
        new CurrencyOption("BDT", "Bangladeshi Taka"),
        new CurrencyOption("BGN", "Bulgarian Lev"),
        new CurrencyOption("BHD", "Bahraini Dinar"),
        new CurrencyOption("BND", "Brunei Dollar"),
        new CurrencyOption("BOB", "Bolivian Boliviano"),
        new CurrencyOption("BRL", "Brazilian Real"),
        new CurrencyOption("BTN", "Bhutanese Ngultrum"),
        new CurrencyOption("BWP", "Botswanan Pula"),
        new CurrencyOption("BYN", "Belarusian Ruble"),
        new CurrencyOption("BZD", "Belize Dollar"),
        new CurrencyOption("CAD", "Canadian Dollar"),
        new CurrencyOption("CHF", "Swiss Franc"),
        new CurrencyOption("CLP", "Chilean Peso"),
        new CurrencyOption("CNY", "Chinese Yuan"),
        new CurrencyOption("COP", "Colombian Peso"),
        new CurrencyOption("CRC", "Costa Rican Colon"),
        new CurrencyOption("CSD", "Serbian Dinar (2002-2006)"),
        new CurrencyOption("CZK", "Czech Koruna"),
        new CurrencyOption("CVE", "Cape Verdean Escudo"),
        new CurrencyOption("DEM", "German Mark"),
        new CurrencyOption("DKK", "Danish Krone"),
        new CurrencyOption("DOP", "Dominican Peso"),
        new CurrencyOption("DZD", "Algerian Dinar"),
        new CurrencyOption("EEK", "Estonian Kroon"),
        new CurrencyOption("EGP", "Egyptian Pound"),
        new CurrencyOption("EUR", "Euro"),
        new CurrencyOption("FJD", "Fijian Dollar"),
        new CurrencyOption("FRF", "French Franc"),
        new CurrencyOption("GBP", "British Pound Sterling"),
        new CurrencyOption("GEL", "Georgian Lari"),
        new CurrencyOption("GHS", "Ghanaian Cedi"),
        new CurrencyOption("HKD", "Hong Kong Dollar"),
        new CurrencyOption("HNL", "Honduran Lempira"),
        new CurrencyOption("HRK", "Croatian Kuna"),
        new CurrencyOption("HUF", "Hungarian Forint"),
        new CurrencyOption("IDR", "Indonesian Rupiah"),
        new CurrencyOption("ILS", "Israeli New Sheqel"),
        new CurrencyOption("INR", "Indian Rupee"),
        new CurrencyOption("IQD", "Iraqi Dinar"),
        new CurrencyOption("ISK", "Icelandic Krona"),
        new CurrencyOption("JMD", "Jamaican Dollar"),
        new CurrencyOption("JOD", "Jordanian Dinar"),
        new CurrencyOption("JPY", "Japanese Yen"),
        new CurrencyOption("KES", "Kenyan Shilling"),
        new CurrencyOption("KGS", "Kyrgystani Som"),
        new CurrencyOption("KRW", "South Korean Won"),
        new CurrencyOption("KWD", "Kuwaiti Dinar"),
        new CurrencyOption("KYD", "Cayman Islands Dollar"),
        new CurrencyOption("KZT", "Kazakhstani Tenge"),
        new CurrencyOption("LAK", "Laotian Kip"),
        new CurrencyOption("LBP", "Lebanese Pound"),
        new CurrencyOption("LKR", "Sri Lankan Rupee"),
        new CurrencyOption("LTL", "Lithuanian Litas"),
        new CurrencyOption("MAD", "Moroccan Dirham"),
        new CurrencyOption("MDL", "Moldovan Leu"),
        new CurrencyOption("MKD", "Macedonian Denar"),
        new CurrencyOption("MMK", "Myanma Kyat"),
        new CurrencyOption("MOP", "Macanese Pataca"),
        new CurrencyOption("MTL", "Maltese Lira"),
        new CurrencyOption("MUR", "Mauritian Rupee"),
        new CurrencyOption("MVR", "Maldivian Rufiyaa"),
        new CurrencyOption("MXN", "Mexican Peso"),
        new CurrencyOption("MYR", "Malaysian Ringgit"),
        new CurrencyOption("NAD", "Namibian Dollar"),
        new CurrencyOption("NGN", "Nigerian Naira"),
        new CurrencyOption("NIO", "Nicaraguan Cordoba"),
        new CurrencyOption("NOK", "Norwegian Krone"),
        new CurrencyOption("NPR", "Nepalese Rupee"),
        new CurrencyOption("NZD", "New Zealand Dollar"),
        new CurrencyOption("OMR", "Omani Rial"),
        new CurrencyOption("PAB", "Panamanian Balboa"),
        new CurrencyOption("PEN", "Peruvian Nuevo Sol"),
        new CurrencyOption("PHP", "Philippine Peso"),
        new CurrencyOption("PKR", "Pakistani Rupee"),
        new CurrencyOption("PLN", "Polish Zloty"),
        new CurrencyOption("PYG", "Paraguayan Guarani"),
        new CurrencyOption("QAR", "Qatari Rial"),
        new CurrencyOption("RON", "Romanian Leu"),
        new CurrencyOption("ROL", "Romanian Leu (1952-2006)"),
        new CurrencyOption("RSD", "Serbian Dinar"),
        new CurrencyOption("RUB", "Russian Ruble"),
        new CurrencyOption("SAR", "Saudi Riyal"),
        new CurrencyOption("SCR", "Seychellois Rupee"),
        new CurrencyOption("SEK", "Swedish Krona"),
        new CurrencyOption("SGD", "Singapore Dollar"),
        new CurrencyOption("SIT", "Slovenian Tolar"),
        new CurrencyOption("SKK", "Slovak Koruna"),
        new CurrencyOption("SVC", "Salvadoran Colon"),
        new CurrencyOption("THB", "Thai Baht"),
        new CurrencyOption("TND", "Tunisian Dinar"),
        new CurrencyOption("TRL", "Turkish Lira (1922-2005)"),
        new CurrencyOption("TRY", "Turkish Lira"),
        new CurrencyOption("TTD", "Trinidad and Tobago Dollar"),
        new CurrencyOption("TWD", "New Taiwan Dollar"),
        new CurrencyOption("TZS", "Tanzanian Shilling"),
        new CurrencyOption("UAH", "Ukrainian Hryvnia"),
        new CurrencyOption("UGX", "Ugandan Shilling"),
        new CurrencyOption("USD", "US Dollar"),
        new CurrencyOption("UYU", "Uruguayan Peso"),
        new CurrencyOption("UZS", "Uzbekistan Som"),
        new CurrencyOption("VEB", "Venezuelan Bolivar (1871-2008)"),
        new CurrencyOption("VEF", "Venezuelan Bolivar (2008-2018)"),
        new CurrencyOption("VES", "Venezuelan Bolivar"),
        new CurrencyOption("VND", "Vietnamese Dong"),
        new CurrencyOption("WST", "Samoan Tala"),
        new CurrencyOption("XCD", "East Caribbean Dollar"),
        new CurrencyOption("XOF", "CFA Franc BCEAO"),
        new CurrencyOption("XPF", "CFP Franc"),
        new CurrencyOption("YER", "Yemeni Rial"),
        new CurrencyOption("ZAR", "South African Rand")
    };

    private static final Period[] PERIODS = new Period[] {
        new Period("today", "Today"),
        new Period("yesterday", "Yesterday"),
        new Period("week", "This Week"),
        new Period("lastweek", "Last Week"),
        new Period("month", "This Month"),
        new Period("lastmonth", "Last Month"),
        new Period("days30", "Last 30 Days"),
        new Period("year", "This Year"),
        new Period("lastyear", "Last Year"),
        new Period("days365", "Last 365 Days")
    };

    private static final Period[] TREND_PERIODS = new Period[] {
        new Period("days7", "7D"),
        new Period("days30", "30D")
    };

    private SharedPreferences prefs;
    private GoogleSignInClient signInClient;
    private GoogleSignInAccount account;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, TextView> periodButtons = new HashMap<>();
    private final Map<String, TextView> trendButtons = new HashMap<>();
    private final Object connectionLock = new Object();
    private final RefreshRequestTracker refreshRequests = new RefreshRequestTracker();

    private String currentCurrency = "EUR";
    private String currentPeriod = "today";
    private String currentTrendPeriod = "days30";
    private ReportData lastData;
    private Future<?> activeRefresh;
    private HttpURLConnection activeConnection;
    private Runnable refreshRunnable;
    private ObjectAnimator refreshAnimator;
    private boolean isLoading;
    private volatile boolean activityDestroyed;
    private boolean isPullingToRefresh;
    private boolean pullRefreshArmed;
    private boolean pullRefreshActive;
    private boolean pullRefreshGestureRejected;
    private float pullRefreshStartX;
    private float pullRefreshStartY;
    private int pullRefreshTouchSlop;

    private TextView modeBadge;
    private TextView syncStatusText;
    private LinearLayout pullRefreshIndicator;
    private TextView pullRefreshText;
    private FrameLayout refreshButton;
    private ImageView refreshGlyph;
    private TextView statusText;
    private TextView todayAmount;
    private TextView todayChange;
    private TextView projectionAmount;
    private TextView projectionMeta;
    private TextView selectedPeriodLabel;
    private TextView selectedPeriodAmount;
    private TextView selectedPeriodComparison;
    private EarningsTrendView trendView;
    private TextView trendTotal;
    private TextView trendComparison;
    private TextView trendMeta;
    private TextView sitesTotal;
    private LinearLayout statusBox;
    private LinearLayout sitesList;
    private LinearLayout actionRow;
    private TextView connectButton;
    private TextView demoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(248, 250, 252));
        getWindow().setNavigationBarColor(Color.rgb(248, 250, 252));

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        currentCurrency = normalizeCurrencyCode(prefs.getString(PREF_CURRENCY, "EUR"));
        configureSignIn();
        account = GoogleSignIn.getLastSignedInAccount(this);
        buildUi();

        if (prefs.getBoolean(PREF_DEMO, false)) {
            displayDataSafely(generateMockData(), "Demo data", "Demo mode");
        } else if (account != null) {
            loadData();
        } else {
            displayEmptyState();
            setStatus("Connect Google or use demo data.");
            setSyncStatus("Not connected");
            updateModeBadge("Not connected");
        }
    }

    @Override
    protected void onDestroy() {
        activityDestroyed = true;
        cancelActiveRefresh(false);
        mainHandler.removeCallbacksAndMessages(null);
        if (refreshAnimator != null) {
            refreshAnimator.cancel();
        }
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                account = task.getResult(ApiException.class);
                prefs.edit().putBoolean(PREF_DEMO, false).apply();
                loadData();
            } catch (ApiException error) {
                setStatus("Google sign-in failed: " + error.getStatusCode());
            }
            return;
        }

        if (requestCode == RC_AUTH_RECOVERY && resultCode == RESULT_OK) {
            loadData();
        }
    }

    private void configureSignIn() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(new Scope(ADSENSE_SCOPE))
            .build();
        signInClient = GoogleSignIn.getClient(this, options);
    }

    private void buildUi() {
        ScrollView scrollView = new PullRefreshScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setBackgroundColor(BG_SECONDARY);
        pullRefreshTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        applySystemInsets(root);
        scrollView.addView(root);
        setContentView(scrollView);

        pullRefreshIndicator = vertical();
        pullRefreshIndicator.setGravity(Gravity.CENTER);
        pullRefreshIndicator.setVisibility(View.GONE);
        pullRefreshIndicator.setAlpha(0f);
        TextView pullHint = text("Pull to refresh", 12, TEXT_SECONDARY, Typeface.BOLD);
        pullHint.setGravity(Gravity.CENTER);
        pullRefreshText = pullHint;
        pullRefreshIndicator.addView(pullHint);
        root.addView(pullRefreshIndicator, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, matchWrap());

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.brand_icon);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        iconParams.setMargins(0, 0, dp(10), 0);
        header.addView(icon, iconParams);

        LinearLayout titleBlock = vertical();
        header.addView(titleBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView title = text("AdSense Earnings", 17, TEXT_PRIMARY, Typeface.BOLD);
        title.setSingleLine(true);
        titleBlock.addView(title);
        modeBadge = badgeText("");
        titleBlock.addView(modeBadge);
        syncStatusText = text("Waiting for data", 11, TEXT_SECONDARY, Typeface.NORMAL);
        syncStatusText.setPadding(0, dp(3), 0, 0);
        syncStatusText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        titleBlock.addView(syncStatusText);

        LinearLayout headerActions = horizontal();
        headerActions.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(headerActions);

        refreshButton = headerIconButton(R.drawable.ic_refresh, true);
        refreshButton.setContentDescription("Refresh earnings");
        refreshButton.setOnClickListener(view -> loadData());
        headerActions.addView(refreshButton);

        FrameLayout settingsButton = headerIconButton(R.drawable.ic_settings, false);
        settingsButton.setContentDescription("Open settings");
        settingsButton.setOnClickListener(view -> showSettingsDialog());
        headerActions.addView(settingsButton);

        actionRow = horizontal();
        actionRow.setPadding(0, dp(14), 0, dp(4));
        root.addView(actionRow, matchWrap());

        connectButton = commandButton("Connect", false);
        connectButton.setOnClickListener(view -> {
            if (account == null) {
                connectGoogle();
            }
        });
        actionRow.addView(connectButton);

        demoButton = commandButton("Demo data", false);
        demoButton.setOnClickListener(view -> {
            cancelActiveRefresh(true);
            prefs.edit().putBoolean(PREF_DEMO, true).apply();
            displayDataSafely(generateMockData(), "Demo data", "Demo mode");
        });
        actionRow.addView(demoButton);
        updateActionVisibility();

        statusBox = card(WARNING_SOFT, 0xFFFDBA74);
        statusBox.setVisibility(View.GONE);
        root.addView(statusBox, matchWrapWithTop(10));
        statusText = text("", 12, WARNING_TEXT, Typeface.BOLD);
        statusText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        statusBox.addView(statusText);

        LinearLayout highlight = card(ACCENT_STRONG, ACCENT_STRONG);
        highlight.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.addView(highlight, matchWrapWithTop(12));
        TextView todayLabel = text("Today so far · estimated", 12, Color.WHITE, Typeface.BOLD);
        highlight.addView(todayLabel);
        todayAmount = text("—", 34, Color.WHITE, Typeface.BOLD);
        todayAmount.setPadding(0, dp(5), 0, 0);
        highlight.addView(todayAmount);
        todayChange = text("No comparison yet", 12, Color.WHITE, Typeface.NORMAL);
        highlight.addView(todayChange);

        LinearLayout projectionRow = horizontal();
        projectionRow.setGravity(Gravity.CENTER_VERTICAL);
        projectionRow.setPadding(0, dp(14), 0, 0);
        highlight.addView(projectionRow, matchWrap());
        LinearLayout projectionCopy = vertical();
        projectionRow.addView(projectionCopy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        projectionCopy.addView(text("Projected month", 13, Color.WHITE, Typeface.BOLD));
        projectionMeta = text("Based on daily average", 11, Color.argb(220, 255, 255, 255), Typeface.NORMAL);
        projectionCopy.addView(projectionMeta);
        projectionAmount = text("—", 20, Color.WHITE, Typeface.BOLD);
        projectionAmount.setGravity(Gravity.END);
        projectionRow.addView(projectionAmount);

        LinearLayout trendCard = card(BG_PRIMARY, BORDER);
        root.addView(trendCard, matchWrapWithTop(14));

        LinearLayout trendHeader = horizontal();
        trendHeader.setGravity(Gravity.CENTER_VERTICAL);
        trendCard.addView(trendHeader, matchWrap());
        trendHeader.addView(text("Completed-day trend", 15, TEXT_PRIMARY, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        trendTotal = text("—", 16, TEXT_PRIMARY, Typeface.BOLD);
        trendTotal.setGravity(Gravity.END);
        trendHeader.addView(trendTotal);

        HorizontalScrollView trendTabScroller = new HorizontalScrollView(this);
        trendTabScroller.setHorizontalScrollBarEnabled(false);
        LinearLayout trendTabs = horizontal();
        trendTabs.setPadding(0, dp(12), 0, dp(6));
        trendTabScroller.addView(trendTabs);
        trendCard.addView(trendTabScroller, matchWrap());

        for (Period period : TREND_PERIODS) {
            TextView tab = periodButton(period.label);
            tab.setOnClickListener(view -> selectTrendPeriod(period.key));
            trendButtons.put(period.key, tab);
            trendTabs.addView(tab);
        }

        trendView = new EarningsTrendView(this);
        trendView.setContentDescription("Daily AdSense earnings trend");
        trendCard.addView(trendView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));

        TextView trendLegend = text("Blue: current period   ·   Dashed: previous period", 11, TEXT_SECONDARY, Typeface.NORMAL);
        trendLegend.setPadding(0, dp(3), 0, 0);
        trendCard.addView(trendLegend);

        trendComparison = text("No comparable trend yet", 12, TEXT_SECONDARY, Typeface.NORMAL);
        trendComparison.setPadding(0, dp(8), 0, 0);
        trendCard.addView(trendComparison);
        trendMeta = text("Completed days · tap the chart for details", 11, TEXT_SECONDARY, Typeface.NORMAL);
        trendMeta.setPadding(0, dp(4), 0, 0);
        trendCard.addView(trendMeta);

        TextView periodSectionTitle = text("Period breakdown", 14, TEXT_PRIMARY, Typeface.BOLD);
        root.addView(periodSectionTitle, matchWrapWithTop(18));

        HorizontalScrollView tabScroller = new HorizontalScrollView(this);
        tabScroller.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = horizontal();
        tabScroller.addView(tabs);
        root.addView(tabScroller, matchWrapWithTop(8));

        for (Period period : PERIODS) {
            TextView tab = periodButton(period.label);
            tab.setOnClickListener(view -> {
                switchPeriod(period.key);
                if (trendButtons.containsKey(period.key)) {
                    selectTrendPeriod(period.key);
                }
            });
            periodButtons.put(period.key, tab);
            tabs.addView(tab);
        }

        LinearLayout periodCard = card(BG_PRIMARY, BORDER);
        root.addView(periodCard, matchWrapWithTop(12));
        selectedPeriodLabel = text("Today", 13, TEXT_SECONDARY, Typeface.BOLD);
        periodCard.addView(selectedPeriodLabel);
        selectedPeriodAmount = text("—", 22, TEXT_PRIMARY, Typeface.BOLD);
        selectedPeriodAmount.setGravity(Gravity.END);
        periodCard.addView(selectedPeriodAmount);
        selectedPeriodComparison = text("", 12, TEXT_SECONDARY, Typeface.NORMAL);
        selectedPeriodComparison.setPadding(0, dp(5), 0, 0);
        periodCard.addView(selectedPeriodComparison);

        LinearLayout sitesHeader = horizontal();
        sitesHeader.setPadding(0, dp(18), 0, dp(8));
        root.addView(sitesHeader, matchWrap());
        sitesHeader.addView(text("Top 7 Sites", 14, TEXT_PRIMARY, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sitesTotal = text("No data", 11, TEXT_SECONDARY, Typeface.BOLD);
        sitesTotal.setGravity(Gravity.END);
        sitesHeader.addView(sitesTotal);

        LinearLayout sitesCard = card(BG_PRIMARY, BORDER);
        sitesCard.setPadding(dp(14), dp(6), dp(14), dp(14));
        root.addView(sitesCard, matchWrap());
        sitesList = vertical();
        sitesCard.addView(sitesList, matchWrap());

        switchPeriod(currentPeriod);
        selectTrendPeriod(currentTrendPeriod);
    }

    private boolean handlePullToRefreshTouch(ScrollView scrollView, MotionEvent event) {
        if (pullRefreshIndicator == null) {
            return false;
        }

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            pullRefreshStartX = event.getX();
            pullRefreshStartY = event.getY();
            isPullingToRefresh = false;
            pullRefreshArmed = false;
            pullRefreshGestureRejected = false;
            return false;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            float deltaX = event.getX() - pullRefreshStartX;
            float deltaY = event.getY() - pullRefreshStartY;
            if (pullRefreshGestureRejected) {
                return false;
            }
            if (!isPullingToRefresh
                && Math.max(Math.abs(deltaX), Math.abs(deltaY)) >= pullRefreshTouchSlop
                && deltaY <= Math.abs(deltaX) * 1.2f) {
                pullRefreshGestureRejected = true;
                return false;
            }
            if (deltaY <= 0 || scrollView.getScrollY() > 0) {
                if (isPullingToRefresh && !pullRefreshActive) {
                    resetPullRefreshIndicator();
                }
                isPullingToRefresh = false;
                pullRefreshArmed = false;
                return false;
            }

            if (deltaY < pullRefreshTouchSlop || isLoading) {
                return false;
            }

            float pullDistance = deltaY - pullRefreshTouchSlop;
            int triggerDistance = dp(72);
            isPullingToRefresh = true;
            pullRefreshArmed = pullDistance >= triggerDistance;
            updatePullRefreshIndicator(
                pullDistance,
                pullRefreshArmed ? "Release to refresh" : "Pull to refresh"
            );
            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (pullRefreshGestureRejected) {
                pullRefreshGestureRejected = false;
                return false;
            }
            if (!isPullingToRefresh) {
                return false;
            }

            boolean shouldRefresh = action == MotionEvent.ACTION_UP && pullRefreshArmed && !isLoading;
            isPullingToRefresh = false;
            pullRefreshArmed = false;

            if (shouldRefresh) {
                pullRefreshActive = true;
                showPullRefreshLoading();
                loadData();
                if (!isLoading) {
                    resetPullRefreshIndicator();
                }
            } else if (!pullRefreshActive) {
                resetPullRefreshIndicator();
            }
            return true;
        }

        return false;
    }

    private void updatePullRefreshIndicator(float pullDistance, String label) {
        int triggerDistance = dp(72);
        int maxHeight = dp(54);
        float progress = Math.min(1f, Math.max(0f, pullDistance / triggerDistance));
        int height = Math.max(dp(20), Math.round(maxHeight * progress));

        pullRefreshIndicator.animate().cancel();
        pullRefreshIndicator.setVisibility(View.VISIBLE);
        pullRefreshIndicator.setAlpha(Math.max(0.35f, progress));
        pullRefreshText.setText(label);
        setPullRefreshHeight(height);
    }

    private void showPullRefreshLoading() {
        pullRefreshIndicator.animate().cancel();
        pullRefreshIndicator.setVisibility(View.VISIBLE);
        pullRefreshIndicator.setAlpha(1f);
        pullRefreshText.setText("Refreshing...");
        setPullRefreshHeight(dp(54));
    }

    private void resetPullRefreshIndicator() {
        pullRefreshActive = false;
        if (pullRefreshIndicator == null) {
            return;
        }

        pullRefreshIndicator.animate().cancel();
        pullRefreshIndicator.setAlpha(0f);
        pullRefreshIndicator.setVisibility(View.GONE);
        if (pullRefreshText != null) {
            pullRefreshText.setText("Pull to refresh");
        }
        setPullRefreshHeight(0);
    }

    private void setPullRefreshHeight(int height) {
        ViewGroup.LayoutParams params = pullRefreshIndicator.getLayoutParams();
        if (params == null || params.height == height) {
            return;
        }
        params.height = height;
        pullRefreshIndicator.setLayoutParams(params);
    }

    private void connectGoogle() {
        startActivityForResult(signInClient.getSignInIntent(), RC_SIGN_IN);
    }

    private void disconnectGoogle() {
        cancelActiveRefresh(true);
        prefs.edit().putBoolean(PREF_DEMO, false).apply();
        signInClient.signOut().addOnCompleteListener(task -> {
            if (activityDestroyed || isFinishing() || isDestroyed()) {
                return;
            }
            account = null;
            lastData = null;
            connectButton.setText("Connect");
            updateActionVisibility();
            displayEmptyState();
            updateModeBadge("Not connected");
            setStatus("Disconnected.");
            setSyncStatus("Not connected");
        });
    }

    private void loadData() {
        if (activityDestroyed || isLoading) {
            return;
        }

        if (prefs.getBoolean(PREF_DEMO, false)) {
            displayDataSafely(generateMockData(), "Demo data", "Demo mode");
            return;
        }

        if (account == null) {
            setStatus("Connect Google before loading live AdSense data.");
            setSyncStatus("Not connected");
            updateModeBadge("Not connected");
            return;
        }

        if (modeBadge.getText() == null || modeBadge.getText().length() == 0 || "Not connected".contentEquals(modeBadge.getText())) {
            updateModeBadge("Live mode");
        }
        setStatus("");
        setSyncStatus(lastData == null ? "Loading earnings…" : "Updating totals…");
        setLoading(true);
        long generation = refreshRequests.begin();
        GoogleSignInAccount refreshAccount = account;
        String refreshCurrency = currentCurrency;
        ReportData fallbackData = lastData;

        try {
            activeRefresh = executor.submit(() -> {
                boolean coordinatorAcquired = false;
                try {
                    RefreshCoordinator.acquireForeground();
                    coordinatorAcquired = true;
                    String token = getAccessToken(refreshAccount);
                    ReportData data = fetchFromAdSenseApi(token, refreshCurrency, fallbackData, generation);
                    runOnUiThreadIfActive(generation, () -> {
                        activeRefresh = null;
                        applyRefreshSuccess(data);
                    });
                } catch (UserRecoverableAuthException recoverable) {
                    runOnUiThreadIfActive(generation, () -> {
                        activeRefresh = null;
                        setLoading(false);
                        startActivityForResult(recoverable.getIntent(), RC_AUTH_RECOVERY);
                    });
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    runOnUiThreadIfActive(generation, () -> {
                        activeRefresh = null;
                        setLoadingSafely(false);
                    });
                } catch (Exception error) {
                    runOnUiThreadIfActive(generation, () -> {
                        activeRefresh = null;
                        applyRefreshFailure(error);
                    });
                } finally {
                    if (coordinatorAcquired) {
                        RefreshCoordinator.release();
                    }
                }
            });
        } catch (RejectedExecutionException error) {
            Log.w(TAG, "Refresh ignored because the activity is shutting down.", error);
            setLoading(false);
        }
    }

    private void runOnUiThreadIfActive(long generation, Runnable action) {
        runOnUiThread(() -> {
            if (activityDestroyed || !refreshRequests.isCurrent(generation) || isFinishing() || isDestroyed()) {
                return;
            }
            try {
                action.run();
            } catch (RuntimeException error) {
                Log.e(TAG, "Refresh UI update failed.", error);
                applyRefreshFailure(error);
            }
        });
    }

    private void applyRefreshSuccess(ReportData data) {
        try {
            displayDataSafely(data, data.source, "Live mode");
        } finally {
            setLoadingSafely(false);
        }
    }

    private void applyFastRefresh(ReportData data) {
        try {
            displayDashboardData(data, data.source, "Live mode", false);
            setSyncStatus("Totals ready · Updating site breakdowns…");
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not apply the fast earnings refresh.", error);
        }
    }

    private void displayDataSafely(ReportData data, String source, String mode) {
        try {
            displayData(data, source, mode);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not apply refreshed AdSense data.", error);
            applyRefreshFailure(error);
        }
    }

    private void applyRefreshFailure(Exception error) {
        setLoadingSafely(false);
        try {
            updateModeBadge("Needs attention");
            String message = error.getMessage();
            setStatus(message == null || message.length() == 0 ? "Failed to load AdSense data." : message, true);
            setSyncStatus(lastData == null ? "Update failed" : "Showing last successful data");
        } catch (RuntimeException uiError) {
            Log.e(TAG, "Could not display the refresh error.", uiError);
        }
    }

    private void setLoadingSafely(boolean loading) {
        try {
            setLoading(loading);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not update the refresh indicator.", error);
        }
    }

    private String getAccessToken(GoogleSignInAccount refreshAccount) throws IOException, GoogleAuthException {
        Account androidAccount = refreshAccount == null ? null : refreshAccount.getAccount();
        if (androidAccount == null) {
            throw new IOException("Google account is unavailable. Reconnect Google.");
        }

        return GoogleAuthUtil.getToken(getApplicationContext(), androidAccount, "oauth2:" + ADSENSE_SCOPE);
    }

    private ReportData fetchFromAdSenseApi(
        String token,
        String requestedCurrency,
        ReportData fallbackData,
        long generation
    ) throws IOException, JSONException, InterruptedException {
        JSONObject accountResponse = apiFetch("/accounts?pageSize=100", token);
        JSONArray accounts = accountResponse.optJSONArray("accounts");
        if (accounts == null || accounts.length() == 0) {
            throw new IOException("No AdSense account is available for this Google user.");
        }

        JSONObject accountJson = accounts.getJSONObject(0);
        String accountName = accountJson.optString("name");
        if (accountName.length() == 0) {
            throw new IOException("The AdSense account response is missing the account name.");
        }

        ZoneId reportingZone = parseZoneId(extractAccountTimeZoneId(accountJson));
        LocalDate reportingDate = LocalDate.now(reportingZone);
        Map<String, DateRange> ranges = getDateRanges(reportingDate);
        ReportData result = new ReportData();
        result.currency = requestedCurrency;
        result.accountName = accountName;
        result.accountDisplayName = accountJson.optString("displayName", accountName);
        result.reportingZoneId = reportingZone.getId();
        result.reportingDate = reportingDate;
        result.weekStartSetting = prefs.getString(PREF_WEEK_START, "monday");
        result.source = "AdSense: " + result.accountDisplayName;
        ReportData compatibleFallback = isFallbackCompatible(fallbackData, result, requestedCurrency)
            ? fallbackData
            : null;
        List<String> refreshWarnings = new ArrayList<>();
        Exception firstPeriodError = null;
        int successfulPeriodReports = 0;
        boolean dailyTrendLoaded = false;

        try {
            DateRange dailyRange = new DateRange(
                reportingDate.minusYears(1).withDayOfYear(1),
                reportingDate
            );
            JSONObject dailyReport = generateReport(
                token,
                accountName,
                dailyRange,
                new String[] { "DATE" },
                new String[] { "+DATE" },
                1000,
                requestedCurrency
            );
            String reportCurrency = extractCurrency(dailyReport);
            if (reportCurrency.length() > 0) {
                result.currency = reportCurrency;
            }
            result.dailyEarnings.putAll(extractDailyEarnings(dailyReport));
            for (Map.Entry<String, DateRange> entry : ranges.entrySet()) {
                result.periods.put(
                    entry.getKey(),
                    TrendCalculator.sum(result.dailyEarnings, entry.getValue().start, entry.getValue().end)
                );
            }
            successfulPeriodReports = ranges.size();
            dailyTrendLoaded = true;
        } catch (Exception error) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Refresh was cancelled.");
            }
            if (compatibleFallback != null) {
                result.dailyEarnings.putAll(compatibleFallback.dailyEarnings);
            }
            refreshWarnings.add("daily trend");
        }

        if (!dailyTrendLoaded) {
            for (Map.Entry<String, DateRange> entry : ranges.entrySet()) {
                try {
                    JSONObject report = generateReport(token, accountName, entry.getValue(), null, null, 0, requestedCurrency);
                    String reportCurrency = extractCurrency(report);
                    if (reportCurrency.length() > 0) {
                        result.currency = reportCurrency;
                    }
                    result.periods.put(entry.getKey(), extractReportTotal(report));
                    successfulPeriodReports++;
                } catch (Exception error) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Refresh was cancelled.");
                    }
                    if (firstPeriodError == null) {
                        firstPeriodError = error;
                    }
                    result.periods.put(entry.getKey(), fallbackPeriodAmount(compatibleFallback, entry.getKey()));
                    refreshWarnings.add(periodLabel(entry.getKey()) + " total");
                }
            }
        }

        if (successfulPeriodReports == 0 && firstPeriodError != null) {
            if (firstPeriodError instanceof IOException) {
                throw (IOException) firstPeriodError;
            }
            if (firstPeriodError instanceof JSONException) {
                throw (JSONException) firstPeriodError;
            }
            throw new IOException(firstPeriodError.getMessage() == null ? "Failed to refresh AdSense reports." : firstPeriodError.getMessage());
        }

        result.updatedAtEpochMs = System.currentTimeMillis();
        ReportData fastSnapshot = result.copy();
        for (Period period : PERIODS) {
            fastSnapshot.topSitesByPeriod.put(period.key, fallbackTopSites(compatibleFallback, period.key));
        }
        fastSnapshot.warning = formatPartialRefreshWarning(refreshWarnings);
        runOnUiThreadIfActive(generation, () -> applyFastRefresh(fastSnapshot));

        for (Map.Entry<String, DateRange> entry : ranges.entrySet()) {
            try {
                JSONObject report = generateReport(
                    token,
                    accountName,
                    entry.getValue(),
                    new String[] { "OWNED_SITE_DOMAIN_NAME" },
                    new String[] { "-ESTIMATED_EARNINGS" },
                    TOP_SITES_REPORT_LIMIT,
                    requestedCurrency
                );
                String reportCurrency = extractCurrency(report);
                if (reportCurrency.length() > 0) {
                    result.currency = reportCurrency;
                }
                result.topSitesByPeriod.put(entry.getKey(), extractTopSites(report));
            } catch (Exception error) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Refresh was cancelled.");
                }
                result.topSitesByPeriod.put(entry.getKey(), fallbackTopSites(compatibleFallback, entry.getKey()));
                refreshWarnings.add(periodLabel(entry.getKey()) + " top sites");
            }
        }

        result.warning = formatPartialRefreshWarning(refreshWarnings);
        return result;
    }

    private JSONObject generateReport(
        String token,
        String accountName,
        DateRange range,
        String[] dimensions,
        String[] orderBy,
        int limit,
        String requestedCurrency
    ) throws IOException, JSONException {
        StringBuilder url = new StringBuilder(ADSENSE_API_BASE)
            .append("/")
            .append(accountName)
            .append("/reports:generate?");

        if (dimensions != null) {
            for (String dimension : dimensions) {
                appendParam(url, "dimensions", dimension);
            }
        }

        appendParam(url, "metrics", "ESTIMATED_EARNINGS");

        if (orderBy != null) {
            for (String order : orderBy) {
                appendParam(url, "orderBy", order);
            }
        }

        appendParam(url, "dateRange", "CUSTOM");
        appendParam(url, "startDate.year", String.valueOf(range.start.getYear()));
        appendParam(url, "startDate.month", String.valueOf(range.start.getMonthValue()));
        appendParam(url, "startDate.day", String.valueOf(range.start.getDayOfMonth()));
        appendParam(url, "endDate.year", String.valueOf(range.end.getYear()));
        appendParam(url, "endDate.month", String.valueOf(range.end.getMonthValue()));
        appendParam(url, "endDate.day", String.valueOf(range.end.getDayOfMonth()));
        appendParam(url, "currencyCode", requestedCurrency);
        appendParam(url, "reportingTimeZone", "ACCOUNT_TIME_ZONE");

        if (limit > 0) {
            appendParam(url, "limit", String.valueOf(limit));
        }

        return apiFetch(url.toString(), token);
    }

    private JSONObject apiFetch(String pathOrUrl, String token) throws IOException, JSONException {
        URL url = pathOrUrl.startsWith("https://")
            ? new URL(pathOrUrl)
            : new URL(ADSENSE_API_BASE + pathOrUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        setActiveConnection(connection);
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
                    GoogleAuthUtil.clearToken(getApplicationContext(), token);
                } catch (GoogleAuthException ignored) {
                }
            }

            if (status < 200 || status >= 300) {
                throw new IOException(formatApiError(body, status));
            }

            return new JSONObject(body);
        } finally {
            clearActiveConnection(connection);
            connection.disconnect();
        }
    }

    private void setActiveConnection(HttpURLConnection connection) throws IOException {
        synchronized (connectionLock) {
            if (activityDestroyed || Thread.currentThread().isInterrupted()) {
                connection.disconnect();
                throw new IOException("Refresh was cancelled.");
            }
            activeConnection = connection;
        }
    }

    private void clearActiveConnection(HttpURLConnection connection) {
        synchronized (connectionLock) {
            if (activeConnection == connection) {
                activeConnection = null;
            }
        }
    }

    private void disconnectActiveConnection() {
        HttpURLConnection connection;
        synchronized (connectionLock) {
            connection = activeConnection;
            activeConnection = null;
        }
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (RuntimeException error) {
                Log.w(TAG, "Could not disconnect the cancelled refresh request.", error);
            }
        }
    }

    private void cancelActiveRefresh(boolean updateLoadingState) {
        refreshRequests.invalidate();
        Future<?> refresh = activeRefresh;
        activeRefresh = null;
        disconnectActiveConnection();
        if (refresh != null) {
            refresh.cancel(true);
        }
        if (updateLoadingState) {
            setLoadingSafely(false);
        }
    }

    private void appendParam(StringBuilder url, String key, String value) throws UnsupportedEncodingException {
        if (url.charAt(url.length() - 1) != '?' && url.charAt(url.length() - 1) != '&') {
            url.append("&");
        }
        url.append(URLEncoder.encode(key, "UTF-8"))
            .append("=")
            .append(URLEncoder.encode(value, "UTF-8"));
    }

    private String readStream(InputStream stream) throws IOException {
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

    private String formatApiError(String body, int status) {
        try {
            JSONObject json = new JSONObject(body);
            JSONObject error = json.optJSONObject("error");
            String message = error == null ? "" : error.optString("message");
            return simplifyApiError(message, status);
        } catch (JSONException ignored) {
            return body.length() > 0 ? simplifyApiError(body, status) : "AdSense API request failed with HTTP " + status + ".";
        }
    }

    private String simplifyApiError(String message, int status) {
        if (message == null || message.length() == 0) {
            return "AdSense API request failed with HTTP " + status + ".";
        }

        if (message.contains("AdSense Management API has not been used") || message.contains("it is disabled")) {
            Matcher matcher = Pattern.compile("project\\s+(\\d+)").matcher(message);
            String project = matcher.find() ? " for project " + matcher.group(1) : "";
            return "AdSense Management API is disabled" + project + ". Enable it in Google Cloud, wait a few minutes, then tap Refresh.";
        }

        return message;
    }

    private double extractReportTotal(JSONObject report) {
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
            String value = cell.optString("value");
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private Map<LocalDate, Double> extractDailyEarnings(JSONObject report) throws JSONException {
        Map<LocalDate, Double> daily = new LinkedHashMap<>();
        int dateIndex = -1;
        int amountIndex = -1;
        JSONArray headers = report.optJSONArray("headers");
        if (headers == null) {
            throw new JSONException("Daily report is missing its headers.");
        }
        for (int i = 0; i < headers.length(); i++) {
            JSONObject header = headers.optJSONObject(i);
            String name = header == null ? "" : header.optString("name", "");
            if ("DATE".equals(name)) {
                dateIndex = i;
            } else if ("ESTIMATED_EARNINGS".equals(name)) {
                amountIndex = i;
            }
        }
        if (dateIndex < 0 || amountIndex < 0) {
            throw new JSONException("Daily report is missing DATE or ESTIMATED_EARNINGS.");
        }

        JSONArray rows = report.optJSONArray("rows");
        if (rows == null) {
            return daily;
        }

        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            JSONArray cells = row == null ? null : row.optJSONArray("cells");
            if (cells == null || dateIndex >= cells.length() || amountIndex >= cells.length()) {
                throw new JSONException("Daily report contains an incomplete row.");
            }

            JSONObject dateCell = cells.optJSONObject(dateIndex);
            JSONObject amountCell = cells.optJSONObject(amountIndex);
            if (dateCell == null || amountCell == null) {
                throw new JSONException("Daily report contains an invalid cell.");
            }

            try {
                LocalDate date = LocalDate.parse(dateCell.optString("value", ""));
                double amount = Double.parseDouble(amountCell.optString("value", "0"));
                if (!Double.isFinite(amount)) {
                    throw new NumberFormatException("Non-finite earnings");
                }
                daily.put(date, amount);
            } catch (RuntimeException ignored) {
                throw new JSONException("Daily report contains an invalid date or amount.");
            }
        }
        return daily;
    }

    private List<SiteEarnings> extractTopSites(JSONObject report) {
        JSONArray rows = report.optJSONArray("rows");
        if (rows == null) {
            return new ArrayList<>();
        }

        List<SiteEarnings> sites = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            JSONArray cells = row == null ? null : row.optJSONArray("cells");
            if (cells == null || cells.length() < 2) {
                continue;
            }
            String name = cells.optJSONObject(0) == null ? "Unknown site" : cells.optJSONObject(0).optString("value", "Unknown site");
            double earnings = parseDouble(cells.optJSONObject(1) == null ? "0" : cells.optJSONObject(1).optString("value", "0"));
            if (earnings > 0) {
                sites.add(new SiteEarnings(name, earnings));
            }
        }

        List<SiteEarnings> mergedSites = mergeTopSites(sites);
        double topSevenTotal = 0d;
        for (int i = 0; i < Math.min(7, mergedSites.size()); i++) {
            topSevenTotal += mergedSites.get(i).earnings;
        }
        double reportTotal = extractReportTotal(report);
        if (TopSitesMerger.isInconsistentWithReportTotal(topSevenTotal, reportTotal)) {
            Log.w(
                TAG,
                "Top Sites total exceeds the matching report total after alias deduplication: "
                    + topSevenTotal + " > " + reportTotal
            );
        }
        return mergedSites;
    }

    private String extractCurrency(JSONObject report) {
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

    private String extractAccountTimeZoneId(JSONObject accountJson) {
        if (accountJson == null) {
            return "";
        }
        JSONObject timeZone = accountJson.optJSONObject("timeZone");
        if (timeZone != null) {
            return timeZone.optString("id", "");
        }
        return accountJson.optString("timeZone", "");
    }

    private ZoneId parseZoneId(String zoneId) {
        if (zoneId == null || zoneId.trim().length() == 0) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(zoneId.trim());
        } catch (RuntimeException error) {
            Log.w(TAG, "Unknown AdSense reporting timezone: " + zoneId, error);
            return ZoneId.systemDefault();
        }
    }

    private Map<String, DateRange> getDateRanges(LocalDate today) {
        String weekStartSetting = prefs.getString(PREF_WEEK_START, "monday");
        LocalDate yesterday = today.minusDays(1);
        LocalDate weekStart = today.minusDays(daysSinceWeekStart(today, weekStartSetting));
        LocalDate lastWeekStart = weekStart.minusDays(7);
        LocalDate lastWeekEnd = weekStart.minusDays(1);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastMonthEnd = monthStart.minusDays(1);
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate lastYearStart = today.minusYears(1).withDayOfYear(1);
        LocalDate lastYearEnd = yearStart.minusDays(1);

        Map<String, DateRange> ranges = new LinkedHashMap<>();
        ranges.put("today", new DateRange(today, today));
        ranges.put("yesterday", new DateRange(yesterday, yesterday));
        ranges.put("week", new DateRange(weekStart, today));
        ranges.put("lastweek", new DateRange(lastWeekStart, lastWeekEnd));
        ranges.put("month", new DateRange(monthStart, today));
        ranges.put("lastmonth", new DateRange(lastMonthStart, lastMonthEnd));
        ranges.put("days30", new DateRange(today.minusDays(29), today));
        ranges.put("year", new DateRange(yearStart, today));
        ranges.put("lastyear", new DateRange(lastYearStart, lastYearEnd));
        ranges.put("days365", new DateRange(today.minusDays(364), today));
        return ranges;
    }

    private long daysSinceWeekStart(LocalDate date, String weekStartSetting) {
        DayOfWeek day = date.getDayOfWeek();
        int isoDay = day.getValue();
        if ("sunday".equals(weekStartSetting)) {
            return isoDay % 7;
        }
        return isoDay - 1L;
    }

    private ReportData generateMockData() {
        ReportData data = new ReportData();
        data.currency = currentCurrency;
        data.source = "Demo data";
        data.reportingZoneId = ZoneId.systemDefault().getId();
        data.weekStartSetting = prefs.getString(PREF_WEEK_START, "monday");
        data.reportingDate = LocalDate.now();
        data.updatedAtEpochMs = System.currentTimeMillis();
        LocalDate seriesStart = data.reportingDate.minusYears(1).withDayOfYear(1);
        int seriesDay = 0;
        for (LocalDate date = seriesStart; !date.isAfter(data.reportingDate); date = date.plusDays(1)) {
            double wave = Math.sin(seriesDay * 0.72d) * 8.5d;
            double weekdayLift = date.getDayOfWeek().getValue() <= 5 ? 6.5d : -2.5d;
            double amount = Math.max(4d, 28d + wave + weekdayLift + seriesDay * 0.025d);
            data.dailyEarnings.put(date, Math.round(amount * 100d) / 100d);
            seriesDay++;
        }
        data.dailyEarnings.put(data.reportingDate.minusDays(1), 40.12d);
        data.dailyEarnings.put(data.reportingDate, 45.32d);

        for (Map.Entry<String, DateRange> entry : getDateRanges(data.reportingDate).entrySet()) {
            double total = TrendCalculator.sum(data.dailyEarnings, entry.getValue().start, entry.getValue().end);
            data.periods.put(entry.getKey(), total);
            data.topSitesByPeriod.put(entry.getKey(), generateMockSites(total));
        }

        return data;
    }

    private List<SiteEarnings> generateMockSites(double periodTotal) {
        String[] names = new String[] {
            "example1.com", "tech-blog.io", "news-site.net", "tutorials.dev",
            "reviews.shop", "lifestyle.com", "gaming-hub.co"
        };
        double[] shares = new double[] { 0.28d, 0.22d, 0.16d, 0.11d, 0.08d, 0.05d, 0.03d };
        List<SiteEarnings> sites = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            sites.add(new SiteEarnings(names[i], Math.round(periodTotal * shares[i] * 100d) / 100d));
        }
        return sites;
    }

    private void displayData(ReportData data, String source, String mode) {
        displayDashboardData(data, source, mode, true);
    }

    private void displayDashboardData(ReportData data, String source, String mode, boolean refreshComplete) {
        lastData = data;
        currentCurrency = normalizeCurrencyCode(data.currency);
        SharedPreferences.Editor editor = prefs.edit().putString(PREF_CURRENCY, currentCurrency);
        if (data.accountName != null && data.accountName.length() > 0) {
            editor
                .putString(PREF_ADSENSE_ACCOUNT_NAME, data.accountName)
                .putString(PREF_ADSENSE_ACCOUNT_DISPLAY_NAME, data.accountDisplayName)
                .putString(PREF_ADSENSE_TIME_ZONE, data.reportingZoneId);
        }
        editor.apply();

        double today = getPeriodAmount("today");
        double yesterday = getPeriodAmount("yesterday");
        todayAmount.setText(formatCurrency(today));
        renderDailyChange(today, yesterday);
        renderProjection(getPeriodAmount("month"), today, data.reportingDate);
        updateModeBadge(data.warning == null || data.warning.length() == 0 ? mode : "Needs attention");
        connectButton.setText("Connect");
        updateActionVisibility();
        setStatus(data.warning == null ? "" : data.warning, false);
        saveWidgetSnapshot(source);
        switchPeriod(currentPeriod);
        renderTrend();
        if (refreshComplete) {
            setSyncStatus(formatUpdatedStatus(data));
            scheduleNextRefresh();
        }
    }

    private void displayEmptyState() {
        todayAmount.setText("—");
        todayChange.setText("No comparison yet");
        projectionAmount.setText("—");
        projectionMeta.setText("Based on daily average");
        selectedPeriodLabel.setText(periodLabel(currentPeriod));
        selectedPeriodAmount.setText("—");
        selectedPeriodComparison.setText("");
        selectedPeriodComparison.setVisibility(View.GONE);
        sitesTotal.setText("No data");
        sitesList.removeAllViews();
        trendTotal.setText("—");
        trendComparison.setText("No comparable trend yet");
        trendComparison.setTextColor(TEXT_SECONDARY);
        trendMeta.setText("Completed days · tap the chart for details");
        trendView.setSeries(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), currentCurrency);
        setSyncStatus("No earnings loaded");
        saveWidgetSnapshot("Open app to refresh");
    }

    private void saveWidgetSnapshot(String source) {
        try {
            prefs.edit()
                .putString(PREF_WIDGET_TODAY_AMOUNT, todayAmount.getText().toString())
                .putString(PREF_WIDGET_TODAY_CHANGE, todayChange.getText().toString())
                .putString(PREF_WIDGET_PROJECTION_AMOUNT, projectionAmount.getText().toString())
                .putString(PREF_WIDGET_PROJECTION_META, projectionMeta.getText().toString())
                .putString(PREF_WIDGET_SOURCE, source == null ? "" : source)
                .apply();
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not persist the widget snapshot.", error);
        }

        try {
            EarningsCardWidgetProvider.updateAll(this);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not update the card widget.", error);
        }

        try {
            EarningsMiniWidgetProvider.updateAll(this);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not update the mini widget.", error);
        }
    }

    private void switchPeriod(String periodKey) {
        currentPeriod = periodKey;
        for (Map.Entry<String, TextView> entry : periodButtons.entrySet()) {
            boolean active = entry.getKey().equals(periodKey);
            entry.getValue().setSelected(active);
            entry.getValue().setContentDescription(periodLabel(entry.getKey()) + (active ? ", selected" : ""));
            entry.getValue().setTextColor(active ? Color.WHITE : TEXT_PRIMARY);
            entry.getValue().setBackground(buttonBackground(active ? ACCENT_STRONG : BG_SECONDARY, active ? ACCENT_STRONG : BORDER));
        }

        selectedPeriodLabel.setText(periodLabel(periodKey));
        if (lastData == null) {
            selectedPeriodAmount.setText("—");
            selectedPeriodComparison.setText("");
            selectedPeriodComparison.setVisibility(View.GONE);
            renderTopSites(periodKey);
            return;
        }
        selectedPeriodAmount.setText(formatCurrency(getPeriodAmount(periodKey)));
        renderSelectedPeriodComparison(periodKey);
        renderTopSites(periodKey);
    }

    private void renderSelectedPeriodComparison(String periodKey) {
        if (selectedPeriodComparison == null) {
            return;
        }

        if ("today".equals(periodKey)) {
            setProgressText(selectedPeriodComparison, getPeriodAmount("today"), getPeriodAmount("yesterday"), "yesterday");
            return;
        }

        if (("week".equals(periodKey)
            || "month".equals(periodKey)
            || "year".equals(periodKey)
            || "days30".equals(periodKey))
            && lastData != null
            && !lastData.dailyEarnings.isEmpty()) {
            try {
                DayOfWeek weekStart = "sunday".equals(lastData.weekStartSetting)
                    ? DayOfWeek.SUNDAY
                    : DayOfWeek.MONDAY;
                TrendCalculator.Comparison comparison = TrendCalculator.compare(
                    lastData.dailyEarnings,
                    periodKey,
                    lastData.reportingDate == null ? LocalDate.now() : lastData.reportingDate,
                    weekStart
                );
                if (amountsMatch(getPeriodAmount(periodKey), comparison.getCurrentTotal())) {
                    setComparisonText(
                        selectedPeriodComparison,
                        comparison.getCurrentTotal(),
                        comparison.getPreviousTotal(),
                        comparison.getComparisonLabel() + " · today in progress"
                    );
                } else {
                    selectedPeriodComparison.setText("Comparison pending a complete daily refresh");
                    selectedPeriodComparison.setTextColor(TEXT_SECONDARY);
                    selectedPeriodComparison.setVisibility(View.VISIBLE);
                }
                return;
            } catch (RuntimeException error) {
                Log.w(TAG, "Could not render the selected-period comparison.", error);
            }
        }

        selectedPeriodComparison.setText("");
        selectedPeriodComparison.setVisibility(View.GONE);
    }

    private void setProgressText(TextView view, double current, double previous, String label) {
        if (previous <= 0d) {
            view.setText("No comparable earnings");
        } else {
            double progress = (current / previous) * 100d;
            view.setText(
                String.format(Locale.US, "%.0f%%", progress)
                    + " of " + label + " · " + formatCurrency(previous)
            );
        }
        view.setTextColor(TEXT_SECONDARY);
        view.setVisibility(View.VISIBLE);
    }

    private void setComparisonText(TextView view, double current, double previous, String label) {
        if (previous == 0) {
            view.setText("No comparable earnings");
            view.setTextColor(TEXT_SECONDARY);
            view.setVisibility(View.VISIBLE);
            return;
        }
        double percent = ((current - previous) / Math.abs(previous)) * 100;
        String prefix = percent > 0 ? "+" : "";
        view.setText(
            prefix + String.format(Locale.US, "%.1f", percent) + "% " + label
                + " · " + formatCurrency(previous)
        );
        view.setTextColor(percent > 0 ? SUCCESS : percent < 0 ? ERROR_TEXT : TEXT_SECONDARY);
        view.setVisibility(View.VISIBLE);
    }

    private boolean amountsMatch(double first, double second) {
        double tolerance = Math.max(0.01d, Math.max(Math.abs(first), Math.abs(second)) * 0.000001d);
        return Math.abs(first - second) <= tolerance;
    }

    private void selectTrendPeriod(String periodKey) {
        currentTrendPeriod = "days7".equals(periodKey) ? "days7" : "days30";
        for (Map.Entry<String, TextView> entry : trendButtons.entrySet()) {
            boolean active = entry.getKey().equals(currentTrendPeriod);
            entry.getValue().setSelected(active);
            entry.getValue().setContentDescription(entry.getValue().getText() + (active ? ", selected" : ""));
            entry.getValue().setTextColor(active ? Color.WHITE : TEXT_PRIMARY);
            entry.getValue().setBackground(buttonBackground(
                active ? ACCENT_STRONG : BG_SECONDARY,
                active ? ACCENT_STRONG : BORDER
            ));
        }
        renderTrend();
    }

    private void renderTrend() {
        if (trendView == null || trendTotal == null || trendComparison == null || trendMeta == null) {
            return;
        }
        if (lastData == null || lastData.dailyEarnings.isEmpty()) {
            trendView.setSeries(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), currentCurrency);
            trendTotal.setText("—");
            trendComparison.setText("Daily trend will appear after refresh");
            trendComparison.setTextColor(TEXT_SECONDARY);
            trendMeta.setText("Completed days · tap the chart for details");
            return;
        }

        try {
            int windowDays = "days7".equals(currentTrendPeriod) ? 7 : 30;
            List<TrendAnalytics.DailyPoint> points = new ArrayList<>();
            for (Map.Entry<LocalDate, Double> entry : lastData.dailyEarnings.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && Double.isFinite(entry.getValue())) {
                    points.add(new TrendAnalytics.DailyPoint(entry.getKey(), entry.getValue()));
                }
            }
            LocalDate anchorDate = lastData.reportingDate == null ? LocalDate.now() : lastData.reportingDate;
            TrendAnalytics.TrendSummary summary = TrendAnalytics.analyze(
                points,
                anchorDate,
                windowDays,
                true
            );

            List<Double> currentValues = new ArrayList<>();
            List<Double> previousValues = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (TrendAnalytics.DailyPoint point : summary.getCurrentSeries()) {
                currentValues.add(point.getValue());
                labels.add(point.getDate().format(TREND_LABEL_FORMAT));
            }
            for (TrendAnalytics.DailyPoint point : summary.getPreviousSeries()) {
                previousValues.add(point.getValue());
            }

            trendView.setSeries(
                currentValues,
                previousValues,
                labels,
                currentCurrency
            );
            trendTotal.setText(formatCurrency(summary.getCurrentTotal()));
            OptionalDouble percentage = summary.getPercentageDelta();
            if (!percentage.isPresent()) {
                trendComparison.setText("Previous " + windowDays + " days had no earnings");
                trendComparison.setTextColor(TEXT_SECONDARY);
            } else {
                double percent = percentage.getAsDouble();
                String prefix = percent > 0 ? "+" : "";
                trendComparison.setText(
                    prefix + String.format(Locale.US, "%.1f", percent) + "% vs previous "
                        + windowDays + " complete days · " + formatCurrency(summary.getPreviousTotal())
                );
                trendComparison.setTextColor(percent > 0 ? SUCCESS : percent < 0 ? ERROR_TEXT : TEXT_SECONDARY);
            }

            TrendAnalytics.DailyPoint bestDay = summary.getBestDay();
            LocalDate currentStart = summary.getCurrentSeries().get(0).getDate();
            LocalDate currentEnd = summary.getCurrentSeries().get(summary.getCurrentSeries().size() - 1).getDate();
            trendMeta.setText(
                currentStart.format(TREND_LABEL_FORMAT) + " – " + currentEnd.format(TREND_LABEL_FORMAT)
                    + " · Average " + formatCurrency(summary.getDailyAverage()) + "/day · Best "
                    + bestDay.getDate().format(TREND_LABEL_FORMAT) + " " + formatCurrency(bestDay.getValue())
            );
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not render the earnings trend.", error);
            trendView.setSeries(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), currentCurrency);
            trendTotal.setText("—");
            trendComparison.setText("Trend unavailable");
            trendComparison.setTextColor(ERROR_TEXT);
            trendMeta.setText("Refresh to try again");
        }
    }

    private void renderDailyChange(double today, double yesterday) {
        if (yesterday == 0) {
            todayChange.setText("Yesterday has no comparable earnings");
            return;
        }

        double progress = (today / yesterday) * 100d;
        todayChange.setText(
            String.format(Locale.US, "%.0f%%", progress)
                + " of yesterday · Yesterday " + formatCurrency(yesterday)
        );
    }

    private void renderProjection(double monthAmount, double todayAmount, LocalDate reportingDate) {
        LocalDate today = reportingDate == null ? LocalDate.now() : reportingDate;
        int elapsedDays = Math.max(1, today.getDayOfMonth());
        int daysInMonth = YearMonth.from(today).lengthOfMonth();
        int completedDays = Math.max(0, elapsedDays - 1);
        double completedEarnings = Math.max(0d, monthAmount - Math.max(0d, todayAmount));
        double dailyAverage = completedDays > 0 ? completedEarnings / completedDays : Math.max(0d, todayAmount);
        double projected = monthAmount + dailyAverage * Math.max(0, daysInMonth - elapsedDays);
        projectionAmount.setText(formatCurrency(projected));
        projectionMeta.setText(
            completedDays > 0
                ? "Based on " + completedDays + " complete days · " + formatCurrency(dailyAverage) + "/day"
                : "Early estimate · " + formatCurrency(dailyAverage) + "/day"
        );
    }

    private void renderTopSites(String periodKey) {
        sitesList.removeAllViews();
        if (lastData == null) {
            sitesTotal.setText("No data");
            TextView empty = text("Site data will appear after refresh.", 13, TEXT_SECONDARY, Typeface.NORMAL);
            empty.setPadding(dp(2), dp(12), dp(2), dp(4));
            sitesList.addView(empty);
            return;
        }
        List<SiteEarnings> sites = lastData == null ? null : lastData.topSitesByPeriod.get(periodKey);
        if (sites == null) {
            sites = new ArrayList<>();
        }

        double topSitesTotal = 0;
        int count = Math.min(7, sites.size());
        for (int i = 0; i < count; i++) {
            topSitesTotal += sites.get(i).earnings;
        }
        double periodTotal = getPeriodAmount(periodKey);
        double shareBase = periodTotal > 0 ? periodTotal : topSitesTotal;
        double coverage = periodTotal > 0 ? Math.max(0d, Math.min(1d, topSitesTotal / periodTotal)) : 0d;
        sitesTotal.setText(
            "Top 7 " + formatCurrency(topSitesTotal)
                + (periodTotal > 0 ? " · " + String.format(Locale.US, "%.0f%%", coverage * 100d) : "")
        );

        if (count == 0) {
            TextView empty = text("No site data for this period.", 13, Color.rgb(71, 85, 105), Typeface.NORMAL);
            empty.setPadding(dp(2), dp(12), dp(2), dp(4));
            sitesList.addView(empty);
            return;
        }

        for (int i = 0; i < count; i++) {
            SiteEarnings site = sites.get(i);
            LinearLayout row = vertical();
            row.setPadding(dp(2), dp(12), dp(2), dp(12));
            LinearLayout content = horizontal();
            content.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(content);

            TextView rank = text(String.valueOf(i + 1), 12, TEXT_SECONDARY, Typeface.BOLD);
            rank.setGravity(Gravity.CENTER_VERTICAL);
            content.addView(rank, new LinearLayout.LayoutParams(dp(24), ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView name = text(site.name, 14, TEXT_PRIMARY, Typeface.NORMAL);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            content.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            double share = shareBase <= 0 ? 0 : Math.max(0, Math.min(1, site.earnings / shareBase));
            TextView value = text(
                formatCurrency(site.earnings) + " · " + String.format(Locale.US, "%.0f", share * 100) + "%",
                13,
                TEXT_PRIMARY,
                Typeface.BOLD
            );
            value.setGravity(Gravity.END);
            content.addView(value);
            row.setContentDescription(
                "Rank " + (i + 1) + ", " + site.name + ", " + formatCurrency(site.earnings)
                    + ", " + String.format(Locale.US, "%.0f percent of period", share * 100d)
            );

            LinearLayout shareBar = horizontal();
            shareBar.setBackground(buttonBackground(BG_TERTIARY, 0, dp(999)));
            LinearLayout.LayoutParams shareBarParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5));
            shareBarParams.setMargins(dp(24), dp(8), 0, 0);
            row.addView(shareBar, shareBarParams);

            View fill = new View(this);
            fill.setBackground(buttonBackground(ACCENT_STRONG, 0, dp(999)));
            shareBar.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) share));
            View remainder = new View(this);
            shareBar.addView(remainder, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) (1 - share)));

            sitesList.addView(row, matchWrap());
            if (i < count - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(BORDER);
                sitesList.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }

        double otherEarnings = Math.max(0d, periodTotal - topSitesTotal);
        if (otherEarnings > 0.005d) {
            View divider = new View(this);
            divider.setBackgroundColor(BORDER);
            sitesList.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            LinearLayout otherRow = horizontal();
            otherRow.setGravity(Gravity.CENTER_VERTICAL);
            otherRow.setPadding(dp(26), dp(12), dp(2), dp(4));
            otherRow.addView(
                text("Other sites", 13, TEXT_SECONDARY, Typeface.NORMAL),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)
            );
            double otherShare = periodTotal > 0 ? otherEarnings / periodTotal : 0d;
            TextView otherValue = text(
                formatCurrency(otherEarnings) + " · " + String.format(Locale.US, "%.0f%%", otherShare * 100d),
                12,
                TEXT_SECONDARY,
                Typeface.BOLD
            );
            otherValue.setGravity(Gravity.END);
            otherRow.addView(otherValue);
            sitesList.addView(otherRow, matchWrap());
        }
    }

    private double getPeriodAmount(String periodKey) {
        if (lastData == null) {
            return 0;
        }
        Double amount = lastData.periods.get(periodKey);
        return amount == null ? 0 : amount;
    }

    private double fallbackPeriodAmount(ReportData fallbackData, String periodKey) {
        return fallbackData == null || fallbackData.periods.get(periodKey) == null
            ? 0
            : fallbackData.periods.get(periodKey);
    }

    private boolean isFallbackCompatible(ReportData fallbackData, ReportData targetData, String requestedCurrency) {
        if (fallbackData == null || targetData == null || targetData.reportingDate == null) {
            return false;
        }
        return targetData.accountName.equals(fallbackData.accountName)
            && normalizeCurrencyCode(requestedCurrency).equals(normalizeCurrencyCode(fallbackData.currency))
            && targetData.reportingZoneId.equals(fallbackData.reportingZoneId)
            && targetData.reportingDate.equals(fallbackData.reportingDate)
            && targetData.weekStartSetting.equals(fallbackData.weekStartSetting);
    }

    private List<SiteEarnings> fallbackTopSites(ReportData fallbackData, String periodKey) {
        if (fallbackData == null || fallbackData.topSitesByPeriod.get(periodKey) == null) {
            return new ArrayList<>();
        }
        List<SiteEarnings> copy = new ArrayList<>();
        for (SiteEarnings site : fallbackData.topSitesByPeriod.get(periodKey)) {
            if (site != null) {
                copy.add(new SiteEarnings(site.name, site.earnings));
            }
        }
        return copy;
    }

    private String formatPartialRefreshWarning(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        StringBuilder message = new StringBuilder("Partial refresh: ");
        int shown = Math.min(3, items.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                message.append(", ");
            }
            message.append(items.get(i));
        }
        if (items.size() > shown) {
            message.append(" and ").append(items.size() - shown).append(" more");
        }
        return message.append(" could not be updated.").toString();
    }

    private void showSettingsDialog() {
        LinearLayout form = vertical();
        form.setPadding(dp(18), dp(8), dp(18), 0);

        Spinner currencySpinner = new Spinner(this);
        List<String> currencyLabels = new ArrayList<>();
        int selectedCurrency = 0;
        for (int i = 0; i < CURRENCIES.length; i++) {
            CurrencyOption option = CURRENCIES[i];
            currencyLabels.add(option.code + " - " + option.name);
            if (option.code.equals(currentCurrency)) {
                selectedCurrency = i;
            }
        }
        currencySpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, currencyLabels));
        currencySpinner.setSelection(selectedCurrency);
        form.addView(settingLabel("Currency"));
        form.addView(currencySpinner);

        Spinner weekStartSpinner = new Spinner(this);
        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[] { "Monday", "Sunday" });
        weekStartSpinner.setAdapter(weekAdapter);
        weekStartSpinner.setSelection("sunday".equals(prefs.getString(PREF_WEEK_START, "monday")) ? 1 : 0);
        form.addView(settingLabel("Week starts on"));
        form.addView(weekStartSpinner);

        EditText refreshInput = new EditText(this);
        refreshInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        refreshInput.setText(String.valueOf(getRefreshInterval()));
        form.addView(settingLabel("Refresh interval while app is open"));
        form.addView(refreshInput);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle("Settings")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (dialog, which) -> {
                CurrencyOption selected = CURRENCIES[currencySpinner.getSelectedItemPosition()];
                int refresh = clamp(parseInt(refreshInput.getText().toString(), 10), 5, 60);
                currentCurrency = selected.code;
                prefs.edit()
                    .putString(PREF_CURRENCY, selected.code)
                    .putString(PREF_WEEK_START, weekStartSpinner.getSelectedItemPosition() == 1 ? "sunday" : "monday")
                    .putInt(PREF_REFRESH, refresh)
                    .apply();
                cancelActiveRefresh(true);
                loadData();
            });

        if (account != null) {
            builder.setNeutralButton("Disconnect", (dialog, which) -> disconnectGoogle());
        }

        builder.show();
    }

    private void scheduleNextRefresh() {
        if (refreshRunnable != null) {
            mainHandler.removeCallbacks(refreshRunnable);
        }
        refreshRunnable = () -> {
            if (lastData != null || account != null) {
                loadData();
            }
        };
        mainHandler.postDelayed(refreshRunnable, getRefreshInterval() * 60L * 1000L);
    }

    private int getRefreshInterval() {
        return clamp(prefs.getInt(PREF_REFRESH, 10), 5, 60);
    }

    private List<SiteEarnings> mergeTopSites(List<SiteEarnings> sites) {
        List<TopSitesMerger.Entry> rawEntries = new ArrayList<>();
        for (SiteEarnings site : sites) {
            rawEntries.add(new TopSitesMerger.Entry(site.name, site.earnings));
        }

        List<SiteEarnings> result = new ArrayList<>();
        for (TopSitesMerger.Entry entry : TopSitesMerger.mergeAliases(rawEntries)) {
            result.add(new SiteEarnings(entry.getName(), entry.getEarnings()));
        }
        return result;
    }

    private String formatCurrency(double amount) {
        try {
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
            format.setCurrency(Currency.getInstance(currentCurrency));
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            return format.format(amount);
        } catch (IllegalArgumentException error) {
            return String.format(Locale.US, "%.2f %s", amount, currentCurrency);
        }
    }

    private String normalizeCurrencyCode(String currencyCode) {
        if (currencyCode == null) {
            return "EUR";
        }
        for (CurrencyOption option : CURRENCIES) {
            if (option.code.equals(currencyCode)) {
                return option.code;
            }
        }
        return "EUR";
    }

    private String periodLabel(String periodKey) {
        for (Period period : PERIODS) {
            if (period.key.equals(periodKey)) {
                return period.label;
            }
        }
        return "Today";
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void setStatus(String text) {
        setStatus(text, false);
    }

    private void setStatus(String text, boolean error) {
        if (statusBox != null) {
            statusBox.setVisibility(text == null || text.length() == 0 ? View.GONE : View.VISIBLE);
            statusBox.setBackground(buttonBackground(
                error ? ERROR_SOFT : WARNING_SOFT,
                error ? ERROR_BORDER : 0xFFFDBA74
            ));
        }
        if (statusText != null) {
            statusText.setTextColor(error ? ERROR_TEXT : WARNING_TEXT);
            statusText.setText(text == null ? "" : text);
        }
    }

    private void setSyncStatus(String text) {
        if (syncStatusText != null) {
            syncStatusText.setText(text == null ? "" : text);
        }
    }

    private String formatUpdatedStatus(ReportData data) {
        long timestamp = data.updatedAtEpochMs > 0 ? data.updatedAtEpochMs : System.currentTimeMillis();
        ZoneId reportingZone = parseZoneId(data.reportingZoneId);
        String time = Instant.ofEpochMilli(timestamp)
            .atZone(reportingZone)
            .format(UPDATED_TIME_FORMAT);
        String accountName = data.accountDisplayName == null ? "" : data.accountDisplayName.trim();
        String state = data.warning == null || data.warning.length() == 0 ? "All data ready" : "Some cached data";
        return accountName.length() == 0
            ? "Updated " + time + " · " + state
            : "Updated " + time + " · " + state + " · " + accountName;
    }

    private void updateModeBadge(String text) {
        modeBadge.setText(text);
        boolean live = "Live mode".equals(text);
        boolean warning = "Needs attention".equals(text);
        modeBadge.setTextColor(live ? SUCCESS : warning ? WARNING_TEXT : TEXT_SECONDARY);
        modeBadge.setBackground(buttonBackground(live ? SUCCESS_SOFT : warning ? WARNING_SOFT : BG_SECONDARY, live || warning ? 0x00000000 : BORDER, dp(999)));
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        if (trendView != null) {
            trendView.setLoading(loading && (lastData == null || lastData.dailyEarnings.isEmpty()));
        }
        if (!loading && pullRefreshActive) {
            resetPullRefreshIndicator();
        }
        if (refreshButton == null || refreshGlyph == null) {
            return;
        }

        refreshButton.setEnabled(!loading);
        refreshButton.setAlpha(loading ? 0.72f : 1f);

        if (loading) {
            if (refreshAnimator == null) {
                refreshAnimator = ObjectAnimator.ofFloat(refreshGlyph, View.ROTATION, 0f, 360f);
                refreshAnimator.setDuration(850);
                refreshAnimator.setRepeatCount(ValueAnimator.INFINITE);
                refreshAnimator.setInterpolator(new LinearInterpolator());
            }
            if (!refreshAnimator.isRunning()) {
                refreshGlyph.setRotation(0f);
                refreshAnimator.start();
            }
            return;
        }

        if (refreshAnimator != null) {
            refreshAnimator.cancel();
        }
        refreshGlyph.setRotation(0f);
    }

    private void updateActionVisibility() {
        boolean connected = account != null;
        if (actionRow != null) {
            actionRow.setVisibility(connected ? View.GONE : View.VISIBLE);
        }
        if (connectButton != null) {
            connectButton.setVisibility(connected ? View.GONE : View.VISIBLE);
        }
        if (demoButton != null) {
            demoButton.setVisibility(connected ? View.GONE : View.VISIBLE);
        }
    }

    private void applySystemInsets(View view) {
        int baseLeft = dp(16);
        int baseTop = dp(14);
        int baseRight = dp(16);
        int baseBottom = dp(24);

        view.setPadding(baseLeft, baseTop, baseRight, baseBottom);
        view.setOnApplyWindowInsetsListener((target, insets) -> {
            target.setPadding(
                baseLeft + insets.getSystemWindowInsetLeft(),
                baseTop + insets.getSystemWindowInsetTop(),
                baseRight + insets.getSystemWindowInsetRight(),
                baseBottom + insets.getSystemWindowInsetBottom()
            );
            return insets;
        });
        view.requestApplyInsets();
    }

    private TextView settingLabel(String text) {
        TextView label = text(text, 12, Color.rgb(71, 85, 105), Typeface.BOLD);
        label.setPadding(0, dp(12), 0, dp(4));
        return label;
    }

    private TextView text(String text, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setIncludeFontPadding(true);
        return view;
    }

    private TextView badgeText(String value) {
        TextView badge = text(value, 11, TEXT_SECONDARY, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(7), dp(2), dp(7), dp(2));
        badge.setMinWidth(0);
        badge.setMinimumWidth(0);
        badge.setBackground(buttonBackground(BG_SECONDARY, BORDER, dp(999)));
        badge.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return badge;
    }

    private FrameLayout headerIconButton(int drawableRes, boolean isRefresh) {
        FrameLayout button = new FrameLayout(this);
        button.setBackground(buttonBackground(BG_SECONDARY, BORDER));
        button.setContentDescription(isRefresh ? "Refresh earnings" : "Open settings");
        button.setFocusable(true);
        button.setClickable(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(48));
        params.setMargins(dp(7), 0, 0, 0);
        button.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageResource(drawableRes);
        icon.setColorFilter(TEXT_PRIMARY);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER);
        button.addView(icon, iconParams);

        if (isRefresh) {
            refreshGlyph = icon;
        }

        return button;
    }

    private TextView commandButton(String value, boolean primary) {
        TextView button = text(value, 12, primary ? Color.WHITE : TEXT_PRIMARY, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(buttonBackground(primary ? ACCENT_STRONG : BG_SECONDARY, primary ? ACCENT_STRONG : BORDER));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        params.setMargins(0, 0, dp(8), 0);
        button.setLayoutParams(params);
        return button;
    }

    private TextView periodButton(String value) {
        TextView button = text(value, 12, TEXT_PRIMARY, Typeface.BOLD);
        button.setFocusable(true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(buttonBackground(BG_SECONDARY, BORDER));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        params.setMargins(0, 0, dp(8), 0);
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout card(int background, int stroke) {
        LinearLayout layout = vertical();
        layout.setPadding(dp(14), dp(14), dp(14), dp(14));
        layout.setBackground(buttonBackground(background, stroke));
        return layout;
    }

    private GradientDrawable buttonBackground(int background, int stroke) {
        return buttonBackground(background, stroke, dp(14));
    }

    private GradientDrawable buttonBackground(int background, int stroke, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(background);
        drawable.setCornerRadius(radius);
        if (Color.alpha(stroke) > 0) {
            drawable.setStroke(dp(1), stroke);
        }
        return drawable;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapWithTop(int top) {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(top), 0, 0);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class PullRefreshScrollView extends ScrollView {
        PullRefreshScrollView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            boolean wasPulling = isPullingToRefresh;
            boolean handled = handlePullToRefreshTouch(this, event);
            if (handled && !wasPulling && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
            }
            if (handled && event.getActionMasked() == MotionEvent.ACTION_UP) {
                performClick();
            }
            return handled || super.dispatchTouchEvent(event);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }
    }

    private static class CurrencyOption {
        final String code;
        final String name;

        CurrencyOption(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    private static class Period {
        final String key;
        final String label;

        Period(String key, String label) {
            this.key = key;
            this.label = label;
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

    private static class SiteEarnings {
        final String name;
        double earnings;

        SiteEarnings(String name, double earnings) {
            this.name = name;
            this.earnings = earnings;
        }
    }

    private static class ReportData {
        final Map<String, Double> periods = new HashMap<>();
        final Map<String, List<SiteEarnings>> topSitesByPeriod = new HashMap<>();
        final Map<LocalDate, Double> dailyEarnings = new LinkedHashMap<>();
        String currency = "EUR";
        String source = "";
        String accountName = "";
        String accountDisplayName = "";
        String reportingZoneId = "";
        String weekStartSetting = "monday";
        LocalDate reportingDate;
        long updatedAtEpochMs;
        String warning = "";

        ReportData copy() {
            ReportData copy = new ReportData();
            copy.periods.putAll(periods);
            copy.dailyEarnings.putAll(dailyEarnings);
            for (Map.Entry<String, List<SiteEarnings>> entry : topSitesByPeriod.entrySet()) {
                List<SiteEarnings> sites = new ArrayList<>();
                if (entry.getValue() != null) {
                    for (SiteEarnings site : entry.getValue()) {
                        sites.add(new SiteEarnings(site.name, site.earnings));
                    }
                }
                copy.topSitesByPeriod.put(entry.getKey(), sites);
            }
            copy.currency = currency;
            copy.source = source;
            copy.accountName = accountName;
            copy.accountDisplayName = accountDisplayName;
            copy.reportingZoneId = reportingZoneId;
            copy.weekStartSetting = weekStartSetting;
            copy.reportingDate = reportingDate;
            copy.updatedAtEpochMs = updatedAtEpochMs;
            copy.warning = warning;
            return copy;
        }
    }
}
