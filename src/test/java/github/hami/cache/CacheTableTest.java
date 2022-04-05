package github.hami.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p> 2022/4/5 16:37 </p>
 *
 * @author 珈乐不甜不要钱
 */
class CacheTableTest {

    @Test
    void count() {
        CacheTable<Integer, Integer> table = Cache.Allocate("count");
        for (int i = 0; i < 10; i++) {
            table.put(i, i);
        }
        assertEquals(table.count(), 10);
    }

    @Test
    void exist() {
        CacheTable<Integer, Integer> table = Cache.Allocate("exist");
        table.put(2, 1);
        assertFalse(table.exist(1));
        assertTrue(table.exist(2));
    }

    @Test
    void forEach() {
        CacheTable<Integer, Integer> table = Cache.Allocate("foreach");
        for (int i = 0; i < 10; i++) {
            table.put(i, i);
        }
        table.forEach(Assertions::assertEquals);
    }

    @Test
    void flush() {
        CacheTable<Integer, Integer> table = Cache.Allocate("flush");
        for (int i = 0; i < 10; i++) {
            table.put(i, i);
        }
        table.flush();
        for (int i = 0; i < 10; i++) {
            assertNull(table.get(i));
        }
    }

    @Test
    void delete() {
        CacheTable<Integer, Integer> table = Cache.Allocate("delete");
        table.put(1, 1);
        table.delete(1);
        assertNull(table.get(1));
    }

    @Test
    void get() {
        CacheTable<Integer, Integer> table = Cache.Allocate("get");
        table.put(1, 1);
        assertEquals(table.get(1), 1);
        assertNull(table.get(2));
    }

    @Test
    void getLoad() {
        CacheTable<Integer, Integer> table = Cache.Allocate("getload");
        table.setLoadCallback(k -> 1);
        assertEquals(table.get(1), 1);
    }

    @Test
    void put() {
        CacheTable<Integer, Integer> table = Cache.Allocate("put");
        table.put(1, 1);
        assertEquals(table.get(1), 1);
    }

    @Test
    void expire() {
        CacheTable<Integer, Integer> table = Cache.Allocate("expire");
        for (int i = 0; i < (1 << 6) + 1; i++) {
            table.put(i, i);
        }
        assertNull(table.get(0));
    }

    @Test
    void expireCallback() {
        CacheTable<Integer, Integer> table = Cache.Allocate("expireCallback");
        boolean[] expire = {false};
        table.setExpireCallback((k, v) -> expire[0] = true);
        for (int i = 0; i < (1 << 6) + 1; i++) {
            table.put(i, i);
        }
        assertNull(table.get(0));
        assertTrue(expire[0]);
    }
}