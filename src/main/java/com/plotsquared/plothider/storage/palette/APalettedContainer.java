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

import com.plotsquared.core.PlotSquared;
import com.plotsquared.plothider.storage.BlockStorage;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Abstract class for the implementation of a palette container storage.
 * This format is used by Minecraft to store various elements such as blocks and biome in chunk sections.
 *
 * @since TODO
 */
public abstract class APalettedContainer {

    public static final int CHUNK_SECTION_SIZE = 16 * 16 * 16;
    public static final int BIOME_SECTION_SIZE = 4 * 4 * 4;
    public static final byte DIRECT_PALETTE_SIZE = 14;
    public static final int AIR = 0;

    protected final PalettedContainerType palettedContainerType;
    protected byte bitsPerEntry;
    protected final List<Integer> states;
    protected final long[] data;

    protected int size;
    protected final long maxEntryValue;

    public APalettedContainer(PalettedContainerType palettedContainerType, byte bitsPerEntry, List<Integer> states, long[] data) {
        // Yes, bitsPerEntry may be equal to 0. https://wiki.vg/Chunk_Format#Data_structure
        if ((bitsPerEntry < 0)) {
            throw new IllegalArgumentException(
                    "BitsPerEntry cannot be outside of accepted range. " + bitsPerEntry);
        }
        this.palettedContainerType = palettedContainerType;
        this.bitsPerEntry = bitsPerEntry;
        this.states = states;
        this.data = data;

        this.size = palettedContainerType == PalettedContainerType.BLOCKS
                ? APalettedContainer.CHUNK_SECTION_SIZE
                : APalettedContainer.BIOME_SECTION_SIZE;
        this.maxEntryValue = ((1L << this.bitsPerEntry) - 1);
    }

    public byte getBitsPerEntry() {
        return bitsPerEntry;
    }

    public byte getRealUsedBitsPerEntry() {
        return getRealUsedBits(this.bitsPerEntry);
    }

    public List<Integer> getStates() {
        return states;
    }

    public int getSize() {
        return size;
    }

    public abstract int get(int index);

    public abstract void set(int index, int value);

    public void write(ByteArrayOutputStream out) {
        out.write(this.bitsPerEntry);
        if (this.bitsPerEntry <= 8) {
            if (this.bitsPerEntry == 0) {
                BlockStorage.writeVarInt(out, this.states.get(0));
            } else {
                BlockStorage.writeVarInt(out, this.states.size());
                for (int state : this.states) {
                    BlockStorage.writeVarInt(out, state);
                }
            }
        }
        BlockStorage.writeVarInt(out, data.length);
        BlockStorage.writeLongs(out, data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof APalettedContainer that))
            return false;
        return bitsPerEntry == that.bitsPerEntry && size == that.size && maxEntryValue == that.maxEntryValue
                && palettedContainerType == that.palettedContainerType && states.equals(that.states)
                && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(palettedContainerType, bitsPerEntry, states, size, maxEntryValue);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    public static APalettedContainer build(PalettedContainerType palettedContainerType, byte bitsPerEntry, List<Integer> states, long[] data) {
        // Special case for single-valued palette.
        if (bitsPerEntry == 0)
            return new SinglePalettedContainer(palettedContainerType, bitsPerEntry, states, data);

        // 1.16+
        if (PlotSquared.platform().serverVersion()[1] >= 16)
            return new PaddedPalettedContainer(palettedContainerType, bitsPerEntry, states, data);

        // 1.15-
        return new LegacyPalettedContainer(palettedContainerType, bitsPerEntry, states, data);
    }

    public static byte getRealUsedBits(byte bits) {
        if (bits == 0)
            return 0;
        if (bits > 8)
            return DIRECT_PALETTE_SIZE;

        return bits;
    }
}
