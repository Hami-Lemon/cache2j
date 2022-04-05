package github.hami.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p> 2022/4/5 10:17 </p>
 *
 * @author 珈乐不甜不要钱
 */
public class Cache {
    //缓存，每一个具体的元素存储在一个缓存表中，map的键是一个缓存表对应的名称
    private static final Map<String, CacheTable<?, ?>> CACHE = new HashMap<>();
    //读写锁
    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

    /**
     * 申请一个表名为name的缓存表，如果已经存在，则直接对应的表。
     *
     * @param name 缓存表的名称
     * @return 获取的缓存表
     */
    @SuppressWarnings("all")
    public static <K, V> CacheTable<K, V> Allocate(String name) {
        //获取读锁
        LOCK.readLock().lock();
        CacheTable<?, ?> table = CACHE.get(name);
        LOCK.readLock().unlock(); //提前释放，减少其他线程等待时间

        if (table == null) {
            //获取写锁
            LOCK.writeLock().lock();
            //再次判断，因为在这期间可能有其它线程写入
            table = CACHE.get(name);
            if (table == null) {
                table = new CacheTable<K, V>();
                CACHE.put(name, table);
            }
            LOCK.writeLock().unlock();
        }
        //因为泛型擦除的机制，这里的转换只是为了语法通过
        return (CacheTable<K, V>) table;
    }
}
