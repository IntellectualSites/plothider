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

public class LegacyPalettedContainer extends APalettedContainer {

    public LegacyPalettedContainer(PalettedContainerType palettedContainerType, byte bitsPerEntry, List<Integer> states, long[] data) {
        super(palettedContainerType, bitsPerEntry, states, data);
    }

    @Override
    public int get(int index) {
        if ((index < 0) || (index >= this.size)) {
            throw new IndexOutOfBoundsException("Index " + index + " is < than 0 or >= than " + this.size + ".");
        }

        int bitIndex = index * this.getRealUsedBitsPerEntry();
        int startIndex = bitIndex / 64;
        int endIndex = ((index + 1) * this.getRealUsedBitsPerEntry() - 1) / 64;
        int startBitSubIndex = bitIndex % 64;
        if (startIndex == endIndex) {
            return (int) (this.data[startIndex] >>> startBitSubIndex & this.maxEntryValue);
        }
        int endBitSubIndex = 64 - startBitSubIndex;
        return (int) (
                (this.data[startIndex] >>> startBitSubIndex | this.data[endIndex] << endBitSubIndex)
                        & this.maxEntryValue);
    }

    @Override
    public void set(int index, int value) {
        if ((index < 0) || (index >= this.size)) {
            throw new IndexOutOfBoundsException("Index " + index + " is < than 0 or >= than " + this.size + ".");
        }
        if ((value < 0) || (value > this.maxEntryValue)) {
            throw new IllegalArgumentException("Value cannot be outside of accepted range.");
        }

        int bitIndex = index * this.getRealUsedBitsPerEntry();
        int startIndex = bitIndex / 64;
        int endIndex = ((index + 1) * this.getRealUsedBitsPerEntry() - 1) / 64;
        int startBitSubIndex = bitIndex % 64;
        this.data[startIndex] = (this.data[startIndex] & ~(this.maxEntryValue << startBitSubIndex)
                | (value & this.maxEntryValue) << startBitSubIndex);
        if (startIndex != endIndex) {
            int endBitSubIndex = 64 - startBitSubIndex;
            this.data[endIndex] = (this.data[endIndex] >>> endBitSubIndex << endBitSubIndex
                    | (value & this.maxEntryValue) >> endBitSubIndex);
        }
    }
}
