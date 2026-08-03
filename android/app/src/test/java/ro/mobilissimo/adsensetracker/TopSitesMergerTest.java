package ro.mobilissimo.adsensetracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class TopSitesMergerTest {
    private static final double DELTA = 0.000_001d;

    @Test
    public void rootAndWwwAliasesFromTheScreenshotAreNotAddedTogether() {
        List<TopSitesMerger.Entry> merged = TopSitesMerger.mergeAliases(Arrays.asList(
            entry("www.mobilissimo.ro", 855.58d),
            entry("mobilissimo.ro", 855.58d),
            entry("tablet-news.com", 5.99d),
            entry("gsmdome.com", 3.26d),
            entry("pescurt.ro", 1.61d),
            entry("mobilissimo.hu", 0.69d),
            entry("xchg.ro", 0.60d),
            entry("bucatardeduminica.ro", 0.44d)
        ));

        assertEquals(7, merged.size());
        assertEquals("mobilissimo.ro", merged.get(0).getName());
        assertEquals(855.58d, merged.get(0).getEarnings(), DELTA);
        assertEquals(868.17d, total(merged), DELTA);
        assertFalse(TopSitesMerger.isInconsistentWithReportTotal(total(merged), 868.52d));
        assertTrue(TopSitesMerger.isInconsistentWithReportTotal(1_723.75d, 868.52d));
    }

    @Test
    public void aliasMergeKeepsTheMaximumRegardlessOfInputOrder() {
        List<TopSitesMerger.Entry> rootFirst = TopSitesMerger.mergeAliases(Arrays.asList(
            entry("mobilissimo.ro", 10d),
            entry("www.mobilissimo.ro", 12d)
        ));
        List<TopSitesMerger.Entry> wwwFirst = TopSitesMerger.mergeAliases(Arrays.asList(
            entry("www.mobilissimo.ro", 12d),
            entry("mobilissimo.ro", 10d)
        ));

        assertEquals(1, rootFirst.size());
        assertEquals(12d, rootFirst.get(0).getEarnings(), DELTA);
        assertEquals(1, wwwFirst.size());
        assertEquals(12d, wwwFirst.get(0).getEarnings(), DELTA);
    }

    @Test
    public void nonWwwSubdomainsRemainDistinct() {
        List<TopSitesMerger.Entry> merged = TopSitesMerger.mergeAliases(Arrays.asList(
            entry("mobilissimo.ro", 10d),
            entry("amp.mobilissimo.ro", 3d)
        ));

        assertEquals(2, merged.size());
        assertEquals("mobilissimo.ro", merged.get(0).getName());
        assertEquals("amp.mobilissimo.ro", merged.get(1).getName());
    }

    @Test
    public void totalGuardIgnoresNonPositiveOrTinyRoundingCases() {
        assertFalse(TopSitesMerger.isInconsistentWithReportTotal(10.04d, 10d));
        assertFalse(TopSitesMerger.isInconsistentWithReportTotal(100.90d, 100d));
        assertTrue(TopSitesMerger.isInconsistentWithReportTotal(101.01d, 100d));
        assertFalse(TopSitesMerger.isInconsistentWithReportTotal(5d, 0d));
        assertFalse(TopSitesMerger.isInconsistentWithReportTotal(Double.NaN, 100d));
    }

    private static TopSitesMerger.Entry entry(String name, double earnings) {
        return new TopSitesMerger.Entry(name, earnings);
    }

    private static double total(List<TopSitesMerger.Entry> entries) {
        double result = 0d;
        for (TopSitesMerger.Entry entry : entries) {
            result += entry.getEarnings();
        }
        return result;
    }
}
