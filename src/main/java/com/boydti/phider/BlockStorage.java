package com.boydti.phider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.mutable.MutableInt;

public class BlockStorage {
    private static final Integer AIR = 0x00;

    private int bitsPerEntry;

    private final List<Integer> states;
    private FlexibleStorage storage;
    private final byte[] light;
    private final int size;

    public BlockStorage(byte[] in, boolean sky) {
        MutableInt mut = new MutableInt();
        this.bitsPerEntry = readUnsigned(in, mut);

        this.states = new LinkedList<>();

        int stateCount = readVarInt(in, mut);

        for (int i = 0; i < stateCount; i++) {
            this.states.add(readVarInt(in, mut));
        }

        int expected = readVarInt(in, mut);
        this.storage = new FlexibleStorage(this.bitsPerEntry, readLongs(in, mut, expected));
        this.light = Arrays.copyOfRange(in, mut.intValue(), in.length);
        this.size = mut.intValue();
    }

    public byte[] getLight() {
        return light;
    }

    public int getSize() {
        return size;
    }

    public byte[] write() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(size + light.length);
        write(out);
        return out.toByteArray();
    }

    public void write(ByteArrayOutputStream out) throws IOException {
        long[] data = this.storage.getData();
        out.write(this.bitsPerEntry);
        writeVarInt(out, this.states.size());
        for (int state : this.states) {
            writeVarInt(out, state);
        }
        writeVarInt(out, data.length);
        writeLongs(out, data);
        out.write(light);
    }

    public int getBitsPerEntry() {
        return this.bitsPerEntry;
    }

    public List<Integer> getStates() {
        return Collections.unmodifiableList(this.states);
    }

    public FlexibleStorage getStorage() {
        return this.storage;
    }

    public int get(int x, int y, int z) {
        int id = this.storage.get(index(x, y, z));
        return this.bitsPerEntry <= 8 ? (id >= 0 && id < this.states.size() ? this.states.get(id) : AIR) : id;
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
                    this.storage.set(index, this.bitsPerEntry <= 8 ? oldStorage.get(index) : oldStates.get(index));
                }
            }

            id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : state;
        }
        this.storage.set(index(x, y, z), id);
    }

    public boolean isEmpty() {
        for (int index = 0; index < this.storage.getSize(); index++) {
            if (this.storage.get(index) != 0) {
                return false;
            }
        }

        return true;
    }

    private static int index(int x, int y, int z) {
        return y << 8 | z << 4 | (x);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockStorage && this.bitsPerEntry == ((BlockStorage) o).bitsPerEntry && this.states.equals(((BlockStorage) o).states) && this.storage.equals(((BlockStorage) o).storage);
    }

    @Override
    public int hashCode() {
        int result = this.bitsPerEntry;
        result = 31 * result + this.states.hashCode();
        result = 31 * result + this.storage.hashCode();
        return result;
    }

    public byte read(byte[] bytes, MutableInt index) {
        byte value = bytes[index.intValue()];
        index.increment();
        return value;
    }

    public int readUnsigned(byte[] bytes, MutableInt index) {
        byte value = bytes[index.intValue()];
        index.increment();
        return value & 0xFF;
    }

    public long readLong(byte[] bytes, MutableInt index) {
        long value = 0;
        for (int i = 0, j = 56; i < 8; i++, j -= 8) {
            value += (bytes[i + index.intValue()] & 0xffL) << (j);
        }
        index.add(8);
        return value;
    }

    public int readVarInt(byte[] bytes, MutableInt index) {
        int value = 0;
        int size = 0;
        int b;
        while (((b = read(bytes, index)) & 0x80) == 0x80) {
            value |= (b & 0x7F) << (size++ * 7);
            if (size > 5) {
                return 1 / 0;
            }
        }

        return value | ((b & 0x7F) << (size * 7));
    }

    public long[] readLongs(byte[] bytes, MutableInt mut, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Array cannot have length less than 0.");
        }

        long l[] = new long[length];
        for (int index = 0; index < length; index++) {
            l[index] = readLong(bytes, mut);
        }

        return l;
    }

    public void writeVarInt(ByteArrayOutputStream out, int i) {
        while ((i & ~0x7F) != 0) {
            out.write((i & 0x7F) | 0x80);
            i >>>= 7;
        }

        out.write(i);
    }

    public void writeLongs(ByteArrayOutputStream out, long[] l) {
        for (int index = 0; index < l.length; index++) {
            writeLong(out, l[index]);
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
        out.write((byte) (l >>> 0));
    }

}
