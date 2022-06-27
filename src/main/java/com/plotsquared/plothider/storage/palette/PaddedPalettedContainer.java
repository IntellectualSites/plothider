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
package com.plotsquared.plothider.storage.palette;

import java.util.List;
import java.util.Objects;

/**
 * Modern and padded palette container implementation.
 * Used by Minecraft 1.16+.
 *
 * @since TODO
 */
public class PaddedPalettedContainer extends APalettedContainer {

    private static final int[] MAGIC = new int[]{-1, -1, 0, Integer.MIN_VALUE, 0, 0, 1431655765, 1431655765, 0, Integer.MIN_VALUE, 0, 1, 858993459, 858993459, 0, 715827882, 715827882, 0, 613566756, 613566756, 0, Integer.MIN_VALUE, 0, 2, 477218588, 477218588, 0, 429496729, 429496729, 0, 390451572, 390451572, 0, 357913941, 357913941, 0, 330382099, 330382099, 0, 306783378, 306783378, 0, 286331153, 286331153, 0, Integer.MIN_VALUE, 0, 3, 252645135, 252645135, 0, 238609294, 238609294, 0, 226050910, 226050910, 0, 214748364, 214748364, 0, 204522252, 204522252, 0, 195225786, 195225786, 0, 186737708, 186737708, 0, 178956970, 178956970, 0, 171798691, 171798691, 0, 165191049, 165191049, 0, 159072862, 159072862, 0, 153391689, 153391689, 0, 148102320, 148102320, 0, 143165576, 143165576, 0, 138547332, 138547332, 0, Integer.MIN_VALUE, 0, 4, 130150524, 130150524, 0, 126322567, 126322567, 0, 122713351, 122713351, 0, 119304647, 119304647, 0, 116080197, 116080197, 0, 113025455, 113025455, 0, 110127366, 110127366, 0, 107374182, 107374182, 0, 104755299, 104755299, 0, 102261126, 102261126, 0, 99882960, 99882960, 0, 97612893, 97612893, 0, 95443717, 95443717, 0, 93368854, 93368854, 0, 91382282, 91382282, 0, 89478485, 89478485, 0, 87652393, 87652393, 0, 85899345, 85899345, 0, 84215045, 84215045, 0, 82595524, 82595524, 0, 81037118, 81037118, 0, 79536431, 79536431, 0, 78090314, 78090314, 0, 76695844, 76695844, 0, 75350303, 75350303, 0, 74051160, 74051160, 0, 72796055, 72796055, 0, 71582788, 71582788, 0, 70409299, 70409299, 0, 69273666, 69273666, 0, 68174084, 68174084, 0, Integer.MIN_VALUE, 0, 5};
    private final int valuesPerLong;
    private final int divideMul;
    private final long divideMulUnsigned;
    private final int divideAdd;
    private final long divideAddUnsigned;
    private final int divideShift;

    public PaddedPalettedContainer(PalettedContainerType palettedContainerType, byte bitsPerEntry, List<Integer> states, long[] data) {
        super(palettedContainerType, bitsPerEntry, states, data);

        this.valuesPerLong = bitsPerEntry == 0 ? 64 : (char) (64 / getRealUsedBitsPerEntry());

        int i = 3 * (this.valuesPerLong - 1);
        this.divideMul = MAGIC[i];
        this.divideMulUnsigned = Integer.toUnsignedLong(this.divideMul);
        this.divideAdd = MAGIC[i + 1];
        this.divideAddUnsigned = Integer.toUnsignedLong(this.divideAdd);
        this.divideShift = MAGIC[i + 2];
    }

    @Override
    public int get(int index) {
        if ((index < 0) || (index >= this.size)) {
            throw new IndexOutOfBoundsException("Index " + index + " is < than 0 or >= than " + this.size + ".");
        }

        int bitIndex = this.cellIndex(index);
        long l = this.data[bitIndex];
        int j = (index - bitIndex * this.valuesPerLong) * this.getRealUsedBitsPerEntry();
        return (int) (l >> j & this.maxEntryValue);
    }

    @Override
    public void set(int index, int value) {
        if ((index < 0) || (index >= this.size)) {
            throw new IndexOutOfBoundsException("Index " + index + " is < than 0 or >= than " + this.size + ".");
        }
        if ((value < 0) || (value > this.maxEntryValue)) {
            throw new IllegalArgumentException("Value cannot be outside of accepted range.");
        }

        int bitIndex = this.cellIndex(index);
        long l = this.data[bitIndex];
        int j = (index - bitIndex * this.valuesPerLong) * this.getRealUsedBitsPerEntry();
        this.data[bitIndex] = l & ~(this.maxEntryValue << j) | ((long) value & this.maxEntryValue) << j;
    }

    private int cellIndex(int index) {
        return (int) ((long) index * this.divideMulUnsigned + this.divideAddUnsigned >> 32 >> this.divideShift);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PaddedPalettedContainer that) || !super.equals(o))
            return false;
        return valuesPerLong == that.valuesPerLong && divideMul == that.divideMul
                && divideMulUnsigned == that.divideMulUnsigned && divideAdd == that.divideAdd
                && divideAddUnsigned == that.divideAddUnsigned && divideShift == that.divideShift;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valuesPerLong, divideMul, divideMulUnsigned, divideAdd, divideAddUnsigned, divideShift);
    }
}
