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

const PERIODS = Object.freeze([
    { key: 'today', rowId: 'todayRow', amountId: 'todayAmt' },
    { key: 'yesterday', rowId: 'yesterdayRow', amountId: 'yesterdayAmt' },
    { key: 'week', rowId: 'weekRow', amountId: 'weekAmt' },
    { key: 'month', rowId: 'monthRow', amountId: 'monthAmt' },
    { key: 'days30', period: '30days', rowId: '30daysRow', amountId: '30daysAmt' },
    { key: 'lastmonth', rowId: 'lastmonthRow', amountId: 'lastmonthAmt' }
]);

const FALLBACK_STORAGE_KEYS = Object.freeze({
    sync: 'adsenseTracker.sync',
    local: 'adsenseTracker.local'
});

class AdSenseTracker {
    constructor() {
        this.currentPeriod = 'today';
        this.currentView = 'loading';
        this.previousView = 'loading';
        this.isLoading = false;
        this.lastData = null;
        this.currentCurrency = DEFAULT_SETTINGS.currencyCode;
        this.currentSettings = { ...DEFAULT_SETTINGS };
        this.cacheElements();
        this.populateCurrencyOptions();
        this.init();
    }

    async init() {
        this.setupEventListeners();

        const settings = await this.getSettings();
        this.currentSettings = settings;
        this.currentCurrency = settings.currencyCode;
        this.applyTheme(settings.theme);
        this.persistResolvedTheme(settings);
        this.populateSettings(settings);

        const cached = await this.getLocal({
            lastEarnings: null,
            lastUpdated: '',
            dataSource: ''
        });

        if (cached.lastEarnings) {
            this.displayData(cached.lastEarnings, {
                source: cached.dataSource || 'Cached data',
                timestamp: cached.lastUpdated,
                mode: settings.useDemoMode ? 'Demo mode' : 'Live mode'
            });
            this.setNotice('Showing cached data while the extension refreshes.', 'warning');
        }

        if (this.shouldPromptFirstRunAuth(settings)) {
            await this.setSync({ firstRunAuthAttempted: true });
            await this.loadData({ showSpinner: !cached.lastEarnings, interactiveAuth: true });
            return;
        }

        await this.loadData({ showSpinner: !cached.lastEarnings });
    }

    cacheElements() {
        this.el = {
            loading: document.getElementById('loading'),
            error: document.getElementById('error'),
            errorMessage: document.getElementById('errorMessage'),
            oauthSetupGuide: document.getElementById('oauthSetupGuide'),
            extensionIdValue: document.getElementById('extensionIdValue'),
            googleCloudFieldsValue: document.getElementById('googleCloudFieldsValue'),
            copyStatus: document.getElementById('copyStatus'),
            content: document.getElementById('content'),
            contentNotice: document.getElementById('contentNotice'),
            settingsPanel: document.getElementById('settingsPanel'),
            refreshBtn: document.getElementById('refreshBtn'),
            settingsBtn: document.getElementById('settingsBtn'),
            backBtn: document.getElementById('backBtn'),
            retryBtn: document.getElementById('retryBtn'),
            connectFromErrorBtn: document.getElementById('connectFromErrorBtn'),
            saveSettingsBtn: document.getElementById('saveSettingsBtn'),
            useDemoBtn: document.getElementById('useDemo'),
            connectGoogleBtn: document.getElementById('connectGoogleBtn'),
            disconnectGoogleBtn: document.getElementById('disconnectGoogleBtn'),
            refreshInterval: document.getElementById('refreshInterval'),
            themeOptions: Array.from(document.querySelectorAll('[data-theme-value]')),
            weekStartDay: document.getElementById('weekStartDay'),
            currencyCode: document.getElementById('currencyCode'),
            badgeStyle: document.getElementById('badgeStyle'),
            settingsStatus: document.getElementById('settingsStatus'),
            authStatus: document.getElementById('authStatus'),
            modeBadge: document.getElementById('modeBadge'),
            todayAmount: document.getElementById('todayAmount'),
            todayChange: document.getElementById('todayChange'),
            monthProjectionAmount: document.getElementById('monthProjectionAmount'),
            monthProjectionMeta: document.getElementById('monthProjectionMeta'),
            sitesList: document.getElementById('sitesList'),
            sitesTotal: document.getElementById('sitesTotal'),
            dataSource: document.getElementById('dataSource'),
            lastUpdated: document.getElementById('lastUpdated'),
            periodTabs: Array.from(document.querySelectorAll('.period-tab')),
            copyButtons: Array.from(document.querySelectorAll('.copy-btn'))
        };
    }

