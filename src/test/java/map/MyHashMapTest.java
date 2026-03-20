package map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MyHashMapTest {
    private MyHashMap<CollisionKey, String> map;

    record CollisionKey(int id) {
        static int equalsCalls = 0;

        @Override
        public int hashCode() {
            if (id == 33) return 2;
            if (id == 66) return 3;
            return 1;
        }

        @Override
        public boolean equals(Object o) {
            equalsCalls++;
            if (this == o) return true;
            if (!(o instanceof CollisionKey)) return false;
            return id == ((CollisionKey) o).id;
        }

        @Override
        public String toString() {
            return "Key:" + id;
        }
    }


    @BeforeEach
    void setUp() {
        map = new MyHashMap<>();
    }


    @Test
    void test_putWithCollision_shouldCorrectlyContain() {
        CollisionKey a = new CollisionKey(1);
        CollisionKey b = new CollisionKey(2);

        map.put(a, "Alpha");
        map.put(b, "Beta");

        assertEquals("Beta", map.get(b));
        assertEquals("Alpha", map.get(a));
    }

    @Test
    @DisplayName("Оптимизация поиска: get должен останавливаться, если текущий DIB стал больше DIB в ячейке")
    void testGetEarlyExitOptimization() {
        map.put(new CollisionKey(1), "Alpha");
        map.put(new CollisionKey(2), "Beta");
        map.put(new CollisionKey(33), "Different target");
        CollisionKey.equalsCalls = 0;

        assertNull(map.get(new CollisionKey(3)));
        assertEquals(2, CollisionKey.equalsCalls,
                "Оптимизация не сработала: метод вызвал equals лишний раз вместо раннего выхода");
    }

    @Test
    @DisplayName("Оптимизация поиска: get должен останавливаться, если null в ячейке")
    void testGetEarlyExitOptimization_WhenFaceNull() {
        map.put(new CollisionKey(1), "Alpha");
        map.put(new CollisionKey(66), "Not neighbor target");
        CollisionKey.equalsCalls = 0;

        assertNull(map.get(new CollisionKey(2)));
        assertEquals(1, CollisionKey.equalsCalls,
                "Оптимизация не сработала: метод вызвал equals лишний раз вместо раннего выхода");
    }

    @Test
    @DisplayName("Удаление элемента не должно нарушать цепочку поиска при коллизии")
    void test_DeletionDoesNotBreakChain() {
        CollisionKey a = new CollisionKey(1);
        CollisionKey b = new CollisionKey(2);
        CollisionKey c = new CollisionKey(3);

        map.put(a, "Alpha");
        map.put(b, "Beta");
        map.put(c, "Gamma");

        map.remove(a);

        // B и C должны "подвинуться" назад к богатству. Если там остался null без сдвига - get(c) вернет null (см тест выше).
        assertAll(
                () -> assertEquals("Beta", map.get(b), "Элемент B потерян после удаления A"),
                () -> assertEquals("Gamma", map.get(c), "Элемент C потерян после удаления A"),
                () -> assertEquals(2, map.size())
        );
    }


    @Test
    @DisplayName("Resize: данные не должны теряться при перестроении таблицы")
    void testResizePreservesData() {
        // Наполняем мапу до срабатывания resize (INITIAL_CAPACITY * LOAD_FACTOR)
        for (int i = 0; i < 20; i++) {
            map.put(new CollisionKey(i), "Val" + i);
        }

        assertEquals(20, map.size());
        for (int i = 0; i < 20; i++) {
            assertEquals("Val" + i, map.get(new CollisionKey(i)));
        }
    }

}