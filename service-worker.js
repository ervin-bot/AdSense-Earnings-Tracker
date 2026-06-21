const ALARM_NAME = 'updateEarnings';
const ADSENSE_API_BASE = 'https://adsense.googleapis.com/v2';
const ADSENSE_SCOPE = 'https://www.googleapis.com/auth/adsense.readonly';
const OAUTH_CLIENT_ID_PLACEHOLDER = 'REPLACE_WITH_GOOGLE_OAUTH_CLIENT_ID.apps.googleusercontent.com';
const TOP_SITES_REPORT_LIMIT = 25;
const ADSENSE_CURRENCY_CODES = Object.freeze([
    'AED', 'AFN', 'ALL', 'AMD', 'ANG', 'ARS', 'AUD', 'AWG', 'AZN', 'BAM',
    'BDT', 'BGN', 'BHD', 'BND', 'BOB', 'BRL', 'BTN', 'BWP', 'BYN', 'BZD',
    'CAD', 'CHF', 'CLP', 'CNY', 'COP', 'CRC', 'CSD', 'CZK', 'CVE', 'DEM',
    'DKK', 'DOP', 'DZD', 'EEK', 'EGP', 'EUR', 'FJD', 'FRF', 'GBP', 'GEL',
    'GHS', 'HKD', 'HNL', 'HRK', 'HUF', 'IDR', 'ILS', 'INR', 'IQD', 'ISK',
    'JMD', 'JOD', 'JPY', 'KES', 'KGS', 'KRW', 'KWD', 'KYD', 'KZT', 'LAK',
    'LBP', 'LKR', 'LTL', 'MAD', 'MDL', 'MKD', 'MMK', 'MOP', 'MTL', 'MUR',
    'MVR', 'MXN', 'MYR', 'NAD', 'NGN', 'NIO', 'NOK', 'NPR', 'NZD', 'OMR',
    'PAB', 'PEN', 'PHP', 'PKR', 'PLN', 'PYG', 'QAR', 'RON', 'ROL', 'RSD',
    'RUB', 'SAR', 'SCR', 'SEK', 'SGD', 'SIT', 'SKK', 'SVC', 'THB', 'TND',
    'TRL', 'TRY', 'TTD', 'TWD', 'TZS', 'UAH', 'UGX', 'USD', 'UYU', 'UZS',
    'VEB', 'VEF', 'VES', 'VND', 'WST', 'XCD', 'XOF', 'XPF', 'YER', 'ZAR'
]);
const BADGE_STYLES = Object.freeze({
    blue: { backgroundColor: '#4285f4', textColor: '#ffffff' },
    yellow: { backgroundColor: '#fbbc04', textColor: '#111827' }
});

const DEFAULT_SETTINGS = Object.freeze({
    refreshInterval: 10,
    theme: 'auto',
    weekStartDay: 'monday',
    currencyCode: 'EUR',
    badgeStyle: 'auto',
    resolvedTheme: 'light',
    useDemoMode: false,
    authConnected: false,
    firstRunAuthAttempted: false
});

chrome.runtime.onInstalled.addListener((details) => {
    if (details.reason === 'install') {
        chrome.storage.sync.set(DEFAULT_SETTINGS, () => {
            configureAlarm();
            updateEarnings();
        });
        return;
    }

    configureAlarm();
    updateEarnings();
});

chrome.runtime.onStartup.addListener(() => {
    configureAlarm();
    updateEarnings();
});

chrome.alarms.onAlarm.addListener((alarm) => {
    if (alarm.name === ALARM_NAME) {
        updateEarnings();
    }
});

chrome.storage.onChanged.addListener((changes, areaName) => {
    if (areaName !== 'sync') {
        return;
    }

    if (changes.refreshInterval) {
        configureAlarm();
    }

    if (
        changes.useDemoMode
        || changes.authConnected
        || changes.currencyCode
        || changes.weekStartDay
        || changes.badgeStyle
        || changes.theme
        || changes.resolvedTheme
    ) {
        updateEarnings();
    }
});

configureAlarm();
updateEarnings();

