package map;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@NotThreadSafe
public class MyHashMap<K, V> implements Map<K, V> {
    private static final int INITIAL_CAPACITY = 16;
    private static final double LOAD_FACTOR = 0.75;

    private Entry<K, V>[] table;
    private int size;
    private int modCount = 0;

    private int hash(Object key) {
        if (key == null) return 0;
        return (key.hashCode() & 0x7fffffff) % table.length;
    }

    private static class Entry<K, V> implements Map.Entry<K, V> {
        K key;
        V value;
        int distanceFromInitialBucket;

        Entry(K key, V value, int dib) {
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

    private abstract class HashIterator<T> implements Iterator<T> {
        int expectedModCount = modCount;
        int currentIndex = -1;
        int nextIndex = 0;
        int count = 0;

        private void checkModification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean hasNext() {
            checkModification();
            return count < size;
        }

        Entry<K, V> nextEntry() {
            checkModification();
            if (!hasNext()) throw new NoSuchElementException();

            while (table[nextIndex] == null) nextIndex++;

            currentIndex = nextIndex;
            nextIndex++;
            count++;
            return table[currentIndex];
        }

        @Override
        public void remove() {
            checkModification();
            if (currentIndex == -1) throw new IllegalStateException();

            MyHashMap.this.shiftBack(currentIndex);
            size--;
            count--;
            modCount++;
            expectedModCount++;

            nextIndex = currentIndex;
            currentIndex = -1;
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public int size() { return size; }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new HashIterator<Map.Entry<K, V>>() {
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
                return new HashIterator<K>() {
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
                return new HashIterator<V>() {
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
            table = (Entry<K, V>[]) new Entry[INITIAL_CAPACITY];
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

    private void shiftBack(int index) {
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
        Entry<K, V>[] oldTable = table;
        table = (Entry<K, V>[]) new Entry[newCapacity];

        for (Entry<K, V> entry : oldTable) {
            if (entry != null) {
                putInternal(entry.key, entry.value, true);
            }
        }
    }

    private V putInternal(K key, V value, boolean isResizing) {
        if (!isResizing) modCount++;

        Entry<K, V> current = new Entry<>(key, value, 0);
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
                Entry<K, V> temp = table[index];
                table[index] = current;
                current = temp;
            }

            index = (index + 1) % table.length; // для закольцовывания
            current.distanceFromInitialBucket++;
        }
    }
}