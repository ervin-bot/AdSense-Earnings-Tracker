package ro.mobilissimo.adsensetracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure-Java alias deduplication for AdSense site breakdowns. */
final class TopSitesMerger {
    private static final double MIN_TOTAL_TOLERANCE = 0.05d;
    private static final double RELATIVE_TOTAL_TOLERANCE = 0.01d;

    private TopSitesMerger() {
    }

    static List<Entry> mergeAliases(List<Entry> sites) {
        Map<String, Entry> merged = new HashMap<>();
        if (sites == null) {
            return new ArrayList<>();
        }

        for (Entry site : sites) {
            if (site == null || !Double.isFinite(site.earnings) || site.earnings <= 0d) {
                continue;
            }
            String canonicalName = canonicalizeSiteName(site.name);
            String key = canonicalName.length() > 0
                ? canonicalName
                : normalizeFallbackName(site.name);
            if (key.length() == 0) {
                continue;
            }

            Entry existing = merged.get(key);
            if (existing == null || site.earnings > existing.earnings) {
                merged.put(
                    key,
                    new Entry(canonicalName.length() > 0 ? canonicalName : site.name, site.earnings)
                );
            }
        }

        List<Entry> result = new ArrayList<>(merged.values());
        Collections.sort(result, (first, second) -> {
            int earningsOrder = Double.compare(second.earnings, first.earnings);
            return earningsOrder != 0 ? earningsOrder : first.name.compareTo(second.name);
        });
        return result;
    }

    static boolean isInconsistentWithReportTotal(double topSitesTotal, double reportTotal) {
        if (!Double.isFinite(topSitesTotal)
            || !Double.isFinite(reportTotal)
            || topSitesTotal < 0d
            || reportTotal <= 0d) {
            return false;
        }
        double tolerance = Math.max(
            MIN_TOTAL_TOLERANCE,
            Math.abs(reportTotal) * RELATIVE_TOTAL_TOLERANCE
        );
        return topSitesTotal > reportTotal + tolerance;
    }

    private static String canonicalizeSiteName(String name) {
        return name == null ? "" : name.trim()
            .replaceFirst("(?i)^https?://", "")
            .split("/")[0]
            .split("\\?")[0]
            .replaceFirst("(?i)^www\\.", "")
            .replaceFirst("\\.$", "")
            .toLowerCase(Locale.US);
    }

    private static String normalizeFallbackName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.US);
    }

    static final class Entry {
        private final String name;
        private final double earnings;

        Entry(String name, double earnings) {
            this.name = name == null ? "" : name;
            this.earnings = earnings;
        }

        String getName() {
            return name;
        }

        double getEarnings() {
            return earnings;
        }
    }
}
