package ro.mobilissimo.adsensetracker;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Serializes foreground and widget AdSense requests inside the app process.
 * Foreground refreshes wait for an active widget refresh, while widget refreshes
 * are skipped when a foreground refresh is already running.
 */
final class RefreshCoordinator {
    private static final ReentrantLock REFRESH_LOCK = new ReentrantLock(true);

    private RefreshCoordinator() {
    }

    static void acquireForeground() throws InterruptedException {
        REFRESH_LOCK.lockInterruptibly();
    }

    static boolean tryAcquireBackground() {
        return REFRESH_LOCK.tryLock();
    }

    static void release() {
        if (REFRESH_LOCK.isHeldByCurrentThread()) {
            REFRESH_LOCK.unlock();
        }
    }

    static boolean isRefreshInProgress() {
        return REFRESH_LOCK.isLocked();
    }
}
