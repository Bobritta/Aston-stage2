package map;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@NotThreadSafe
public class MyHashMap<K, V> implements Map<K, V> {
    private static final int INITIAL_CAPACITY = 16;
    private static final double LOAD_FACTOR = 0.75;

    MyEntry<K, V>[] table;
    int size;
    int modCount = 0;

    private int hash(Object key) {
        if (key == null) return 0;
        return (key.hashCode() & 0x7fffffff) % table.length;
    }


    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public int size() { return size; }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new MyHashIterator<Map.Entry<K, V>, K, V>(MyHashMap.this) {
                    @Override
                    public Map.Entry<K, V> next() {
                        return nextEntry();
                    }
                };
            }
        };
    }


    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            @Override
            public int size() { return size; }

            @Override
            public Iterator<K> iterator() {
                return new MyHashIterator<K, K, V>(MyHashMap.this) {
                    @Override
                    public K next() {
                        return nextEntry().key;
                    }
                };
            }

            @Override
            public void clear() { MyHashMap.this.clear(); }
        };
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public int size() { return size; }

            @Override
            public Iterator<V> iterator() {
                return new MyHashIterator<V, K, V>(MyHashMap.this) {
                    @Override
                    public V next() {
                        return nextEntry().value;
                    }
                };
            }

            @Override
            public void clear() { MyHashMap.this.clear(); }
        };
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Map.Entry<K, V> entry : entrySet()) {
            if (Objects.equals(entry.getValue(), value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        if (table == null) return null;
        int index = hash(key);
        int start = index;
        int currentDist = 0;

        while (table[index] != null) {
            // Прошли расстояние большее, чем DIB элемента в ячейке - ключа в таблице нет
            if (currentDist > table[index].distanceFromInitialBucket) return null;

            if (Objects.equals(table[index].key, key)) {
                return table[index].value;
            }

            index = (index + 1) % table.length;
            currentDist++;
            if (index == start) break;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V put(Object key, Object value) {
        if (table == null) {
            table = (MyEntry<K, V>[]) new MyEntry[INITIAL_CAPACITY];
        }

        if (size >= table.length * LOAD_FACTOR) {
            resize(table.length * 2);
        }

        return putInternal((K) key, (V) value, false);
    }

    @Override
    public V remove(Object key) {
        if (table == null || size == 0) return null;

        int index = hash(key);
        int start = index;

        while (table[index] != null) {
            if (table[index].key != null && table[index].key.equals(key)) {
                V oldValue = table[index].value;
                modCount++;
                shiftBack(index);

                size--;
                if (size > INITIAL_CAPACITY && size <= table.length * 0.25) {
                    resize(table.length / 2);
                }
                return oldValue;
            }

            index = (index + 1) % table.length;
            if (index == start) break;
        }
        return null;
    }

    void shiftBack(int index) {
        int nextIndex = (index + 1) % table.length;

        while (table[nextIndex] != null && table[nextIndex].distanceFromInitialBucket > 0) {
            table[index] = table[nextIndex];
            table[index].distanceFromInitialBucket--;

            index = nextIndex;
            nextIndex = (nextIndex + 1) % table.length;
        }
        table[index] = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map m) {
        for (Object entryObj : m.entrySet()) {
            Map.Entry<? extends K, ? extends V> e = (Map.Entry<? extends K, ? extends V>) entryObj;
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
//      Компромисс между памятью и скоростью
        if (table != null && size > 0) {
            if (table.length > INITIAL_CAPACITY * 4) {
                table = null;
            } else {
                java.util.Arrays.fill(table, null);
            }
            size = 0;
        }
    }

    @SuppressWarnings("unchecked")
    private void resize(int newCapacity) {
        MyEntry<K, V>[] oldTable = table;
        table = (MyEntry<K, V>[]) new MyEntry[newCapacity];

        for (MyEntry<K, V> entry : oldTable) {
            if (entry != null) {
                putInternal(entry.key, entry.value, true);
            }
        }
    }

    private V putInternal(K key, V value, boolean isResizing) {
        if (!isResizing) modCount++;

        MyEntry<K, V> current = new MyEntry<>(key, value, 0);
        int index = hash(key);

        while (true) {
            if (table[index] == null) {
                table[index] = current;
                // При использовании внутри метода ресайз не увеличивается общее число вложенных элементов
                if (!isResizing) size++;
                return null;
            }

            // Для публичного метода, при ресайзе ключи всегда уникальны
            if (table[index].key != null && table[index].key.equals(current.key)) {
                V oldValue = table[index].value;
                table[index].value = current.value;
                return oldValue;
            }

            // Логика Robin Hood: если текущий элемент "прошел" больше, чем тот, что в ячейке
            if (current.distanceFromInitialBucket > table[index].distanceFromInitialBucket) {
                MyEntry<K, V> temp = table[index];
                table[index] = current;
                current = temp;
            }

            index = (index + 1) % table.length; // для закольцовывания
            current.distanceFromInitialBucket++;
        }
    }
}