package github.hami.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p> 2022/4/5 11:03 </p>
 *
 * @author 珈乐不甜不要钱
 */
class CacheTest {
    int threadNum = 10;
    CountDownLatch latch = new CountDownLatch(threadNum);

    @Test
    @Timeout(2)
    void testCacheAllocate() throws InterruptedException {
        CacheTable[] tables = new CacheTable[2];

        for (int i = 0; i < threadNum; i++) {
            final int index = i;

            Thread t = new Thread(() -> {
                int times = 10;
                while (times-- != 0) {
                    CacheTable prev;
                    CacheTable now;
                    if (index % 2 == 0) {
                        prev = tables[0];
                        now = Cache.Allocate("0");
                        tables[0] = now;
                    } else {
                        prev = tables[1];
                        now = Cache.Allocate("1");
                        tables[1] = now;
                    }
                    if (prev != null) {
                        assertEquals(prev, now);
                    }
                }
                latch.countDown();

            });
            t.start();
        }

        latch.await();
    }
}