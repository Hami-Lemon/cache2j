package github.hami.cache;

/**
 * <p> 2022/4/5 12:32 </p>
 * 缓存元素被淘汰时的回调函数
 *
 * @author 珈乐不甜不要钱
 */
@FunctionalInterface
public interface ExpireCallback<K, V> {
    void call(K key, V value);
}