async function configureAlarm() {
    const settings = await getSettings();
    const refreshInterval = clamp(
        Number.parseInt(settings.refreshInterval, 10) || DEFAULT_SETTINGS.refreshInterval,
        5,
        60
    );

    await clearAlarm(ALARM_NAME);
    chrome.alarms.create(ALARM_NAME, { periodInMinutes: refreshInterval });
}

async function updateEarnings() {
    try {
        const settings = await getSettings();

        if (settings.useDemoMode) {
            const mockData = generateMockData(settings.currencyCode);
            updateBadge(mockData.periods.today, settings);
            await setLocal({
                lastEarnings: mockData,
                lastUpdated: new Date().toISOString(),
                dataSource: 'Demo data',
                lastError: ''
            });
            return;
        }

        if (!isOAuthConfigured()) {
            const message = 'Set a real Google OAuth client ID in manifest.json, then reload the extension.';
            updateAttentionBadge();
            await setLocal({ lastError: message, lastUpdated: new Date().toISOString() });
            return;
        }

        const token = await getAuthToken(false);
        const data = await fetchFromAdSenseAPI(token, settings);
        updateBadge(data.periods.today, settings);
        await setSync({ authConnected: true, useDemoMode: false });
        await setLocal({
            lastEarnings: data,
            lastUpdated: new Date().toISOString(),
            dataSource: data.accountDisplayName ? `AdSense: ${data.accountDisplayName}` : 'AdSense API',
            lastError: ''
        });
    } catch (error) {
        const message = error instanceof Error ? error.message : 'Failed to update earnings.';
        console.warn('Error updating earnings:', message);
        updateAttentionBadge();
        await setLocal({
            lastError: message,
            lastUpdated: new Date().toISOString()
        });
    }
}

async function fetchFromAdSenseAPI(token, settings) {
    const accountResponse = await apiFetch('/accounts?pageSize=100', token);
    const accounts = Array.isArray(accountResponse.accounts) ? accountResponse.accounts : [];
    const account = accounts[0];

    if (!account || !account.name) {
        throw new Error('No AdSense account is available for this Google user.');
    }

    const ranges = getDateRanges(settings.weekStartDay);
    let currency = settings.currencyCode;
    const periodEntries = await Promise.all(Object.entries(ranges).map(async ([period, range]) => {
        const report = await generateReport(token, account.name, range, {
            currencyCode: settings.currencyCode
        });
        currency = extractCurrency(report) || currency;
        return [period, extractReportTotal(report)];
    }));
    const periods = Object.fromEntries(periodEntries);
    const topSiteEntries = await Promise.all(Object.entries(ranges).map(async ([period, range]) => {
        const report = await generateReport(token, account.name, range, {
            dimensions: ['OWNED_SITE_DOMAIN_NAME'],
            orderBy: ['-ESTIMATED_EARNINGS'],
            limit: TOP_SITES_REPORT_LIMIT,
            currencyCode: settings.currencyCode
        });
        currency = extractCurrency(report) || currency;

        return [period, extractTopSites(report)];
    }));
    const topSitesByPeriod = Object.fromEntries(topSiteEntries);

    return {
        periods,
        topSites: topSitesByPeriod.days30 || [],
        topSitesByPeriod,
        currency,
        accountName: account.name,
        accountDisplayName: account.displayName || account.display_name || account.name
    };
}

function generateReport(token, accountName, range, options = {}) {
    const url = new URL(`${ADSENSE_API_BASE}/${accountName}/reports:generate`);
    const dimensions = options.dimensions || [];
    const metrics = options.metrics || ['ESTIMATED_EARNINGS'];
    const orderBy = options.orderBy || [];

    dimensions.forEach((dimension) => url.searchParams.append('dimensions', dimension));
    metrics.forEach((metric) => url.searchParams.append('metrics', metric));
    orderBy.forEach((order) => url.searchParams.append('orderBy', order));
    url.searchParams.set('dateRange', 'CUSTOM');
    url.searchParams.set('startDate.year', String(range.start.year));
    url.searchParams.set('startDate.month', String(range.start.month));
    url.searchParams.set('startDate.day', String(range.start.day));
    url.searchParams.set('endDate.year', String(range.end.year));
    url.searchParams.set('endDate.month', String(range.end.month));
    url.searchParams.set('endDate.day', String(range.end.day));

    if (options.limit) {
        url.searchParams.set('limit', String(options.limit));
    }

    if (options.currencyCode) {
        url.searchParams.set('currencyCode', options.currencyCode);
    }

    return apiFetch(url, token);
}

