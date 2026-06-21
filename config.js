/**
 * AdSense Earnings Tracker Configuration
 * 
 * This file captures planned configuration for a future Google
 * AdSense Management API integration.
 */

// SETUP INSTRUCTIONS:
// 
// 1. Create a Google Cloud Project:
//    - Go to https://console.cloud.google.com
//    - Create a new project
//    - Enable the "AdSense Management API"
//
// 2. Create OAuth 2.0 Credentials:
//    - Go to Credentials
//    - Create OAuth 2.0 Client ID (Chrome extension type)
//    - Use the extension ID shown in chrome://extensions/
//    - Put the generated client ID in manifest.json
//
// 3. Configure the extension:
//    - Open the extension popup
//    - Click Settings
//    - Open the popup and approve the Google authentication prompt
//    - Select "Save Settings"
//
// 4. For Development/Testing:
//    - Click "Use Demo Data" to test with mock data
//    - This will populate the popup with sample earnings

const CONFIG = {
    // API Configuration
    API: {
        // Base URL for AdSense API
        BASE_URL: 'https://www.googleapis.com/adsense/v2',
        
        // Required scopes for AdSense API access
        SCOPES: [
            'https://www.googleapis.com/auth/adsense.readonly'
        ],
        
        // API endpoints
        ENDPOINTS: {
            ACCOUNTS: '/accounts',
            REPORTS: '/{account}/reports:generate'
        }
    },

    // Extension Configuration
    EXTENSION: {
        // Default refresh interval in minutes
        DEFAULT_REFRESH_INTERVAL: 10,
        
        // Minimum refresh interval (minutes)
        MIN_REFRESH_INTERVAL: 5,
        
        // Maximum refresh interval (minutes)
        MAX_REFRESH_INTERVAL: 60,
        
        // Popup window dimensions
        POPUP_WIDTH: 400,
        POPUP_HEIGHT: 600
    },

    // Cache Configuration
    CACHE: {
        // How long to cache data (milliseconds)
        CACHE_DURATION: 5 * 60 * 1000, // 5 minutes
        
        // Storage areas to use
        STORAGE_AREAS: ['local', 'sync']
    },

    // Display Configuration
    DISPLAY: {
        // Currency to display (ISO 4217 code)
        CURRENCY: 'USD',
        
        // Number of top sites to display
        TOP_SITES_COUNT: 7,
        
        // Time periods to track
        PERIODS: [
            'today',
            'yesterday',
            'week',
            'month',
            '30days',
            'lastmonth'
        ]
    },

    // Feature Flags
    FEATURES: {
        // Enable notifications on earnings milestones
        NOTIFICATIONS_ENABLED: false,
        
        // Enable dark mode support
        DARK_MODE: true,
        
        // Enable data export
        EXPORT_DATA: false,
        
        // Enable historical charts
        CHARTS_ENABLED: false
    }
};

// Helper function to get API endpoint URL
function getApiEndpoint(endpoint, params = {}) {
    let url = CONFIG.API.BASE_URL + endpoint;
    
    // Replace parameters in URL
    Object.entries(params).forEach(([key, value]) => {
        url = url.replace(`{${key}}`, value);
    });
    
    return url;
}

// Helper function to format earnings
function formatEarnings(amount, currency = CONFIG.DISPLAY.CURRENCY) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(amount);
}

// Helper function to get date range for a period
function getDateRange(period) {
    const today = new Date();
    const startDate = new Date();
    const endDate = new Date();

    switch (period) {
        case 'today':
            startDate.setHours(0, 0, 0, 0);
            endDate.setHours(23, 59, 59, 999);
            break;
            
        case 'yesterday':
            startDate.setDate(today.getDate() - 1);
            startDate.setHours(0, 0, 0, 0);
            endDate.setDate(today.getDate() - 1);
            endDate.setHours(23, 59, 59, 999);
            break;
            
        case 'week':
            const firstDay = today.getDate() - today.getDay();
            startDate.setDate(firstDay);
            startDate.setHours(0, 0, 0, 0);
            break;
            
        case 'month':
            startDate.setDate(1);
            startDate.setHours(0, 0, 0, 0);
            break;
            
        case '30days':
            startDate.setDate(today.getDate() - 30);
            startDate.setHours(0, 0, 0, 0);
            break;
            
        case 'lastmonth':
            const lastMonthDate = new Date(today.getFullYear(), today.getMonth() - 1, 1);
            const lastMonthEnd = new Date(today.getFullYear(), today.getMonth(), 0);
            
            startDate.setFullYear(lastMonthDate.getFullYear());
            startDate.setMonth(lastMonthDate.getMonth());
            startDate.setDate(lastMonthDate.getDate());
            startDate.setHours(0, 0, 0, 0);
            
            endDate.setFullYear(lastMonthEnd.getFullYear());
            endDate.setMonth(lastMonthEnd.getMonth());
            endDate.setDate(lastMonthEnd.getDate());
            endDate.setHours(23, 59, 59, 999);
            
            return { startDate, endDate };
    }

    return { startDate, endDate };
}

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { CONFIG, getApiEndpoint, formatEarnings, getDateRange };
}
