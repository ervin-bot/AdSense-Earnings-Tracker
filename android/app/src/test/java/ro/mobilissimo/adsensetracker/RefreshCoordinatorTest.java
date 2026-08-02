package ro.mobilissimo.adsensetracker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class RefreshCoordinatorTest {
    @Test
    public void backgroundRefreshIsSkippedWhileForegroundRefreshRuns() throws Exception {
        CountDownLatch foregroundAcquired = new CountDownLatch(1);
        CountDownLatch releaseForeground = new CountDownLatch(1);
        AtomicReference<Throwable> threadFailure = new AtomicReference<>();

        Thread foreground = new Thread(() -> {
            try {
                RefreshCoordinator.acquireForeground();
                foregroundAcquired.countDown();
                releaseForeground.await(5, TimeUnit.SECONDS);
            } catch (Throwable error) {
                threadFailure.set(error);
            } finally {
                RefreshCoordinator.release();
            }
        });

        foreground.start();
        assertTrue("Foreground refresh did not acquire the coordinator", foregroundAcquired.await(5, TimeUnit.SECONDS));
        assertTrue(RefreshCoordinator.isRefreshInProgress());
        assertFalse(RefreshCoordinator.tryAcquireBackground());

        releaseForeground.countDown();
        foreground.join(5000);
        assertFalse("Foreground refresh thread did not stop", foreground.isAlive());
        if (threadFailure.get() != null) {
            throw new AssertionError(threadFailure.get());
        }

        assertTrue(RefreshCoordinator.tryAcquireBackground());
        RefreshCoordinator.release();
        assertFalse(RefreshCoordinator.isRefreshInProgress());
    }
}