    setupEventListeners() {
        this.el.refreshBtn.addEventListener('click', () => this.loadData());
        this.el.settingsBtn.addEventListener('click', () => this.showSettings());
        this.el.backBtn.addEventListener('click', () => this.hideSettings());
        this.el.retryBtn.addEventListener('click', () => this.loadData());
        this.el.connectFromErrorBtn.addEventListener('click', () => this.connectGoogle());
        this.el.connectGoogleBtn.addEventListener('click', () => this.connectGoogle());
        this.el.disconnectGoogleBtn.addEventListener('click', () => this.disconnectGoogle());
        this.el.saveSettingsBtn.addEventListener('click', () => this.saveSettings());
        this.el.useDemoBtn.addEventListener('click', () => this.useDemoData());
        this.el.themeOptions.forEach((button) => {
            button.addEventListener('click', () => this.selectTheme(button.dataset.themeValue));
            button.addEventListener('keydown', (event) => this.handleThemeKeydown(event, button));
        });
        this.el.copyButtons.forEach((button) => {
            button.addEventListener('click', () => this.copyField(button.dataset.copyTarget));
        });

        this.el.periodTabs.forEach((tab) => {
            tab.addEventListener('click', () => this.switchPeriod(tab.dataset.period));
            tab.addEventListener('keydown', (event) => this.handlePeriodKeydown(event, tab));
        });
    }

    shouldPromptFirstRunAuth(settings) {
        return !settings.useDemoMode
            && !settings.authConnected
            && !settings.firstRunAuthAttempted
            && this.hasChromeIdentity
            && this.isOAuthConfigured();
    }

    async loadData(options = {}) {
        const { showSpinner = true, interactiveAuth = false } = options;

        if (this.isLoading) {
            return;
        }

        this.isLoading = true;
        this.setRefreshState(true);
        this.setNotice('');

        if (showSpinner) {
            this.showView('loading');
        }

        try {
            const settings = await this.getSettings();
            this.currentSettings = settings;
            this.currentCurrency = settings.currencyCode;
            this.applyTheme(settings.theme);
            this.persistResolvedTheme(settings);
            this.populateSettings(settings);

            let data;
            let source;
            let mode;

            if (settings.useDemoMode) {
                data = this.generateMockData();
                source = 'Demo data';
                mode = 'Demo mode';
            } else {
                const token = await this.getAuthToken(interactiveAuth);
                data = await this.fetchFromAdSenseAPI(token, settings);
                source = data.accountDisplayName ? `AdSense: ${data.accountDisplayName}` : 'AdSense API';
                mode = 'Live mode';
                await this.setSync({ authConnected: true, useDemoMode: false });
            }

            const timestamp = new Date().toISOString();
            this.displayData(data, { source, timestamp, mode });
            await this.setLocal({
                lastEarnings: data,
                lastUpdated: timestamp,
                dataSource: source,
                lastError: ''
            });
            this.updateBadge(data.periods.today);
        } catch (error) {
            const message = error instanceof Error ? error.message : 'Failed to load earnings data.';
            await this.setLocal({
                lastError: message,
                lastUpdated: new Date().toISOString()
            });

            if (this.lastData) {
                this.setNotice(message, 'warning');
                this.updateModeBadge('Needs attention', 'warning');
                this.showView('content');
            } else {
                this.showError(message);
            }
        } finally {
            this.isLoading = false;
            this.setRefreshState(false);
        }
    }

    async connectGoogle() {
        await this.setSync({
            useDemoMode: false,
            firstRunAuthAttempted: true
        });
        await this.loadData({ showSpinner: true, interactiveAuth: true });
    }

    async disconnectGoogle() {
        if (this.hasChromeIdentity) {
            await new Promise((resolve) => chrome.identity.clearAllCachedAuthTokens(resolve));
        }

        await this.setSync({
            authConnected: false,
            useDemoMode: false,
            firstRunAuthAttempted: false
        });
        await this.setLocal({
            lastError: '',
            dataSource: ''
        });
        this.lastData = null;
        this.populateSettings(await this.getSettings());
        this.showError('Disconnected. Connect Google to load live AdSense data.');
    }

    async fetchFromAdSenseAPI(token, settings) {
        const accountResponse = await this.apiFetch('/accounts?pageSize=100', token);
        const accounts = Array.isArray(accountResponse.accounts) ? accountResponse.accounts : [];
        const account = accounts[0];

        if (!account || !account.name) {
            throw new Error('No AdSense account is available for this Google user.');
        }

        const ranges = this.getDateRanges(settings.weekStartDay);
        const periodEntries = await Promise.all(Object.entries(ranges).map(async ([period, range]) => {
            const report = await this.generateReport(token, account.name, range, {
                currencyCode: settings.currencyCode
            });
            return [period, this.extractReportTotal(report)];
        }));
        const periods = Object.fromEntries(periodEntries);
        const topSiteEntries = await Promise.all(Object.entries(ranges).map(async ([period, range]) => {
            const report = await this.generateReport(token, account.name, range, {
                dimensions: ['OWNED_SITE_DOMAIN_NAME'],
                orderBy: ['-ESTIMATED_EARNINGS'],
                limit: TOP_SITES_REPORT_LIMIT,
                currencyCode: settings.currencyCode
            });

            return [period, this.extractTopSites(report)];
        }));
        const topSitesByPeriod = Object.fromEntries(topSiteEntries);

        return {
            periods,
            topSites: topSitesByPeriod.days30 || [],
            topSitesByPeriod,
            currency: this.currentCurrency,
            accountName: account.name,
            accountDisplayName: account.displayName || account.display_name || account.name
        };
    }

