package ro.mobilissimo.adsensetracker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RefreshRequestTrackerTest {
    @Test
    public void onlyLatestRefreshCanPublish() {
        RefreshRequestTracker tracker = new RefreshRequestTracker();

        long first = tracker.begin();
        long second = tracker.begin();

        assertFalse(tracker.isCurrent(first));
        assertTrue(tracker.isCurrent(second));
    }

    @Test
    public void invalidationRejectsLateCallback() {
        RefreshRequestTracker tracker = new RefreshRequestTracker();
        long refresh = tracker.begin();

        tracker.invalidate();

        assertFalse(tracker.isCurrent(refresh));
    }
}
