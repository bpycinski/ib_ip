package serwer;

/**
 * Pomocnicza klasa przechowująca parę parametryzowanych elementów.
 * W przykładzie zawsze parametrami są < String, String >. 
 * Po utworzeniu obiektu nie ma możliwości, by zmienić wartość któregoś
 * ze składników. 
 * Aby klasa była użyteczna poza tym przykładem, należy zaimplementować
 * odziedziczone funkcje toString(), equals() oraz hashCode().
 */
public class Pair<K, V> {
    private K key;
    private V value;

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
    
    /** Konstruktor. Wymaga podania obu parametrów */
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }
    
}
