package map;

import java.util.Map;

class MyEntry<K, V> implements Map.Entry<K, V> {
    K key;
    V value;
    int distanceFromInitialBucket;

    MyEntry(K key, V value, int dib) {
        this.key = key;
        this.value = value;
        this.distanceFromInitialBucket = dib;
    }

    @Override public K getKey() { return key; }
    @Override public V getValue() { return value; }
    @Override public V setValue(V value) {
        V old = this.value;
        this.value = value;
        return old;
    }
}