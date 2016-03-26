package com.boydti.phider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.mutable.MutableInt;

import com.intellectualcrafters.plot.object.PlotBlock;

public class BlockStorage {
    private static final PlotBlock AIR = new PlotBlock((short) 0, (byte) 0);
    
    private int bitsPerEntry;
    
    private final List<PlotBlock> states;
    private FlexibleStorage storage;
    private final byte[] light;
    private final int size;
    
    public BlockStorage(byte[] in) throws IOException {
        MutableInt mut = new MutableInt();
        this.bitsPerEntry = readUnsigned(in, mut);
        
        this.states = new ArrayList<PlotBlock>();
        
        int stateCount = readVarInt(in, mut);
        
        for (int i = 0; i < stateCount; i++) {
            this.states.add(readPlotBlock(in, mut));
        }
        
        int expected = readVarInt(in, mut);
        this.storage = new FlexibleStorage(this.bitsPerEntry, readLongs(in, mut, expected));
        if (in[mut.intValue()] == 0) {
            this.light = Arrays.copyOfRange(in, mut.intValue(), mut.intValue() + 4096);
        } else {
            this.light = new byte[0];
        }
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
        for (PlotBlock state : this.states) {
            writeBlockState(out, state);
        }
        writeVarInt(out, data.length);
        writeLongs(out, data);
        out.write(light);
    }
    
    public int getBitsPerEntry() {
        return this.bitsPerEntry;
    }
    
    public List<PlotBlock> getStates() {
        return Collections.unmodifiableList(this.states);
    }
    
    public FlexibleStorage getStorage() {
        return this.storage;
    }
    
    public PlotBlock get(int x, int y, int z) {
        int id = this.storage.get(index(x, y, z));
        return this.bitsPerEntry <= 8 ? (id >= 0 && id < this.states.size() ? this.states.get(id) : AIR) : rawToState(id);
    }
    
    public void set(int x, int y, int z, PlotBlock state) {
        int id = this.bitsPerEntry <= 8 ? (state.id == 0 ? 0 : this.states.indexOf(state)) : stateToRaw(state);
        if (id == -1) {
            this.states.add(state);
            if (this.states.size() > 1 << this.bitsPerEntry) {
                this.bitsPerEntry++;
                
                List<PlotBlock> oldStates = this.states;
                if (this.bitsPerEntry > 8) {
                    oldStates = new ArrayList<PlotBlock>(this.states);
                    this.states.clear();
                    this.bitsPerEntry = 13;
                }
                
                FlexibleStorage oldStorage = this.storage;
                this.storage = new FlexibleStorage(this.bitsPerEntry, this.storage.getSize());
                for (int index = 0; index < this.storage.getSize(); index++) {
                    this.storage.set(index, this.bitsPerEntry <= 8 ? oldStorage.get(index) : stateToRaw(oldStates.get(index)));
                }
            }
            
            id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : stateToRaw(state);
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
    
    private static PlotBlock rawToState(int raw) {
        return new PlotBlock((short) (raw >> 4), (byte) (raw & 0xF));
    }
    
    private static int stateToRaw(PlotBlock state) {
        return (state.id << 4) | (state.data & 0xF);
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
    
    public long[] readLongs(byte[] bytes, MutableInt mut, int length) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException("Array cannot have length less than 0.");
        }
        
        long l[] = new long[length];
        for (int index = 0; index < length; index++) {
            l[index] = readLong(bytes, mut);
        }
        
        return l;
    }

    public PlotBlock readPlotBlock(byte[] bytes, MutableInt index) {
        int rawId = readVarInt(bytes, index);
        return new PlotBlock((short) (rawId >> 4), (byte) (rawId & 0xF));
    }
    
    public void writeVarInt(ByteArrayOutputStream out, int i) throws IOException {
        while ((i & ~0x7F) != 0) {
            out.write((i & 0x7F) | 0x80);
            i >>>= 7;
        }
        
        out.write(i);
    }
    
    public void writeBlockState(ByteArrayOutputStream out, PlotBlock blockState) throws IOException {
        writeVarInt(out, (blockState.id << 4) | (blockState.data & 0xF));
    }
    
    public void writeLongs(ByteArrayOutputStream out, long[] l) throws IOException {
        for (int index = 0; index < l.length; index++) {
            writeLong(out, l[index]);
        }
    }
    
    public void writeLong(ByteArrayOutputStream out, long l) throws IOException {
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