async function apiFetch(pathOrUrl, token) {
    const url = pathOrUrl instanceof URL ? pathOrUrl : new URL(`${ADSENSE_API_BASE}${pathOrUrl}`);
    const response = await fetch(url.toString(), {
        headers: {
            Authorization: `Bearer ${token}`,
            Accept: 'application/json'
        }
    });

    if (response.status === 401) {
        await removeCachedAuthToken(token);
    }

    if (!response.ok) {
        const details = await readApiError(response);
        throw new Error(formatApiError(details, response.status));
    }

    return response.json();
}

async function readApiError(response) {
    try {
        const body = await response.json();
        return body.error && body.error.message ? body.error.message : '';
    } catch (error) {
        return response.statusText || '';
    }
}

function formatApiError(details, status) {
    const message = details || `AdSense API request failed with HTTP ${status}.`;
    const projectMatch = message.match(/project[=\s-]+(\d+)/i);

    if (/AdSense Management API has not been used|adsense\.googleapis\.com.*disabled|it is disabled/i.test(message)) {
        const projectId = projectMatch ? projectMatch[1] : '';
        const enableUrl = projectId
            ? `https://console.developers.google.com/apis/api/adsense.googleapis.com/overview?project=${projectId}`
            : 'https://console.developers.google.com/apis/api/adsense.googleapis.com/overview';

        return `AdSense Management API is disabled for this Google Cloud project. Enable it here: ${enableUrl}. Wait a few minutes after enabling it, then click Retry.`;
    }

    return message;
}

function extractReportTotal(report) {
    const row = report && report.totals ? report.totals : null;
    const cells = row && Array.isArray(row.cells) ? row.cells : [];
    const metricCell = cells.find((cell) => Number.isFinite(Number(cell.value)));
    return metricCell ? toNumber(metricCell.value) : 0;
}

function extractTopSites(report) {
    const rows = report && Array.isArray(report.rows) ? report.rows : [];

    const sites = rows.map((row) => {
        const cells = Array.isArray(row.cells) ? row.cells : [];
        return {
            name: cells[0] && cells[0].value ? String(cells[0].value) : 'Unknown site',
            earnings: toNumber(cells[1] && cells[1].value)
        };
    }).filter((site) => site.earnings > 0);

    return mergeTopSites(sites);
}

function mergeTopSites(sites) {
    const mergedSites = new Map();

    sites.forEach((site) => {
        const canonicalName = canonicalizeSiteName(site.name);
        const key = canonicalName || site.name.toLowerCase();
        const existingSite = mergedSites.get(key);

        if (existingSite) {
            existingSite.earnings = Math.max(existingSite.earnings, site.earnings);
            return;
        }

        mergedSites.set(key, {
            name: canonicalName || site.name,
            earnings: site.earnings
        });
    });

    return Array.from(mergedSites.values())
        .sort((a, b) => b.earnings - a.earnings);
}

