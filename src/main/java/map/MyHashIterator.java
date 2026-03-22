package map;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

abstract class MyHashIterator<T, K, V> implements Iterator<T> {
    private final MyHashMap<K, V> map;

    int expectedModCount;
    int currentIndex = -1;
    int nextIndex = 0;
    int count = 0;

    protected MyHashIterator(MyHashMap<K, V> map) {
        this.map = map;
        this.expectedModCount = map.modCount;
    }

    private void checkModification() {
        if (map.modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public boolean hasNext() {
        checkModification();
        return count < map.size;
    }

    MyEntry<K, V> nextEntry() {
        checkModification();
        if (!hasNext()) throw new NoSuchElementException();

        while (map.table[nextIndex] == null) nextIndex++;

        currentIndex = nextIndex;
        nextIndex++;
        count++;
        return map.table[currentIndex];
    }

    @Override
    public void remove() {
        checkModification();
        if (currentIndex == -1) throw new IllegalStateException();

        map.shiftBack(currentIndex);
        map.size--;
        count--;
        map.modCount++;
        expectedModCount++;

        nextIndex = currentIndex;
        currentIndex = -1;
    }
}