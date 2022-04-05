package github.hami.cache;

/**
 * <p> 2022/4/5 12:36 </p>
 * 获取缓存元素时的加载回调
 *
 * @author 珈乐不甜不要钱
 */
@FunctionalInterface
public interface LoadCallback<K, V> {
    V load(K key);
}
