package github.hami.cache;

/**
 * <p> 2022/4/5 10:44 </p>
 * 缓存的每个元素结点
 *
 * @author 珈乐不甜不要钱
 */
class CacheItem<K, V> {
    private final K key;
    private V value;

    //前驱节点
    private CacheItem<K, V> prev;
    //后继节点
    private CacheItem<K, V> next;

    protected CacheItem(K key, V value) {
        this.key = key;
        this.value = value;
        this.prev = this.next = null;
    }


    protected K getKey() {
        return key;
    }

    protected V getValue() {
        return value;
    }

    protected void setValue(V value) {
        this.value = value;
    }

    protected CacheItem<K, V> getPrev() {
        return prev;
    }

    protected void setPrev(CacheItem<K, V> prev) {
        this.prev = prev;
    }

    protected CacheItem<K, V> getNext() {
        return next;
    }

    protected void setNext(CacheItem<K, V> next) {
        this.next = next;
    }
}
