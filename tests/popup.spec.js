const path = require('path');
const { test, expect } = require('@playwright/test');

const popupUrl = `file://${path.resolve(__dirname, '..', 'popup.html')}`;

test('asks for Chrome extension authentication outside extension context', async ({ page }) => {
    const browserErrors = [];

    page.on('pageerror', (error) => {
        browserErrors.push(error.message);
    });

    page.on('console', (message) => {
        if (message.type() === 'error') {
            browserErrors.push(message.text());
        }
    });

    await page.goto(popupUrl);

    await expect(page.getByRole('heading', { name: 'AdSense Earnings' })).toBeVisible();
    await expect(page.getByText('Data unavailable')).toBeVisible();
    await expect(page.getByText('Install the repository root with Chrome Developer mode')).toBeVisible();
    await expect(page.locator('#modeBadge')).toHaveText('Not connected');
    await expect(page.locator('.brand-icon')).toHaveJSProperty('naturalWidth', 128);
    await expect(page.getByText('OAuth setup')).toBeVisible();
    await expect(page.locator('#chromeExtensionsValue')).toHaveText('chrome://extensions');
    await expect(page.locator('#googleCloudFieldsValue')).toContainText('Application type: Chrome extension');
    await expect(page.locator('#googleCloudFieldsValue')).toContainText('Item ID: Load unpacked first');

    expect(browserErrors).toEqual([]);
});

test('can render explicit demo mode for local UI checks', async ({ page }) => {
    await page.goto(popupUrl);
    await page.evaluate(() => {
        window.localStorage.setItem('adsenseTracker.sync', JSON.stringify({ useDemoMode: true }));
    });
    await page.reload();

    const expectedProjection = await page.evaluate(() => {
        const now = new Date();
        const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
        const projectedMonth = (892.10 / now.getDate()) * daysInMonth;

        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'EUR',
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(projectedMonth);
    });

    await expect(page.locator('#todayAmount')).toHaveText('€45.32');
    await expect(page.getByText('Projected month')).toBeVisible();
    await expect(page.locator('#monthProjectionAmount')).toHaveText(expectedProjection);
    await expect(page.getByText('Top 7 Sites')).toBeVisible();
    await expect(page.getByText('example1.com')).toBeVisible();
    await expect(page.locator('#modeBadge')).toHaveText('Demo mode');
});

test('saves currency and toolbar badge style settings', async ({ page }) => {
    await page.goto(popupUrl);
    await page.evaluate(() => {
        window.localStorage.setItem('adsenseTracker.sync', JSON.stringify({ useDemoMode: true }));
    });
    await page.reload();

    await page.getByRole('button', { name: 'Settings' }).click();
    await expect(page.getByRole('radio', { name: 'Auto' })).toHaveAttribute('aria-checked', 'true');
    await page.getByRole('radio', { name: 'Night' }).click();
    await expect(page.locator('#weekStartDay')).toHaveValue('monday');
    await page.locator('#weekStartDay').selectOption('sunday');
    await page.locator('#currencyCode').selectOption('USD');
    await page.locator('#badgeStyle').selectOption('blue');
    await page.getByRole('button', { name: 'Save Settings' }).click();

    await expect(page.locator('#todayAmount')).toHaveText('$45.32');

    await page.getByRole('button', { name: 'Settings' }).click();
    await expect(page.getByRole('radio', { name: 'Night' })).toHaveAttribute('aria-checked', 'true');
    await expect(page.locator('#weekStartDay')).toHaveValue('sunday');
    await expect(page.locator('#currencyCode')).toHaveValue('USD');
    await expect(page.locator('#badgeStyle')).toHaveValue('blue');
});

test('deduplicates www and root domains in top sites', async ({ page }) => {
    await page.goto(popupUrl);
    await page.evaluate(() => {
        window.localStorage.setItem('adsenseTracker.local', JSON.stringify({
            lastUpdated: new Date().toISOString(),
            dataSource: 'Test cache',
            lastEarnings: {
                currency: 'EUR',
                periods: {
                    today: 16.62,
                    yesterday: 12.24,
                    week: 42.1,
                    lastweek: 75.4,
                    month: 98.5,
                    days30: 2320.07,
                    lastmonth: 100
                },
                topSitesByPeriod: {
                    today: [
                        { name: 'www.example.com', earnings: 1156.98 },
                        { name: 'example.com', earnings: 1156.98 },
                        { name: 'amp.example.com', earnings: 800.00 },
                        { name: 'tablet-news.com', earnings: 6.11 }
                    ]
                }
            }
        }));
    });

    await page.reload();

    await expect(page.locator('.site-name').first()).toHaveText('example.com');
    await expect(page.getByText('www.example.com')).toHaveCount(0);
    await expect(page.getByText('amp.example.com')).toHaveCount(0);
    await expect(page.getByText('€1,156.98')).toBeVisible();
    await expect(page.getByText('€1,163.09 total')).toBeVisible();
    await expect(page.getByText('€2,313.96')).toHaveCount(0);
});

test('syncs top sites with the selected period', async ({ page }) => {
    await page.goto(popupUrl);
    await page.evaluate(() => {
        window.localStorage.setItem('adsenseTracker.local', JSON.stringify({
            lastUpdated: new Date().toISOString(),
            dataSource: 'Test cache',
            lastEarnings: {
                currency: 'EUR',
                periods: {
                    today: 16.62,
                    yesterday: 12.24,
                    week: 42.1,
                    lastweek: 75.4,
                    month: 98.5,
                    days30: 2320.07,
                    lastmonth: 100
                },
                topSitesByPeriod: {
                    today: [
                        { name: 'today-site.ro', earnings: 16.62 }
                    ],
                    week: [
                        { name: 'week-site.ro', earnings: 42.1 }
                    ],
                    lastweek: [
                        { name: 'previous-period.ro', earnings: 75.4 }
                    ]
                }
            }
        }));
    });

    await page.reload();

    await expect(page.getByText('today-site.ro')).toBeVisible();
    await expect(page.getByText('week-site.ro')).toHaveCount(0);
    await expect(page.getByText('previous-period.ro')).toHaveCount(0);

    await page.getByRole('tab', { name: 'This Week' }).click();

    await expect(page.getByText('week-site.ro')).toBeVisible();
    await expect(page.getByText('today-site.ro')).toHaveCount(0);

    await page.getByRole('tab', { name: 'Last Week' }).click();

    await expect(page.getByText('previous-period.ro')).toBeVisible();
    await expect(page.getByText('week-site.ro')).toHaveCount(0);
    await expect(page.locator('#lastweekAmt')).toHaveText('€75.40');
});
