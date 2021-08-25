/*
 *              _____  _       _   _    _ _     _
 *              |  __ \| |     | | | |  | (_)   | |
 *              | |__) | | ___ | |_| |__| |_  __| | ___ _ __
 *              |  ___/| |/ _ \| __|  __  | |/ _` |/ _ \ '__|
 *              | |    | | (_) | |_| |  | | | (_| |  __/ |
 *              |_|    |_|\___/ \__|_|  |_|_|\__,_|\___|_|
 *               PlotHider PlotSquared addon for Minecraft
 *              Copyright (C) 2016 - 2022 IntellectualSites
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plotsquared.plothider;

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
        this.data[startIndex] = (this.data[startIndex] & ~(this.maxEntryValue << startBitSubIndex)
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