function canonicalizeSiteName(name) {
    const host = String(name || '')
        .trim()
        .replace(/^https?:\/\//i, '')
        .split('/')[0]
        .split('?')[0]
        .replace(/^www\./i, '')
        .replace(/\.$/, '')
        .toLowerCase();

    if (/^\d{1,3}(\.\d{1,3}){3}$/.test(host)) {
        return host;
    }

    const labels = host.split('.').filter(Boolean);

    if (labels.length <= 2) {
        return host;
    }

    return labels.slice(-2).join('.');
}

function extractCurrency(report) {
    const headers = report && Array.isArray(report.headers) ? report.headers : [];
    const metricHeader = headers.find((header) => header.name === 'ESTIMATED_EARNINGS' || header.type === 'METRIC_CURRENCY');
    return metricHeader && metricHeader.currencyCode ? metricHeader.currencyCode : '';
}

function getAuthToken(interactive) {
    return new Promise((resolve, reject) => {
        chrome.identity.getAuthToken({
            interactive,
            enableGranularPermissions: true,
            scopes: [ADSENSE_SCOPE]
        }, (result) => {
            if (chrome.runtime.lastError) {
                reject(new Error(chrome.runtime.lastError.message));
                return;
            }

            const token = typeof result === 'string' ? result : result && result.token;

            if (!token) {
                reject(new Error('Google authentication did not return an access token.'));
                return;
            }

            resolve(token);
        });
    });
}

function removeCachedAuthToken(token) {
    return new Promise((resolve) => {
        chrome.identity.removeCachedAuthToken({ token }, resolve);
    });
}

function isOAuthConfigured() {
    const manifest = chrome.runtime.getManifest();
    const clientId = manifest.oauth2 && manifest.oauth2.client_id;
    return Boolean(clientId && clientId !== OAUTH_CLIENT_ID_PLACEHOLDER && clientId.endsWith('.apps.googleusercontent.com'));
}

function updateBadge(amount, settings) {
    chrome.action.setBadgeText({ text: formatBadgeText(amount, settings.currencyCode) });
    const badgeColors = getBadgeColors(settings);
    chrome.action.setBadgeBackgroundColor({ color: badgeColors.backgroundColor });

    if (chrome.action.setBadgeTextColor) {
        chrome.action.setBadgeTextColor({ color: badgeColors.textColor });
    }
}

function updateAttentionBadge() {
    chrome.action.setBadgeText({ text: '!' });
    chrome.action.setBadgeBackgroundColor({ color: '#dc2626' });

    if (chrome.action.setBadgeTextColor) {
        chrome.action.setBadgeTextColor({ color: '#ffffff' });
    }
}

function formatBadgeText(amount, currencyCode) {
    const numericAmount = Number(amount) || 0;
    const currencySymbol = getCurrencySymbol(currencyCode);

    if (numericAmount >= 1000) {
        return `${currencySymbol}${Math.round(numericAmount / 1000)}k`;
    }

    return `${currencySymbol}${Math.round(numericAmount)}`;
}

function getBadgeColors(settings) {
    const badgeStyle = normalizeBadgeStyle(settings.badgeStyle);

    if (badgeStyle !== 'auto') {
        return BADGE_STYLES[badgeStyle];
    }

    return settings.resolvedTheme === 'dark' || settings.theme === 'dark'
        ? BADGE_STYLES.blue
        : BADGE_STYLES.yellow;
}

function getCurrencySymbol(currencyCode) {
    const symbols = {
        EUR: '€',
        USD: '$',
        RON: 'lei',
        GBP: '£'
    };

    return symbols[normalizeCurrencyCode(currencyCode)] || currencyCode;
}

function getSettings() {
    return new Promise((resolve) => {
        chrome.storage.sync.get(DEFAULT_SETTINGS, (items) => {
            if (chrome.runtime.lastError) {
                console.warn(chrome.runtime.lastError.message);
                resolve({ ...DEFAULT_SETTINGS });
                return;
            }

            resolve({
                refreshInterval: clamp(
                    Number.parseInt(items.refreshInterval, 10) || DEFAULT_SETTINGS.refreshInterval,
                    5,
                    60
                ),
                theme: ['auto', 'light', 'dark'].includes(items.theme) ? items.theme : 'auto',
                weekStartDay: normalizeWeekStartDay(items.weekStartDay),
                currencyCode: normalizeCurrencyCode(items.currencyCode),
                badgeStyle: normalizeBadgeStyle(items.badgeStyle),
                resolvedTheme: normalizeResolvedTheme(items.resolvedTheme),
                useDemoMode: items.useDemoMode === true,
                authConnected: items.authConnected === true,
                firstRunAuthAttempted: items.firstRunAuthAttempted === true
            });
        });
    });
}

function setSync(items) {
    return new Promise((resolve) => {
        chrome.storage.sync.set(items, resolve);
    });
}

function setLocal(items) {
    return new Promise((resolve) => {
        chrome.storage.local.set(items, resolve);
    });
}

function clearAlarm(name) {
    return new Promise((resolve) => {
        chrome.alarms.clear(name, resolve);
    });
}

function getDateRanges(weekStartDay = DEFAULT_SETTINGS.weekStartDay) {
    const today = startOfDay(new Date());
    const yesterday = addDays(today, -1);
    const weekStart = addDays(today, -getDaysSinceWeekStart(today, weekStartDay));
    const lastWeekStart = addDays(weekStart, -7);
    const lastWeekEnd = addDays(weekStart, -1);
    const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);
    const lastMonthStart = new Date(today.getFullYear(), today.getMonth() - 1, 1);
    const lastMonthEnd = new Date(today.getFullYear(), today.getMonth(), 0);

    return {
        today: { start: toApiDate(today), end: toApiDate(today) },
        yesterday: { start: toApiDate(yesterday), end: toApiDate(yesterday) },
        week: { start: toApiDate(weekStart), end: toApiDate(today) },
        lastweek: { start: toApiDate(lastWeekStart), end: toApiDate(lastWeekEnd) },
        month: { start: toApiDate(monthStart), end: toApiDate(today) },
        days30: { start: toApiDate(addDays(today, -29)), end: toApiDate(today) },
        lastmonth: { start: toApiDate(lastMonthStart), end: toApiDate(lastMonthEnd) }
    };
}

