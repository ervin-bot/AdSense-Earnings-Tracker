const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
    testDir: './tests',
    timeout: 30000,
    expect: {
        timeout: 5000
    },
    use: {
        browserName: 'chromium',
        viewport: { width: 400, height: 700 },
        trace: 'on-first-retry'
    },
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] }
        }
    ]
});