    generateReport(token, accountName, range, options = {}) {
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

        return this.apiFetch(url, token);
    }

    async apiFetch(pathOrUrl, token) {
        const url = pathOrUrl instanceof URL ? pathOrUrl : new URL(`${ADSENSE_API_BASE}${pathOrUrl}`);
        const response = await fetch(url.toString(), {
            headers: {
                Authorization: `Bearer ${token}`,
                Accept: 'application/json'
            }
        });

        if (response.status === 401 && this.hasChromeIdentity) {
            await new Promise((resolve) => chrome.identity.removeCachedAuthToken({ token }, resolve));
        }

        if (!response.ok) {
            const details = await this.readApiError(response);
            throw new Error(this.formatApiError(details, response.status));
        }

        return response.json();
    }

    async readApiError(response) {
        try {
            const body = await response.json();
            return body.error && body.error.message ? body.error.message : '';
        } catch (error) {
            return response.statusText || '';
        }
    }

    formatApiError(details, status) {
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

    extractReportTotal(report) {
        const currency = this.extractCurrency(report);

        if (currency) {
            this.currentCurrency = currency;
        }

        const row = report && report.totals ? report.totals : null;
        const cells = row && Array.isArray(row.cells) ? row.cells : [];
        const metricCell = cells.find((cell) => Number.isFinite(Number(cell.value)));
        return metricCell ? this.toNumber(metricCell.value) : 0;
    }

    extractTopSites(report) {
        const rows = report && Array.isArray(report.rows) ? report.rows : [];

        const sites = rows.map((row) => {
            const cells = Array.isArray(row.cells) ? row.cells : [];
            return {
                name: cells[0] && cells[0].value ? String(cells[0].value) : 'Unknown site',
                earnings: this.toNumber(cells[1] && cells[1].value)
            };
        }).filter((site) => site.earnings > 0);

        return this.mergeTopSites(sites);
    }

    extractCurrency(report) {
        const headers = report && Array.isArray(report.headers) ? report.headers : [];
        const metricHeader = headers.find((header) => header.name === 'ESTIMATED_EARNINGS' || header.type === 'METRIC_CURRENCY');
        return metricHeader && metricHeader.currencyCode ? metricHeader.currencyCode : '';
    }

    displayData(rawData, meta = {}) {
        const data = this.normalizeData(rawData);
        const timestamp = meta.timestamp || new Date().toISOString();
        const source = meta.source || 'AdSense API';
        const mode = meta.mode || 'Live mode';

        this.lastData = data;
        this.currentCurrency = data.currency || this.currentCurrency || 'USD';
        this.showView('content');
        this.updateModeBadge(mode, mode === 'Live mode' ? 'live' : '');

        const todayEarnings = data.periods.today;
        const yesterdayEarnings = data.periods.yesterday;
        const monthProjection = this.calculateMonthlyProjection(data.periods.month, timestamp);
        this.el.todayAmount.textContent = this.formatCurrency(todayEarnings);
        this.updateDailyChange(todayEarnings, yesterdayEarnings);
        this.renderMonthlyProjection(monthProjection);

        PERIODS.forEach((period) => {
            const amountEl = document.getElementById(period.amountId);
            amountEl.textContent = this.formatCurrency(data.periods[period.key]);
        });

        this.el.dataSource.textContent = source;
        this.el.lastUpdated.textContent = this.formatTime(timestamp);
        this.switchPeriod(this.currentPeriod);
    }

    normalizeData(rawData) {
        const data = rawData && typeof rawData === 'object' ? rawData : {};
        const periods = data.periods && typeof data.periods === 'object' ? data.periods : {};
        const normalizedPeriods = {
            today: this.toNumber(periods.today),
            yesterday: this.toNumber(periods.yesterday),
            week: this.toNumber(periods.week),
            month: this.toNumber(periods.month),
            days30: this.toNumber(periods.days30),
            lastmonth: this.toNumber(periods.lastmonth)
        };

        const topSites = this.normalizeTopSites(data.topSites);
        const topSitesByPeriod = this.normalizeTopSitesByPeriod(data.topSitesByPeriod, topSites);

        return {
            periods: normalizedPeriods,
            topSites: topSitesByPeriod.days30,
            topSitesByPeriod,
            currency: data.currency || this.currentCurrency || 'USD'
        };
    }

    normalizeTopSites(sites) {
        return Array.isArray(sites)
            ? sites
                .map((site) => ({
                    name: String(site.name || 'Unknown site'),
                    earnings: this.toNumber(site.earnings)
                }))
                .filter((site) => site.earnings > 0)
            : [];
    }

    normalizeTopSitesByPeriod(rawTopSitesByPeriod, fallbackTopSites) {
        const source = rawTopSitesByPeriod && typeof rawTopSitesByPeriod === 'object' ? rawTopSitesByPeriod : {};
        const topSitesByPeriod = {};

        PERIODS.forEach((period) => {
            topSitesByPeriod[period.key] = this.mergeTopSites(this.normalizeTopSites(source[period.key]));
        });

        if (!Object.values(topSitesByPeriod).some((sites) => sites.length) && fallbackTopSites.length) {
            topSitesByPeriod.days30 = this.mergeTopSites(fallbackTopSites);
        }

        return topSitesByPeriod;
    }

    mergeTopSites(sites) {
        const mergedSites = new Map();

        sites.forEach((site) => {
            const canonicalName = this.canonicalizeSiteName(site.name);
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

    canonicalizeSiteName(name) {
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

    updateDailyChange(todayEarnings, yesterdayEarnings) {
        const change = todayEarnings - yesterdayEarnings;
        const changeEl = this.el.todayChange;
        changeEl.className = 'change';

        if (!yesterdayEarnings) {
            changeEl.textContent = 'No comparison yet';
            return;
        }

        const changePercent = Math.abs((change / yesterdayEarnings) * 100).toFixed(1);
        const changeAmount = this.formatCurrency(Math.abs(change));

        if (change > 0) {
            changeEl.textContent = `+${changeAmount} (+${changePercent}%) vs yesterday`;
            changeEl.classList.add('positive');
        } else if (change < 0) {
            changeEl.textContent = `-${changeAmount} (-${changePercent}%) vs yesterday`;
            changeEl.classList.add('negative');
        } else {
            changeEl.textContent = 'No change vs yesterday';
        }
    }

    calculateMonthlyProjection(monthToDate, timestamp = '') {
        const date = this.parseDate(timestamp);
        const elapsedDays = Math.max(1, date.getDate());
        const daysInMonth = new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
        const dailyAverage = this.toNumber(monthToDate) / elapsedDays;

        return {
            amount: dailyAverage * daysInMonth,
            dailyAverage,
            elapsedDays,
            daysInMonth
        };
    }

    renderMonthlyProjection(projection) {
        this.el.monthProjectionAmount.textContent = this.formatCurrency(projection.amount);
        this.el.monthProjectionMeta.textContent = `Based on ${projection.elapsedDays}/${projection.daysInMonth} days, ${this.formatCurrency(projection.dailyAverage)}/day`;
    }

    parseDate(timestamp) {
        const date = timestamp ? new Date(timestamp) : new Date();
        return Number.isNaN(date.getTime()) ? new Date() : date;
    }

    displayTopSites(sites) {
        this.el.sitesList.replaceChildren();

        const visibleSites = sites.slice(0, 7);
        const total = visibleSites.reduce((sum, site) => sum + site.earnings, 0);
        this.el.sitesTotal.textContent = `${this.formatCurrency(total)} total`;

        if (!visibleSites.length) {
            const empty = document.createElement('div');
            empty.className = 'empty-state';
            empty.textContent = 'No site-level earnings available yet.';
            this.el.sitesList.appendChild(empty);
            return;
        }

        visibleSites.forEach((site, index) => {
            const item = document.createElement('div');
            item.className = 'site-item';

            const main = document.createElement('div');
            main.className = 'site-main';

            const rank = document.createElement('div');
            rank.className = 'site-rank';
            rank.textContent = String(index + 1);

            const info = document.createElement('div');
            info.className = 'site-info';

            const name = document.createElement('div');
            name.className = 'site-name';
            name.title = site.name;
            name.textContent = site.name;

            const share = document.createElement('div');
            share.className = 'site-share';
            share.textContent = total > 0 ? `${this.formatShare(site.earnings / total)} of top sites` : 'No share';

            const earnings = document.createElement('div');
            earnings.className = 'site-earnings';
            earnings.textContent = this.formatCurrency(site.earnings);

            info.append(name, share);
            main.append(rank, info);
            item.append(main, earnings);
            this.el.sitesList.appendChild(item);
        });
    }

    formatShare(ratio) {
        const percent = ratio * 100;

        if (percent > 0 && percent < 1) {
            return '<1%';
        }

        return `${Math.round(percent)}%`;
    }

    switchPeriod(period, options = {}) {
        const targetPeriod = PERIODS.some((item) => (item.period || item.key) === period) ? period : 'today';

        this.el.periodTabs.forEach((tab) => {
            const isActive = tab.dataset.period === targetPeriod;
            tab.classList.toggle('active', isActive);
            tab.setAttribute('aria-selected', String(isActive));
            tab.tabIndex = isActive ? 0 : -1;

            if (isActive && options.focus) {
                tab.focus();
            }
        });

        PERIODS.forEach((periodConfig) => {
            const row = document.getElementById(periodConfig.rowId);
            const rowPeriod = periodConfig.period || periodConfig.key;
            row.classList.toggle('active', rowPeriod === targetPeriod);
        });

        this.currentPeriod = targetPeriod;

        if (this.lastData) {
            this.displayTopSites(this.getTopSitesForPeriod(targetPeriod));
        }
    }

    getTopSitesForPeriod(period) {
        const periodKey = this.getPeriodDataKey(period);
        return this.lastData.topSitesByPeriod[periodKey] || [];
    }

    getPeriodDataKey(period) {
        const periodConfig = PERIODS.find((item) => (item.period || item.key) === period);
        return periodConfig ? periodConfig.key : 'today';
    }

    handlePeriodKeydown(event, tab) {
        if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) {
            return;
        }

        event.preventDefault();
        const tabs = this.el.periodTabs;
        const currentIndex = tabs.indexOf(tab);
        let nextIndex = currentIndex;

        if (event.key === 'ArrowRight') {
            nextIndex = (currentIndex + 1) % tabs.length;
        } else if (event.key === 'ArrowLeft') {
            nextIndex = (currentIndex - 1 + tabs.length) % tabs.length;
        } else if (event.key === 'Home') {
            nextIndex = 0;
        } else if (event.key === 'End') {
            nextIndex = tabs.length - 1;
        }

        this.switchPeriod(tabs[nextIndex].dataset.period, { focus: true });
    }

    showView(view) {
        this.currentView = view;
        this.el.loading.hidden = view !== 'loading';
        this.el.error.hidden = view !== 'error';
        this.el.content.hidden = view !== 'content';
        this.el.settingsPanel.hidden = view !== 'settings';
    }

    showError(message) {
        this.el.errorMessage.textContent = message;
        this.updateModeBadge('Not connected', 'warning');
        this.renderOAuthSetupGuide(this.shouldShowOAuthSetup(message));
        this.showView('error');
    }

    shouldShowOAuthSetup(message) {
        return message.includes('OAuth client ID')
            || message.includes('Chrome Developer mode')
            || message.includes('Google authentication');
    }

    renderOAuthSetupGuide(isVisible) {
        this.el.oauthSetupGuide.hidden = !isVisible;

        if (!isVisible) {
            return;
        }

        const extensionId = this.getExtensionId();
        const itemId = extensionId || 'Load unpacked first, then copy the ID from chrome://extensions';
        this.el.extensionIdValue.textContent = itemId;
        this.el.googleCloudFieldsValue.textContent = [
            'Application type: Chrome extension',
            'Name: AdSense Earnings Tracker',
            `Item ID: ${itemId}`
        ].join('\n');
        this.el.copyStatus.textContent = '';
    }

    getExtensionId() {
        return this.hasChromeRuntime && chrome.runtime.id ? chrome.runtime.id : '';
    }

    async copyField(targetId) {
        const target = document.getElementById(targetId);

        if (!target) {
            return;
        }

        const text = target.textContent.trim();

        try {
            await this.copyText(text);
            this.el.copyStatus.textContent = 'Copied.';
        } catch (error) {
            this.el.copyStatus.textContent = 'Copy failed. Select the value manually.';
        }
    }

    async copyText(text) {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            await navigator.clipboard.writeText(text);
            return;
        }

        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.setAttribute('readonly', '');
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        const copied = document.execCommand('copy');
        textarea.remove();

        if (!copied) {
            throw new Error('Clipboard copy failed.');
        }
    }

    async showSettings() {
        this.previousView = this.currentView;
        this.showView('settings');
        this.setSettingsStatus('');

        const settings = await this.getSettings();
        this.populateSettings(settings);
    }

    hideSettings() {
        this.setSettingsStatus('');

        if (this.lastData) {
            this.showView('content');
            return;
        }

        if (this.el.errorMessage.textContent) {
            this.showView('error');
            return;
        }

        this.showView(this.previousView || 'loading');
    }

    async saveSettings() {
        const refreshInterval = this.clamp(
            Number.parseInt(this.el.refreshInterval.value, 10) || DEFAULT_SETTINGS.refreshInterval,
            5,
            60
        );
        const theme = this.getSelectedTheme();
        const weekStartDay = this.normalizeWeekStartDay(this.el.weekStartDay.value);
        const currencyCode = this.normalizeCurrencyCode(this.el.currencyCode.value);
        const badgeStyle = this.normalizeBadgeStyle(this.el.badgeStyle.value);
        const settings = await this.getSettings();

        await this.setSync({
            ...settings,
            refreshInterval,
            theme,
            weekStartDay,
            currencyCode,
            badgeStyle,
            resolvedTheme: this.resolveThemeMode(theme)
        });

        this.currentSettings = {
            ...settings,
            refreshInterval,
            theme,
            weekStartDay,
            currencyCode,
            badgeStyle,
            resolvedTheme: this.resolveThemeMode(theme)
        };
        this.currentCurrency = currencyCode;
        this.populateSettings(this.currentSettings);
        this.applyTheme(theme);
        this.setSettingsStatus('Settings saved.', 'success');

        if (this.currentSettings.useDemoMode || this.currentSettings.authConnected) {
            await this.loadData({ showSpinner: false });
        } else if (this.lastData) {
            this.updateBadge(this.lastData.periods.today);
        }
    }

    async useDemoData() {
        const settings = await this.getSettings();

        await this.setSync({
            ...settings,
            useDemoMode: true
        });

        this.populateSettings({ ...settings, useDemoMode: true });
        this.setSettingsStatus('Demo mode enabled.', 'warning');
        await this.loadData({ showSpinner: false });
    }

    populateSettings(settings) {
        this.el.refreshInterval.value = String(settings.refreshInterval || DEFAULT_SETTINGS.refreshInterval);
        this.setThemeControl(settings.theme);
        this.el.weekStartDay.value = this.normalizeWeekStartDay(settings.weekStartDay);
        this.el.currencyCode.value = this.normalizeCurrencyCode(settings.currencyCode);
        this.el.badgeStyle.value = this.normalizeBadgeStyle(settings.badgeStyle);
        this.el.authStatus.textContent = this.getAuthStatusText(settings);
        this.el.disconnectGoogleBtn.disabled = !settings.authConnected;
    }

    populateCurrencyOptions() {
        this.el.currencyCode.replaceChildren();

        ADSENSE_CURRENCY_CODES.forEach((currencyCode) => {
            const option = document.createElement('option');
            option.value = currencyCode;
            option.textContent = currencyCode;
            this.el.currencyCode.appendChild(option);
        });
    }

    getAuthStatusText(settings) {
        if (settings.useDemoMode) {
            return 'Demo mode is active. Connect Google for live AdSense data.';
        }

        if (!this.hasChromeIdentity) {
            return 'Install the repository root as a Chrome extension to use Google authentication.';
        }

        if (!this.isOAuthConfigured()) {
            return 'OAuth client ID is missing in manifest.json.';
        }

        return settings.authConnected ? 'Connected to Google.' : 'Not connected. Authentication is requested on first run.';
    }

    setSettingsStatus(message, variant = '') {
        this.el.settingsStatus.textContent = message;
        this.el.settingsStatus.className = variant ? `settings-status ${variant}` : 'settings-status';
    }

    setNotice(message, variant = '') {
        this.el.contentNotice.textContent = message;
        this.el.contentNotice.className = variant ? `content-notice ${variant}` : 'content-notice';
        this.el.contentNotice.hidden = !message;
    }

    updateModeBadge(label, variant = '') {
        this.el.modeBadge.textContent = label;
        this.el.modeBadge.className = variant ? `mode-badge ${variant}` : 'mode-badge';
    }

    setRefreshState(isRefreshing) {
        this.el.refreshBtn.disabled = isRefreshing;
        this.el.refreshBtn.classList.toggle('is-loading', isRefreshing);
        this.el.refreshBtn.setAttribute('aria-busy', String(isRefreshing));
    }

    selectTheme(theme) {
        const normalizedTheme = this.normalizeTheme(theme);
        this.setThemeControl(normalizedTheme);
        this.applyTheme(normalizedTheme);
    }

    setThemeControl(theme) {
        const normalizedTheme = this.normalizeTheme(theme);

        this.el.themeOptions.forEach((button) => {
            const isSelected = button.dataset.themeValue === normalizedTheme;
            button.classList.toggle('active', isSelected);
            button.setAttribute('aria-checked', String(isSelected));
            button.tabIndex = isSelected ? 0 : -1;
        });
    }

    getSelectedTheme() {
        const selected = this.el.themeOptions.find((button) => button.getAttribute('aria-checked') === 'true');
        return this.normalizeTheme(selected ? selected.dataset.themeValue : DEFAULT_SETTINGS.theme);
    }

    handleThemeKeydown(event, button) {
        if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) {
            return;
        }

        event.preventDefault();
        const options = this.el.themeOptions;
        const currentIndex = options.indexOf(button);
        let nextIndex = currentIndex;

        if (event.key === 'ArrowRight') {
            nextIndex = (currentIndex + 1) % options.length;
        } else if (event.key === 'ArrowLeft') {
            nextIndex = (currentIndex - 1 + options.length) % options.length;
        } else if (event.key === 'Home') {
            nextIndex = 0;
        } else if (event.key === 'End') {
            nextIndex = options.length - 1;
        }

        const nextButton = options[nextIndex];
        this.selectTheme(nextButton.dataset.themeValue);
        nextButton.focus();
    }

    async getAuthToken(interactive) {
        if (!this.hasChromeIdentity) {
            throw new Error('Install the repository root with Chrome Developer mode to use Google authentication.');
        }

        if (!this.isOAuthConfigured()) {
            throw new Error('Set a real Google OAuth client ID in manifest.json, then reload the extension.');
        }

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

    isOAuthConfigured() {
        if (!this.hasChromeRuntime) {
            return false;
        }

        const manifest = chrome.runtime.getManifest();
        const clientId = manifest.oauth2 && manifest.oauth2.client_id;
        return Boolean(clientId && clientId !== OAUTH_CLIENT_ID_PLACEHOLDER && clientId.endsWith('.apps.googleusercontent.com'));
    }

    async getSettings() {
        const stored = await this.getSync(DEFAULT_SETTINGS);
        return {
            refreshInterval: this.clamp(
                Number.parseInt(stored.refreshInterval, 10) || DEFAULT_SETTINGS.refreshInterval,
                5,
                60
            ),
            theme: this.normalizeTheme(stored.theme),
            weekStartDay: this.normalizeWeekStartDay(stored.weekStartDay),
            currencyCode: this.normalizeCurrencyCode(stored.currencyCode),
            badgeStyle: this.normalizeBadgeStyle(stored.badgeStyle),
            resolvedTheme: this.normalizeResolvedTheme(stored.resolvedTheme),
            useDemoMode: stored.useDemoMode === true,
            authConnected: stored.authConnected === true,
            firstRunAuthAttempted: stored.firstRunAuthAttempted === true
        };
    }

    applyTheme(theme = 'auto') {
        const normalizedTheme = this.normalizeTheme(theme);

        if (normalizedTheme === 'auto') {
            document.documentElement.removeAttribute('data-theme');
        } else {
            document.documentElement.setAttribute('data-theme', normalizedTheme);
        }
    }

    normalizeTheme(theme) {
        return ['auto', 'light', 'dark'].includes(theme) ? theme : 'auto';
    }

    normalizeWeekStartDay(weekStartDay) {
        return ['monday', 'sunday'].includes(weekStartDay) ? weekStartDay : DEFAULT_SETTINGS.weekStartDay;
    }

    normalizeCurrencyCode(currencyCode) {
        const normalized = String(currencyCode || '').trim().toUpperCase();
        return ADSENSE_CURRENCY_CODES.includes(normalized) ? normalized : DEFAULT_SETTINGS.currencyCode;
    }

    normalizeBadgeStyle(badgeStyle) {
        return ['auto', 'yellow', 'blue'].includes(badgeStyle) ? badgeStyle : DEFAULT_SETTINGS.badgeStyle;
    }

    normalizeResolvedTheme(resolvedTheme) {
        return ['light', 'dark'].includes(resolvedTheme) ? resolvedTheme : DEFAULT_SETTINGS.resolvedTheme;
    }

    persistResolvedTheme(settings) {
        const resolvedTheme = this.resolveThemeMode(settings.theme);
        this.currentSettings = { ...this.currentSettings, resolvedTheme };

        if (this.hasChromeStorage && settings.resolvedTheme !== resolvedTheme) {
            this.setSync({ resolvedTheme });
        }
    }

    get hasChromeRuntime() {
        return typeof chrome !== 'undefined' && chrome.runtime;
    }

    get hasChromeStorage() {
        return this.hasChromeRuntime && chrome.storage && chrome.storage.sync && chrome.storage.local;
    }

    get hasChromeIdentity() {
        return this.hasChromeRuntime && chrome.identity && chrome.identity.getAuthToken;
    }

    get hasChromeAction() {
        return this.hasChromeRuntime && chrome.action;
    }

    getSync(defaults) {
        return new Promise((resolve) => {
            if (this.hasChromeStorage) {
                chrome.storage.sync.get(defaults, (items) => {
                    if (chrome.runtime && chrome.runtime.lastError) {
                        console.warn(chrome.runtime.lastError.message);
                        resolve({ ...defaults });
                        return;
                    }

                    resolve({ ...defaults, ...items });
                });
                return;
            }

            resolve({ ...defaults, ...this.readFallbackStorage(FALLBACK_STORAGE_KEYS.sync) });
        });
    }

    setSync(items) {
        return new Promise((resolve) => {
            if (this.hasChromeStorage) {
                chrome.storage.sync.set(items, resolve);
                return;
            }

            this.writeFallbackStorage(FALLBACK_STORAGE_KEYS.sync, items);
            resolve();
        });
    }

    getLocal(defaults) {
        return new Promise((resolve) => {
            if (this.hasChromeStorage) {
                chrome.storage.local.get(defaults, (items) => {
                    if (chrome.runtime && chrome.runtime.lastError) {
                        console.warn(chrome.runtime.lastError.message);
                        resolve({ ...defaults });
                        return;
                    }

                    resolve({ ...defaults, ...items });
                });
                return;
            }

            resolve({ ...defaults, ...this.readFallbackStorage(FALLBACK_STORAGE_KEYS.local) });
        });
    }

    setLocal(items) {
        return new Promise((resolve) => {
            if (this.hasChromeStorage) {
                chrome.storage.local.set(items, resolve);
                return;
            }

            this.writeFallbackStorage(FALLBACK_STORAGE_KEYS.local, items);
            resolve();
        });
    }

    readFallbackStorage(key) {
        try {
            return JSON.parse(window.localStorage.getItem(key) || '{}');
        } catch (error) {
            console.warn('Could not read local fallback storage.', error);
            return {};
        }
    }

    writeFallbackStorage(key, items) {
        const current = this.readFallbackStorage(key);
        window.localStorage.setItem(key, JSON.stringify({ ...current, ...items }));
    }

    getDateRanges(weekStartDay = DEFAULT_SETTINGS.weekStartDay) {
        const today = this.startOfDay(new Date());
        const yesterday = this.addDays(today, -1);
        const weekStart = this.addDays(today, -this.getDaysSinceWeekStart(today, weekStartDay));
        const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);
        const lastMonthStart = new Date(today.getFullYear(), today.getMonth() - 1, 1);
        const lastMonthEnd = new Date(today.getFullYear(), today.getMonth(), 0);

        return {
            today: { start: this.toApiDate(today), end: this.toApiDate(today) },
            yesterday: { start: this.toApiDate(yesterday), end: this.toApiDate(yesterday) },
            week: { start: this.toApiDate(weekStart), end: this.toApiDate(today) },
            month: { start: this.toApiDate(monthStart), end: this.toApiDate(today) },
            days30: { start: this.toApiDate(this.addDays(today, -29)), end: this.toApiDate(today) },
            lastmonth: { start: this.toApiDate(lastMonthStart), end: this.toApiDate(lastMonthEnd) }
        };
    }

    getDaysSinceWeekStart(date, weekStartDay) {
        const day = date.getDay();
        return this.normalizeWeekStartDay(weekStartDay) === 'monday' ? (day + 6) % 7 : day;
    }

    startOfDay(date) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate());
    }

