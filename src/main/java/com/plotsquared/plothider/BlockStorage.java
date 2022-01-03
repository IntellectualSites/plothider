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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class BlockStorage {
    private static final int AIR = 0x00;
    private final List<Integer> states;
    private final short blockCount;
    private int bitsPerEntry;
    private FlexibleStorage storage;

    public BlockStorage(short blockCount, int bitsPerEntry, List<Integer> states, long[] data) {
        this.blockCount = blockCount;
        this.bitsPerEntry = bitsPerEntry;
        this.states = states;
        this.storage = new FlexibleStorage(this.bitsPerEntry, data);
    }

    private static int index(int x, int y, int z) {
        return y << 8 | z << 4 | (x);
    }

    public void write(ByteArrayOutputStream out) {
        long[] data = this.storage.getData();
        writeShort(out, this.blockCount);
        out.write(this.bitsPerEntry);
        if (this.bitsPerEntry <= 8) {
            writeVarInt(out, this.states.size());
            for (int state : this.states) {
                writeVarInt(out, state);
            }
        }
        writeVarInt(out, data.length);
        writeLongs(out, data);
    }

    public int get(int x, int y, int z) {
        int id = this.storage.get(index(x, y, z));
        return this.bitsPerEntry <= 8 ?
                (id >= 0 && id < this.states.size() ? this.states.get(id) : AIR) :
                id;
    }

    public void set(int x, int y, int z, int state) {
        int id = this.bitsPerEntry <= 8 ? (state == 0 ? 0 : this.states.indexOf(state)) : state;
        if (id == -1) {
            this.states.add(state);
            if (this.states.size() > 1 << this.bitsPerEntry) {
                this.bitsPerEntry++;

                List<Integer> oldStates = this.states;
                if (this.bitsPerEntry > 8) {
                    oldStates = new ArrayList<>(this.states);
                    this.states.clear();
                    this.bitsPerEntry = 14;
                }

                FlexibleStorage oldStorage = this.storage;
                this.storage = new FlexibleStorage(this.bitsPerEntry, this.storage.getSize());
                for (int index = 0; index < this.storage.getSize(); index++) {
                    this.storage.set(index,
                            this.bitsPerEntry <= 8 ? oldStorage.get(index) : oldStates.get(index));
                }
            }

            id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : state;
        }
        this.storage.set(index(x, y, z), id);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockStorage && this.bitsPerEntry == ((BlockStorage) o).bitsPerEntry
                && this.states.equals(((BlockStorage) o).states) && this.storage
                .equals(((BlockStorage) o).storage);
    }

    @Override
    public int hashCode() {
        int result = this.bitsPerEntry;
        result = 31 * result + this.states.hashCode();
        result = 31 * result + this.storage.hashCode();
        return result;
    }

    public void writeShort(ByteArrayOutputStream out, short s) {
        out.write((s >> 8) & 0xFF);
        out.write(s & 0xFF);
    }

    public void writeVarInt(ByteArrayOutputStream out, int i) {
        while ((i & ~0x7F) != 0) {
            out.write((i & 0x7F) | 0x80);
            i >>>= 7;
        }

        out.write(i);
    }

    public void writeLongs(ByteArrayOutputStream out, long[] l) {
        for (long value : l) {
            writeLong(out, value);
        }
    }

    public void writeLong(ByteArrayOutputStream out, long l) {
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