function getDaysSinceWeekStart(date, weekStartDay) {
    const day = date.getDay();
    return normalizeWeekStartDay(weekStartDay) === 'monday' ? (day + 6) % 7 : day;
}

function startOfDay(date) {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function addDays(date, days) {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
}

function toApiDate(date) {
    return {
        year: date.getFullYear(),
        month: date.getMonth() + 1,
        day: date.getDate()
    };
}

function generateMockData(currencyCode = DEFAULT_SETTINGS.currencyCode) {
    const topSites = [
        { name: 'example1.com', earnings: 234.50 },
        { name: 'tech-blog.io', earnings: 189.30 },
        { name: 'news-site.net', earnings: 145.20 },
        { name: 'tutorials.dev', earnings: 98.15 },
        { name: 'reviews.shop', earnings: 76.45 },
        { name: 'lifestyle.com', earnings: 54.30 },
        { name: 'gaming-hub.co', earnings: 42.18 },
        { name: 'fitness.app', earnings: 35.90 },
        { name: 'travel.guide', earnings: 28.75 }
    ];

    return {
        currency: normalizeCurrencyCode(currencyCode),
        periods: {
            today: 45.32,
            yesterday: 40.12,
            week: 298.45,
            lastweek: 512.20,
            month: 892.10,
            days30: 1245.60,
            lastmonth: 1100.00
        },
        topSites,
        topSitesByPeriod: {
            today: topSites,
            yesterday: topSites,
            week: topSites,
            lastweek: topSites,
            month: topSites,
            days30: topSites,
            lastmonth: topSites
        }
    };
}

function toNumber(value) {
    const number = Number(value);
    return Number.isFinite(number) ? number : 0;
}

function normalizeCurrencyCode(currencyCode) {
    const normalized = String(currencyCode || '').trim().toUpperCase();
    return ADSENSE_CURRENCY_CODES.includes(normalized) ? normalized : DEFAULT_SETTINGS.currencyCode;
}

function normalizeBadgeStyle(badgeStyle) {
    return ['auto', 'yellow', 'blue'].includes(badgeStyle) ? badgeStyle : DEFAULT_SETTINGS.badgeStyle;
}

function normalizeWeekStartDay(weekStartDay) {
    return ['monday', 'sunday'].includes(weekStartDay) ? weekStartDay : DEFAULT_SETTINGS.weekStartDay;
}

function normalizeResolvedTheme(resolvedTheme) {
    return ['light', 'dark'].includes(resolvedTheme) ? resolvedTheme : DEFAULT_SETTINGS.resolvedTheme;
}

function clamp(value, min, max) {
    return Math.min(Math.max(value, min), max);
}
