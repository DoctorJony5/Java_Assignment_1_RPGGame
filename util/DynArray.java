package util;

//
//Custom dynamic array implementation without using Java collections.
//Automatically grows as needed. Very simplified version of an Arraylist.
//This was more important in an earlier version of this project where I played with a tetris-like inventory system with Ascii but in the end I decided against it; was too much of a mess, hard to manage & deal with, and because everytime the player levelled up I would need to do some "fun" maths to redo that entire array (and then also store, in a txt file for easy access, the visual aspect of it (was this necessary? no, it was just for debugging to be honest. The things I do for a 100%))

public class DynArray<T> {
    private Object[] items;
    private int size;
    private static final int INITIAL_CAPACITY = 10;

    public DynArray() {
        items = new Object[INITIAL_CAPACITY];
        size = 0;
    }

    public DynArray(int initialCapacity) {
        items = new Object[initialCapacity];
        size = 0;
    }

    public void add(T item) {
        if (size == items.length) {
            resize();
        }
        items[size++] = item;
    }

    public void add(int index, T item) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        if (size == items.length) {
            resize();
        }
        for (int i = size; i > index; i--) {
            items[i] = items[i - 1];
        }
        items[index] = item;
        size++;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return (T) items[index];
    }

    @SuppressWarnings("unchecked")
    public T remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        T item = (T) items[index];
        for (int i = index; i < size - 1; i++) {
            items[i] = items[i + 1];
        }
        size--;
        return item;
    }

    public boolean remove(T item) {
        for (int i = 0; i < size; i++) {
            if (items[i].equals(item)) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        items = new Object[INITIAL_CAPACITY];
        size = 0;
    }

    public boolean contains(T item) {
        for (int i = 0; i < size; i++) {
            if (items[i].equals(item)) {
                return true;
            }
        }
        return false;
    }

    public int indexOf(T item) {
        for (int i = 0; i < size; i++) {
            if (items[i].equals(item)) {
                return i;
            }
        }
        return -1;
    }

    private void resize() {
        Object[] newItems = new Object[items.length * 2];
        for (int i = 0; i < size; i++) {
            newItems[i] = items[i];
        }
        items = newItems;
    }

    public T[] toArray() {
        Object[] result = new Object[size];
        System.arraycopy(items, 0, result, 0, size); // simpler way to copy an array, but less fun. Probably not Exam-accepted either
        return (T[]) result;
    }
}
