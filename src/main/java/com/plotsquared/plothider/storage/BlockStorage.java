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
package com.plotsquared.plothider.storage;

import com.plotsquared.plothider.storage.palette.APalettedContainer;
import com.plotsquared.plothider.storage.palette.PalettedContainerType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlockStorage {
    private short blockCount;
    private FlexibleStorage storage;

    public BlockStorage(short blockCount) {
        this.blockCount = blockCount;
        this.storage = new FlexibleStorage();
    }

    public void write(ByteArrayOutputStream out) {
        writeShort(out, this.blockCount);
        storage.write(out);
    }

    public void read(ByteArrayInputStream buffer, PalettedContainerType palettedContainerType) {
        try {
            // First part, Block states, as a Paletted Container.
            byte bitsPerEntry = (byte) buffer.read();

            ArrayList<Integer> states = new ArrayList<>();
            if (bitsPerEntry <= 8) {
                // bitsPerBlock == 0 is an edge case to handle.
                if (bitsPerEntry == 0) {
                    int palette = readVarInt(buffer);
                    states.add(palette);
                } else {
                    int paletteLength = readVarInt(buffer);
                    states.ensureCapacity(paletteLength);
                    for (int i = 0; i < paletteLength; i++) {
                        states.add(readVarInt(buffer));
                    }
                }
            }

            int dataArrayLength = readVarInt(buffer);

            // Read the actual (compressed) block data.
            long[] data = BlockStorage.readLongs(buffer, dataArrayLength);

            APalettedContainer palettedContainer = APalettedContainer.build(palettedContainerType, bitsPerEntry, states, data);
            if (palettedContainerType == PalettedContainerType.BLOCKS) {
                storage.setBlocksPalletedContainer(palettedContainer);
            } else {
                storage.setBiomesPalletedContainer(palettedContainer);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public int getBlock(int x, int y, int z) {
        APalettedContainer palettedContainer = this.storage.getBlocksPalletedContainer();

        int id = palettedContainer.get(index(x, y, z));
        return palettedContainer.getBitsPerEntry() <= 8 ?
                (id >= 0 && id < palettedContainer.getStates().size()
                        ? palettedContainer.getStates().get(id)
                        : APalettedContainer.AIR) :
                id;
    }

    public void setBlock(int x, int y, int z, int state) {
        APalettedContainer palettedContainer = this.storage.getBlocksPalletedContainer();
        byte bitsPerEntry = palettedContainer.getBitsPerEntry();

        int id = bitsPerEntry <= 8
                ? palettedContainer.getStates().indexOf(state)
                : state;
        if (id == -1) {
            palettedContainer.getStates().add(state);

            // Update storage if necessary.
            if (palettedContainer.getStates().size() > 1 << bitsPerEntry) {
                bitsPerEntry++;

                List<Integer> oldStates = palettedContainer.getStates();
                if (bitsPerEntry > 8) {
                    oldStates = new ArrayList<>(palettedContainer.getStates());
                    palettedContainer.getStates().clear();
                    bitsPerEntry = APalettedContainer.DIRECT_PALETTE_SIZE;
                }
                FlexibleStorage oldStorage = this.storage;
                this.storage = new FlexibleStorage(bitsPerEntry, palettedContainer.getStates(),
                        palettedContainer.getSize());
                this.storage.setBiomesPalletedContainer(oldStorage.getBiomesPalletedContainer());

                palettedContainer = this.storage.getBlocksPalletedContainer();
                for (int index = 0; index < palettedContainer.getSize(); index++) {
                    palettedContainer.set(index,
                            bitsPerEntry <= 8
                                    ? oldStorage.getBlocksPalletedContainer().get(index)
                                    : oldStates.get(index));
                }
            }

            // Update id.
            id = bitsPerEntry <= 8 ? palettedContainer.getStates().indexOf(state) : state;
        }

        // Make the changes.
        palettedContainer.set(index(x, y, z), id);

        if (state == APalettedContainer.AIR)
            blockCount--;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BlockStorage that))
            return false;
        return blockCount == that.blockCount && storage.equals(that.storage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockCount, storage);
    }

    private static int index(int x, int y, int z) {
        return y << 8 | z << 4 | (x);
    }

    private static int readVarInt(InputStream stream) throws IOException {
        int i = 0;
        int j = 0;

        byte b0;
        do {
            b0 = (byte) stream.read();
            i |= (b0 & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b0 & 128) == 128);

        return i;
    }

    private static long[] readLongs(ByteArrayInputStream buffer, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Array cannot have length less than 0.");
        }

        long[] l = new long[length];
        for (int index = 0; index < length; index++) {
            long result = 0;
            for (int i = 0; i < Long.BYTES; i++) {
                result |= (buffer.read() & 0xFFL) << (i << 3);
            }
            l[index] = Long.reverseBytes(result); // little endian to big endian conversion
        }

        return l;
    }

    public static void writeShort(ByteArrayOutputStream out, short s) {
        out.write((s >> 8) & 0xFF);
        out.write(s & 0xFF);
    }

    public static void writeVarInt(ByteArrayOutputStream out, int i) {
        while ((i & ~0x7F) != 0) {
            out.write((i & 0x7F) | 0x80);
            i >>>= 7;
        }

        out.write(i);
    }

    public static void writeLongs(ByteArrayOutputStream out, long[] l) {
        for (long value : l) {
            writeLong(out, value);
        }
    }

    public static void writeLong(ByteArrayOutputStream out, long l) {
        out.write((byte) (l >>> 56));
        out.write((byte) (l >>> 48));
        out.write((byte) (l >>> 40));
        out.write((byte) (l >>> 32));
        out.write((byte) (l >>> 24));
        out.write((byte) (l >>> 16));
        out.write((byte) (l >>> 8));
        out.write((byte) (l));
    }
}
