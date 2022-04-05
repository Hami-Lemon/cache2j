import github.hami.cache.Cache;
import github.hami.cache.CacheTable;

/**
 * <p> 2022/4/5 15:34 </p>
 *
 * @author 珈乐不甜不要钱
 */
public class Example {
    public static class MyObj {
        String text;

        public MyObj(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return "MyObj{" +
                    "text='" + text + '\'' +
                    '}';
        }
    }

    public static void main(String[] args) {
        CacheTable<String, MyObj> cache = Cache.Allocate("myCache");

        //设置加载回调，获取元素时，如果缓存中不存在，会调用这个方法，并将返回值加入到缓存中。
        cache.setLoadCallback((key -> new MyObj("create by load callback.")));
        //设置淘汰回调，当元素被淘汰时，会调用这个方法。
        cache.setExpireCallback((k, v) -> System.out.println("expire: " + k + " --- " + v));

        //获取一个不存在的元素
        MyObj obj1 = cache.get("noValue");
        System.out.println("get noValue: " + obj1);

        //添加一个元素
        cache.put("key0", new MyObj("hi"));
        System.out.println("get key0: " + cache.get("key0"));

        //修改已有元素的值
        cache.put("key0", new MyObj("hi! hi!"));
        System.out.println("get key0: " + cache.get("key0"));

        //删除元素,会返回被删除的元素
        MyObj deleteObj = cache.delete("key0");
        System.out.println("delete key0: " + deleteObj);
        //如果删除的元素不存在，则返回null
        System.out.println("delete noKey: " + cache.delete("noKey"));

        //模拟缓存区达到最大容量，触发淘汰回调
        for (int i = 0; i < (1 << 6) + 1; i++) {
            cache.put("key" + i, new MyObj(String.valueOf(i)));
        }

        //清空缓存区
        cache.flush();
    }
}
