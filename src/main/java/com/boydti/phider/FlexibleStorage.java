package com.boydti.phider;

import java.util.Arrays;

public class FlexibleStorage {
    private final long[] data;
    private final int bitsPerEntry;
    private final int size;
    private final long maxEntryValue;

    FlexibleStorage(int bitsPerEntry, int size) {
        this(bitsPerEntry, new long[roundToNearest(size * bitsPerEntry, 64) / 64]);
    }

    FlexibleStorage(int bitsPerEntry, long[] data) {
        if ((bitsPerEntry < 1) || (bitsPerEntry > 32)) {
            throw new IllegalArgumentException(
                "BitsPerEntry cannot be outside of accepted range. " + bitsPerEntry);
        }
        this.bitsPerEntry = bitsPerEntry;
        this.data = data;

        this.size = (this.data.length * 64 / this.bitsPerEntry);
        this.maxEntryValue = ((1L << this.bitsPerEntry) - 1L);
    }

    private static int roundToNearest(int value, int roundTo) {
        if (roundTo == 0) {
            return 0;
        }
        if (value == 0) {
            return roundTo;
        }
        if (value < 0) {
            roundTo *= -1;
        }
        int remainder = value % roundTo;
        return remainder != 0 ? value + roundTo - remainder : value;
    }

    long[] getData() {
        return this.data;
    }

    int getSize() {
        return this.size;
    }

    int get(int index) {
        if ((index < 0) || (index > this.size - 1)) {
            throw new IndexOutOfBoundsException();
        }
        int bitIndex = index * this.bitsPerEntry;
        int startIndex = bitIndex / 64;
        int endIndex = ((index + 1) * this.bitsPerEntry - 1) / 64;
        int startBitSubIndex = bitIndex % 64;
        if (startIndex == endIndex) {
            return (int) (this.data[startIndex] >>> startBitSubIndex & this.maxEntryValue);
        }
        int endBitSubIndex = 64 - startBitSubIndex;
        return (int) (
            (this.data[startIndex] >>> startBitSubIndex | this.data[endIndex] << endBitSubIndex)
                & this.maxEntryValue);
    }

    void set(int index, int value) {
        if ((index < 0) || (index > this.size - 1)) {
            throw new IndexOutOfBoundsException();
        }
        if ((value < 0) || (value > this.maxEntryValue)) {
            throw new IllegalArgumentException("Value cannot be outside of accepted range.");
        }
        int bitIndex = index * this.bitsPerEntry;
        int startIndex = bitIndex / 64;
        int endIndex = ((index + 1) * this.bitsPerEntry - 1) / 64;
        int startBitSubIndex = bitIndex % 64;
        this.data[startIndex] =
            (this.data[startIndex] & ~(this.maxEntryValue << startBitSubIndex)
                | (value & this.maxEntryValue) << startBitSubIndex);
        if (startIndex != endIndex) {
            int endBitSubIndex = 64 - startBitSubIndex;
            this.data[endIndex] = (this.data[endIndex] >>> endBitSubIndex << endBitSubIndex
                | (value & this.maxEntryValue) >> endBitSubIndex);
        }
    }

    public boolean equals(Object o) {
        return (this == o) || (((o instanceof FlexibleStorage)) && (Arrays
            .equals(this.data, ((FlexibleStorage) o).data)) && (this.bitsPerEntry
            == ((FlexibleStorage) o).bitsPerEntry) && (this.size == ((FlexibleStorage) o).size) && (
            this.maxEntryValue == ((FlexibleStorage) o).maxEntryValue));
    }

    public int hashCode() {
        int result = Arrays.hashCode(this.data);
        result = 31 * result + this.bitsPerEntry;
        result = 31 * result + this.size;
        result = 31 * result + (int) this.maxEntryValue;
        return result;
    }
}
