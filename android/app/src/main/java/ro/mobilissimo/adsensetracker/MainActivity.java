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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private static final String PREFS = "adsense_tracker";
    private static final String PREF_CURRENCY = "currencyCode";
    private static final String PREF_ADSENSE_ACCOUNT_NAME = "adsenseAccountName";
    private static final String PREF_ADSENSE_ACCOUNT_DISPLAY_NAME = "adsenseAccountDisplayName";
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
    private static final int ACCENT = 0xFF4285F4;
    private static final int ACCENT_STRONG = 0xFF2563EB;
    private static final int SUCCESS = 0xFF16A34A;
    private static final int SUCCESS_SOFT = 0xFFDCFCE7;
    private static final int WARNING_TEXT = 0xFF92400E;
    private static final int WARNING_SOFT = 0xFFFEF3C7;

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
        new Period("days356", "Last 356 Days")
    };

    private SharedPreferences prefs;
    private GoogleSignInClient signInClient;
    private GoogleSignInAccount account;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, TextView> periodButtons = new HashMap<>();
    private final Object connectionLock = new Object();
    private final RefreshRequestTracker refreshRequests = new RefreshRequestTracker();

    private String currentCurrency = "EUR";
    private String currentPeriod = "today";
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
    private float pullRefreshStartY;
    private int pullRefreshTouchSlop;

    private TextView modeBadge;
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
            setStatus("Connect Google or use demo data.");
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
        scrollView.setBackgroundColor(BG_PRIMARY);
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

        LinearLayout headerActions = horizontal();
        headerActions.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(headerActions);

        refreshButton = headerIconButton(R.drawable.ic_refresh, true);
        refreshButton.setOnClickListener(view -> loadData());
        headerActions.addView(refreshButton);

        FrameLayout settingsButton = headerIconButton(R.drawable.ic_settings, false);
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

        LinearLayout highlight = card(ACCENT, ACCENT);
        highlight.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.addView(highlight, matchWrapWithTop(12));
        TextView todayLabel = text("Today's earnings", 12, Color.WHITE, Typeface.BOLD);
        highlight.addView(todayLabel);
        todayAmount = text(formatCurrency(0), 34, Color.WHITE, Typeface.BOLD);
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
        projectionAmount = text(formatCurrency(0), 20, Color.WHITE, Typeface.BOLD);
        projectionAmount.setGravity(Gravity.RIGHT);
        projectionRow.addView(projectionAmount);

        HorizontalScrollView tabScroller = new HorizontalScrollView(this);
        tabScroller.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = horizontal();
        tabScroller.addView(tabs);
        root.addView(tabScroller, matchWrapWithTop(14));

        for (Period period : PERIODS) {
            TextView tab = periodButton(period.label);
            tab.setOnClickListener(view -> switchPeriod(period.key));
            periodButtons.put(period.key, tab);
            tabs.addView(tab);
        }

        LinearLayout periodCard = card(BG_PRIMARY, BORDER);
        root.addView(periodCard, matchWrapWithTop(12));
        selectedPeriodLabel = text("Today", 13, TEXT_SECONDARY, Typeface.BOLD);
        periodCard.addView(selectedPeriodLabel);
        selectedPeriodAmount = text(formatCurrency(0), 22, TEXT_PRIMARY, Typeface.BOLD);
        selectedPeriodAmount.setGravity(Gravity.RIGHT);
        periodCard.addView(selectedPeriodAmount);

        LinearLayout sitesHeader = horizontal();
        sitesHeader.setPadding(0, dp(18), 0, dp(8));
        root.addView(sitesHeader, matchWrap());
        sitesHeader.addView(text("Top 7 Sites", 14, TEXT_PRIMARY, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sitesTotal = text(formatCurrency(0) + " total", 11, TEXT_SECONDARY, Typeface.BOLD);
        sitesTotal.setGravity(Gravity.RIGHT);
        sitesHeader.addView(sitesTotal);

        sitesList = vertical();
        root.addView(sitesList, matchWrap());

        statusBox = card(WARNING_SOFT, 0xFFFDBA74);
        statusBox.setVisibility(View.GONE);
        root.addView(statusBox, matchWrapWithTop(14));
        statusText = text("", 12, WARNING_TEXT, Typeface.BOLD);
        statusBox.addView(statusText);

        switchPeriod(currentPeriod);
    }

    private boolean handlePullToRefreshTouch(ScrollView scrollView, MotionEvent event) {
        if (pullRefreshIndicator == null) {
            return false;
        }

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            pullRefreshStartY = event.getY();
            isPullingToRefresh = false;
            pullRefreshArmed = false;
            return false;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            float deltaY = event.getY() - pullRefreshStartY;
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
            updateModeBadge("Not connected");
            return;
        }

        if (modeBadge.getText() == null || modeBadge.getText().length() == 0 || "Not connected".contentEquals(modeBadge.getText())) {
            updateModeBadge("Live mode");
        }
        setStatus("");
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
                    ReportData data = fetchFromAdSenseApi(token, refreshCurrency, fallbackData);
                    runOnUiThreadIfActive(generation, () -> applyRefreshSuccess(data));
                } catch (UserRecoverableAuthException recoverable) {
                    runOnUiThreadIfActive(generation, () -> {
                        setLoading(false);
                        startActivityForResult(recoverable.getIntent(), RC_AUTH_RECOVERY);
                    });
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    runOnUiThreadIfActive(generation, () -> setLoadingSafely(false));
                } catch (Exception error) {
                    runOnUiThreadIfActive(generation, () -> applyRefreshFailure(error));
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
            activeRefresh = null;
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
            setStatus(message == null || message.length() == 0 ? "Failed to load AdSense data." : message);
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

    private ReportData fetchFromAdSenseApi(String token, String requestedCurrency, ReportData fallbackData) throws IOException, JSONException {
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

        Map<String, DateRange> ranges = getDateRanges();
        ReportData result = new ReportData();
        result.currency = requestedCurrency;
        result.accountName = accountName;
        result.accountDisplayName = accountJson.optString("displayName", accountName);
        result.source = "AdSense: " + result.accountDisplayName;
        List<String> refreshWarnings = new ArrayList<>();
        Exception firstPeriodError = null;
        int successfulPeriodReports = 0;

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
                if (firstPeriodError == null) {
                    firstPeriodError = error;
                }
                result.periods.put(entry.getKey(), fallbackPeriodAmount(fallbackData, entry.getKey()));
                refreshWarnings.add(periodLabel(entry.getKey()) + " total");
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
                result.topSitesByPeriod.put(entry.getKey(), fallbackTopSites(fallbackData, entry.getKey()));
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

        return mergeTopSites(sites);
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

    private Map<String, DateRange> getDateRanges() {
        String weekStartSetting = prefs.getString(PREF_WEEK_START, "monday");
        LocalDate today = LocalDate.now();
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
        ranges.put("days356", new DateRange(today.minusDays(355), today));
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
        List<SiteEarnings> topSites = new ArrayList<>();
        topSites.add(new SiteEarnings("example1.com", 234.50));
        topSites.add(new SiteEarnings("tech-blog.io", 189.30));
        topSites.add(new SiteEarnings("news-site.net", 145.20));
        topSites.add(new SiteEarnings("tutorials.dev", 98.15));
        topSites.add(new SiteEarnings("reviews.shop", 76.45));
        topSites.add(new SiteEarnings("lifestyle.com", 54.30));
        topSites.add(new SiteEarnings("gaming-hub.co", 42.18));
        topSites.add(new SiteEarnings("fitness.app", 35.90));
        topSites.add(new SiteEarnings("travel.guide", 28.75));

        ReportData data = new ReportData();
        data.currency = currentCurrency;
        data.source = "Demo data";
        data.periods.put("today", 45.32);
        data.periods.put("yesterday", 40.12);
        data.periods.put("week", 298.45);
        data.periods.put("lastweek", 276.80);
        data.periods.put("month", 892.10);
        data.periods.put("lastmonth", 1100.00);
        data.periods.put("days30", 1245.60);
        data.periods.put("year", 5200.40);
        data.periods.put("lastyear", 18420.75);
        data.periods.put("days356", 17140.25);

        for (Period period : PERIODS) {
            data.topSitesByPeriod.put(period.key, topSites);
        }

        return data;
    }

    private void displayData(ReportData data, String source, String mode) {
        lastData = data;
        currentCurrency = normalizeCurrencyCode(data.currency);
        SharedPreferences.Editor editor = prefs.edit().putString(PREF_CURRENCY, currentCurrency);
        if (data.accountName != null && data.accountName.length() > 0) {
            editor
                .putString(PREF_ADSENSE_ACCOUNT_NAME, data.accountName)
                .putString(PREF_ADSENSE_ACCOUNT_DISPLAY_NAME, data.accountDisplayName);
        }
        editor.apply();

        double today = getPeriodAmount("today");
        double yesterday = getPeriodAmount("yesterday");
        todayAmount.setText(formatCurrency(today));
        renderDailyChange(today, yesterday);
        renderProjection(getPeriodAmount("month"));
        updateModeBadge(mode);
        connectButton.setText("Connect");
        updateActionVisibility();
        setStatus(data.warning != null && data.warning.length() > 0 ? data.warning : source);
        saveWidgetSnapshot(source);
        switchPeriod(currentPeriod);
        scheduleNextRefresh();
    }

    private void displayEmptyState() {
        todayAmount.setText(formatCurrency(0));
        todayChange.setText("No comparison yet");
        projectionAmount.setText(formatCurrency(0));
        projectionMeta.setText("Based on daily average");
        selectedPeriodLabel.setText(periodLabel(currentPeriod));
        selectedPeriodAmount.setText(formatCurrency(0));
        sitesTotal.setText(formatCurrency(0) + " total");
        sitesList.removeAllViews();
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
            entry.getValue().setTextColor(active ? Color.WHITE : TEXT_PRIMARY);
            entry.getValue().setBackground(buttonBackground(active ? ACCENT_STRONG : BG_SECONDARY, active ? ACCENT_STRONG : BORDER));
        }

        selectedPeriodLabel.setText(periodLabel(periodKey));
        selectedPeriodAmount.setText(formatCurrency(getPeriodAmount(periodKey)));
        renderTopSites(periodKey);
    }

    private void renderDailyChange(double today, double yesterday) {
        if (yesterday == 0) {
            todayChange.setText("No comparison yet");
            return;
        }

        double change = today - yesterday;
        double percent = Math.abs((change / yesterday) * 100);
        String prefix = change > 0 ? "+" : change < 0 ? "-" : "";
        if (change == 0) {
            todayChange.setText("No change vs yesterday");
        } else {
            todayChange.setText(prefix + formatCurrency(Math.abs(change)) + " (" + prefix + String.format(Locale.US, "%.1f", percent) + "%) vs yesterday");
        }
    }

    private void renderProjection(double monthAmount) {
        LocalDate today = LocalDate.now();
        int elapsedDays = Math.max(1, today.getDayOfMonth());
        int daysInMonth = YearMonth.from(today).lengthOfMonth();
        double dailyAverage = monthAmount / elapsedDays;
        double projected = dailyAverage * daysInMonth;
        projectionAmount.setText(formatCurrency(projected));
        projectionMeta.setText("Based on " + elapsedDays + "/" + daysInMonth + " days, " + formatCurrency(dailyAverage) + "/day");
    }

    private void renderTopSites(String periodKey) {
        sitesList.removeAllViews();
        List<SiteEarnings> sites = lastData == null ? null : lastData.topSitesByPeriod.get(periodKey);
        if (sites == null) {
            sites = new ArrayList<>();
        }

        double total = 0;
        int count = Math.min(7, sites.size());
        for (int i = 0; i < count; i++) {
            total += sites.get(i).earnings;
        }
        sitesTotal.setText(formatCurrency(total) + " total");

        if (count == 0) {
            TextView empty = text("No site data for this period.", 13, Color.rgb(71, 85, 105), Typeface.NORMAL);
            sitesList.addView(empty);
            return;
        }

        for (int i = 0; i < count; i++) {
            SiteEarnings site = sites.get(i);
            LinearLayout row = card(Color.WHITE, Color.rgb(226, 232, 240));
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout content = horizontal();
            content.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(content);

            TextView name = text(site.name, 14, Color.rgb(15, 23, 42), Typeface.BOLD);
            content.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView value = text(formatCurrency(site.earnings), 14, Color.rgb(15, 23, 42), Typeface.BOLD);
            value.setGravity(Gravity.RIGHT);
            content.addView(value);
            sitesList.addView(row, matchWrapWithTop(8));
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

    private List<SiteEarnings> fallbackTopSites(ReportData fallbackData, String periodKey) {
        if (fallbackData == null || fallbackData.topSitesByPeriod.get(periodKey) == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(fallbackData.topSitesByPeriod.get(periodKey));
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
        Map<String, SiteEarnings> merged = new HashMap<>();
        for (SiteEarnings site : sites) {
            String canonicalName = canonicalizeSiteName(site.name);
            String key = canonicalName.length() > 0 ? canonicalName : site.name.toLowerCase(Locale.US);
            SiteEarnings existing = merged.get(key);
            if (existing == null) {
                merged.put(key, new SiteEarnings(canonicalName.length() > 0 ? canonicalName : site.name, site.earnings));
            } else {
                existing.earnings = Math.max(existing.earnings, site.earnings);
            }
        }

        List<SiteEarnings> result = new ArrayList<>(merged.values());
        Collections.sort(result, (a, b) -> Double.compare(b.earnings, a.earnings));
        return result;
    }

    private String canonicalizeSiteName(String name) {
        String host = name == null ? "" : name.trim()
            .replaceFirst("(?i)^https?://", "")
            .split("/")[0]
            .split("\\?")[0]
            .replaceFirst("(?i)^www\\.", "")
            .replaceFirst("\\.$", "")
            .toLowerCase(Locale.US);

        if (host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            return host;
        }

        String[] labels = host.split("\\.");
        List<String> parts = new ArrayList<>();
        for (String label : labels) {
            if (label.length() > 0) {
                parts.add(label);
            }
        }
        if (parts.size() <= 2) {
            return host;
        }
        return parts.get(parts.size() - 2) + "." + parts.get(parts.size() - 1);
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
        if (statusBox != null) {
            statusBox.setVisibility(text == null || text.length() == 0 ? View.GONE : View.VISIBLE);
        }
        statusText.setText(text);
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(38), dp(38));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        params.setMargins(0, 0, dp(8), 0);
        button.setLayoutParams(params);
        return button;
    }

    private TextView periodButton(String value) {
        TextView button = text(value, 12, TEXT_PRIMARY, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(buttonBackground(BG_SECONDARY, BORDER));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
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
        return buttonBackground(background, stroke, dp(8));
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
            boolean handled = handlePullToRefreshTouch(this, event);
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
        String currency = "EUR";
        String source = "";
        String accountName = "";
        String accountDisplayName = "";
        String warning = "";
    }
}