    addDays(date, days) {
        const next = new Date(date);
        next.setDate(next.getDate() + days);
        return next;
    }

    toApiDate(date) {
        return {
            year: date.getFullYear(),
            month: date.getMonth() + 1,
            day: date.getDate()
        };
    }

    generateMockData() {
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
            currency: this.currentSettings.currencyCode,
            periods: {
                today: 45.32,
                yesterday: 40.12,
                week: 298.45,
                month: 892.10,
                days30: 1245.60,
                lastmonth: 1100.00
            },
            topSites,
            topSitesByPeriod: Object.fromEntries(PERIODS.map((period) => [period.key, topSites]))
        };
    }

    updateBadge(amount) {
        if (!this.hasChromeAction) {
            return;
        }

        chrome.action.setBadgeText({ text: this.formatBadgeText(amount) });
        const badgeColors = this.getBadgeColors();
        chrome.action.setBadgeBackgroundColor({ color: badgeColors.backgroundColor });

        if (chrome.action.setBadgeTextColor) {
            chrome.action.setBadgeTextColor({ color: badgeColors.textColor });
        }
    }

    formatBadgeText(amount) {
        const numericAmount = this.toNumber(amount);
        const currencySymbol = this.getCurrencySymbol(this.currentSettings.currencyCode);

        if (numericAmount >= 1000) {
            return `${currencySymbol}${Math.round(numericAmount / 1000)}k`;
        }

        return `${currencySymbol}${Math.round(numericAmount)}`;
    }

    getBadgeColors() {
        const badgeStyle = this.normalizeBadgeStyle(this.currentSettings.badgeStyle);

        if (badgeStyle !== 'auto') {
            return BADGE_STYLES[badgeStyle];
        }

        return this.resolveThemeMode(this.currentSettings.theme) === 'dark'
            ? BADGE_STYLES.blue
            : BADGE_STYLES.yellow;
    }

    resolveThemeMode(theme) {
        if (theme === 'dark' || theme === 'light') {
            return theme;
        }

        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            return 'dark';
        }

        return 'light';
    }

    getCurrencySymbol(currencyCode) {
        const symbols = {
            EUR: '€',
            USD: '$',
            RON: 'lei',
            GBP: '£'
        };

        return symbols[this.normalizeCurrencyCode(currencyCode)] || currencyCode;
    }

    formatCurrency(amount) {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: this.currentCurrency || 'USD',
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(this.toNumber(amount));
    }

    formatTime(timestamp) {
        const date = timestamp ? new Date(timestamp) : new Date();

        if (Number.isNaN(date.getTime())) {
            return '--:--';
        }

        return date.toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    toNumber(value) {
        const number = Number(value);
        return Number.isFinite(number) ? number : 0;
    }

    clamp(value, min, max) {
        return Math.min(Math.max(value, min), max);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new AdSenseTracker();
});
