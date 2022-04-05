package github.hami.cache;

import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

/**
 * <p> 2022/4/5 10:21 </p>
 * 一个缓存表，每一个缓存结点存在一个缓存表里面。
 *
 * @author 珈乐不甜不要钱
 */
public class CacheTable<K, V> {
    //64 缓存容量
    private final int capacity = 1 << 6;

    private final HashMap<K, CacheItem<K, V>> table;
    //读写锁
    private final ReadWriteLock lock;

    //获取元素时，如果缓存中不存在指定元素，这调用该回调函数获取并将获取的值加入到缓存中
    private LoadCallback<K, V> loadCallback;
    //淘汰元素时的回调函数
    private ExpireCallback<K, V> expireCallback;
    /**
     * 维护一个哈希链表，实现LRU淘汰策略
     * https://blog.csdn.net/qq_34343254/article/details/110082648
     */
    //哈希链表的头节点和尾节点
    private CacheItem<K, V> head;
    private CacheItem<K, V> tail;

    protected CacheTable() {
        //哈希的扩容阈值是荷载因子乘以容量，所以荷载因子设为1，避免哈希表自动扩容
        table = new HashMap<>(capacity, 1);
        lock = new ReentrantReadWriteLock();
    }

    /**
     * 获取当前缓存中的元素个数
     */
    public int count() {
        lock.readLock().lock();
        int num = table.size();
        lock.readLock().unlock();
        return num;
    }

    /**
     * 检查是否缓存中是否存在指定元素
     *
     * @return 如果存在指定元素，返回true，否则返回false
     */
    public boolean exist(K key) {
        lock.readLock().lock();
        boolean ok = table.containsKey(key);
        lock.readLock().unlock();
        return ok;
    }

    /**
     * 迭代缓存中的所有元素
     */
    public void forEach(BiConsumer<K, V> consumer) {
        try {
            lock.readLock().lock();
            table.forEach((k, item) -> consumer.accept(k, item.getValue()));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清空缓冲区
     */
    public void flush() {
        lock.writeLock().lock();
        this.table.clear();
        lock.writeLock().unlock();
    }

    /**
     * 从缓存中删除指定元素，返回被删除元素的值，如果缓存中不存在该元素，则返回null,
     * 删除操作不会触发淘汰回调。
     */
    public V delete(K key) {
        lock.readLock().lock();
        CacheItem<K, V> value = table.get(key);
        lock.readLock().unlock();

        if (value == null) {
            return null;
        }
        try {
            lock.writeLock().lock();
            value = table.remove(key);
            //再次判断，可能在释放读锁后，该元素被其他线程删除或被淘汰
            if (value == null) {
                return null;
            }
            //从哈希链表中删除该元素
            CacheItem<K, V> prev = value.getPrev(), next = value.getNext();
            if (prev == null) {
                //当前元素为头节点
                value.setNext(null);
                this.head = next;
                //删除后链表为空
                if (next == null) {
                    this.tail = null;
                } else {
                    next.setPrev(null);
                }
            } else if (next == null) {
                //当前节点为尾节点
                this.tail = prev;
                this.tail.setNext(null);
                value.setPrev(null);
            } else {
                //当前节点在中间
                prev.setNext(next);
                next.setPrev(prev);
                value.setPrev(null);
                value.setNext(null);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return value.getValue();
    }

    /**
     * 从缓存中获取数据
     */
    public V get(final K key) {
        //先获取读锁，判断缓存中是否有数据
        lock.readLock().lock();
        CacheItem<K, V> item = table.get(key);
        lock.readLock().unlock();
        //不存在该元素
        if (item == null) {
            //没有回调函数，直接返回null
            if (loadCallback == null) {
                return null;
            } else {
                //二次校验，查看缓存中是否没有该元素，因为在释放读锁后
                // 可能有另一个线程向缓存中添加了这个键对应的元素
                V value;
                try {
                    lock.writeLock().lock();
                    item = table.get(key);
                    if (item == null) {
                        //调用回调函数获取值并加入到缓存中
                        value = loadCallback.load(key);
                        //ReentrantReadWriteLock是一个可重入锁，当前线程获取写锁后，可以再次获取写锁。
                        // 和ReentrantLock一样的机制。
                        put(key, value);
                    } else {
                        value = item.getValue();
                    }
                } finally {
                    lock.writeLock().unlock();
                }
                return value;
            }
        }

        try {
            //因为需要对链表操作，所以获取写锁
            lock.writeLock().lock();
            //移动item到哈希链表的尾部
            moveItem(item);
        } finally {
            lock.writeLock().unlock();
        }
        return item.getValue();
    }

    /**
     * 缓存中添加数据
     */
    public void put(final K key, final V value) {
        CacheItem<K, V> expireValue = null;

        try {
            lock.writeLock().lock();
            CacheItem<K, V> item = table.get(key);
            //元素已经存在,修改其保存的值，并移动到哈希链表的最后
            if (item != null) {
                item.setValue(value);
                moveItem(item);
                return;
            }

            final int size = table.size();
            if (size >= capacity) {
                //达到最大容量，淘汰元素
                expireValue = expire();
            }
            //元素不存在缓存中
            item = new CacheItem<>(key, value);
            table.put(key, item);
            if (this.head == null) {
                //链表为空
                head = item;
            } else {
                item.setPrev(tail);
                tail.setNext(item);
            }
            //添加到链表尾
            tail = item;
        } finally {
            lock.writeLock().unlock();
            //如果回调函数执行一个长时间操作（例如：访问数据库），
            // 那么会导致线程长时间占有锁，但这是不必要的，所以这里在释放锁之后执行回调。
            if (expireCallback != null && expireValue != null) {
                expireCallback.call(expireValue.getKey(), expireValue.getValue());
            }
        }
    }

    /**
     * 移动item到哈希链表的尾部
     */
    private void moveItem(final CacheItem<K, V> item) {
        final CacheItem<K, V> prev = item.getPrev();
        final CacheItem<K, V> next = item.getNext();
        //已经在尾部，不移动
        if (next == null) {
            return;
        }
        //item在第一个
        if (prev == null) {
            head = next;
            head.setPrev(null);
            item.setNext(null);

            tail.setNext(item);
            item.setPrev(tail);
        } else {
            //item在中间
            next.setPrev(prev);
            prev.setNext(next);

            item.setNext(null);
            item.setPrev(tail);
            tail.setNext(item);
        }
        tail = item;
    }

    /**
     * 从缓存中淘汰元素，每次淘汰都淘汰哈希链表的第一个元素，因为每次访问元素都会将其加入到链表尾部，
     * 所以第一个元素一定是最近最少使用的元素，即：采用LRU的淘汰策略。
     *
     * @return 被淘汰的元素
     */
    private CacheItem<K, V> expire() {
        final CacheItem<K, V> value = head;
        table.remove(value.getKey());

        final CacheItem<K, V> temp = head.getNext();
        head.setNext(null);
        //如果容量为1，temp会为null
        if (temp == null) {
            head = null;
            tail = null;
        } else {
            temp.setPrev(null);
            head = temp;
        }
        return value;
    }

    public void setLoadCallback(LoadCallback<K, V> loadCallback) {
        this.loadCallback = loadCallback;
    }

    public void setExpireCallback(ExpireCallback<K, V> expireCallback) {
        this.expireCallback = expireCallback;
    }
}
