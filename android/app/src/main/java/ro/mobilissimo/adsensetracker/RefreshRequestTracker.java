package ro.mobilissimo.adsensetracker;

/** Tracks which foreground refresh is still allowed to publish UI state. */
final class RefreshRequestTracker {
    private long generation;

    synchronized long begin() {
        return ++generation;
    }

    synchronized void invalidate() {
        generation++;
    }

    synchronized boolean isCurrent(long candidate) {
        return candidate == generation;
    }
}
