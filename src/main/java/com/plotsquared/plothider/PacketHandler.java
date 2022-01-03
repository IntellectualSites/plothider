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

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.google.common.primitives.Shorts;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.location.World;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

public class PacketHandler {
    // would be better to check the protocol version but who cares
    private static final boolean bitSetChunkSections = Bukkit.getUnsafe().getDataVersion() >= 2724;

    PacketHandler(PlotHiderPlugin main) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(
                new PacketAdapter(main, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_CHANGE) {
                    public void onPacketSending(PacketEvent event) {
                        Player player = event.getPlayer();
                        PlotPlayer<?> pp = BukkitUtil.adapt(player);
                        if (Permissions.hasPermission(pp, "plots.plothider.bypass")) { // Admin bypass
                            return;
                        }
                        World<?> world = pp.getLocation().getWorld();
                        if (!hasPlotArea(world)) { // Not a plot area
                            return;
                        }
                        PacketContainer packet = event.getPacket();
                        StructureModifier<BlockPosition> positions = packet.getBlockPositionModifier();
                        BlockPosition position = positions.read(0);
                        Location loc = Location.at(world, position.getX(), 0, position.getZ());
                        Plot plot = loc.getOwnedPlot();
                        if (plot != null && (plot.isDenied(pp.getUUID()) || (!plot.isAdded(pp.getUUID())
                                && plot.getFlag(HideFlag.class)))) {
                            event.setCancelled(true);
                        }
                    }
                });

        manager.addPacketListener(new PacketAdapter(main, ListenerPriority.NORMAL,
                PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PlotPlayer<?> pp = BukkitUtil.adapt(player);
                if (Permissions.hasPermission(pp, "plots.plothider.bypass")) { // Admin bypass
                    return;
                }
                World<?> world = pp.getLocation().getWorld();
                if (!hasPlotArea(world)) { // Not a plot area
                    return;
                }
                PacketContainer packet = event.getPacket();
                StructureModifier<BlockPosition> sectionPositions = packet.getSectionPositions();
                BlockPosition sectionPosition = sectionPositions.read(0);
                ChunkCoordIntPair chunk = new ChunkCoordIntPair(sectionPosition.getX(), sectionPosition.getZ());
                int cx = chunk.getChunkX();
                int cz = chunk.getChunkZ();
                int bx = cx << 4;
                int bz = cz << 4;
                Location corner1 = Location.at(world, bx, 0, bz);
                Location corner2 = Location.at(world, bx + 15, 0, bz);
                Location corner3 = Location.at(world, bx, 0, bz + 15);
                Location corner4 = Location.at(world, bx + 15, 0, bz + 15);
                Plot plot1 = corner1.getOwnedPlot();
                Plot plot2 = corner2.getOwnedPlot();
                Plot plot3 = corner3.getOwnedPlot();
                Plot plot4 = corner4.getOwnedPlot();
                plot1 =
                        (plot1 != null && (plot1.isDenied(pp.getUUID()) || (!plot1.isAdded(pp.getUUID())
                                && plot1.getFlag(HideFlag.class)))) ? plot1 : null;
                plot2 =
                        (plot2 != null && (plot2.isDenied(pp.getUUID()) || (!plot2.isAdded(pp.getUUID())
                                && plot2.getFlag(HideFlag.class)))) ? plot2 : null;
                plot3 =
                        (plot3 != null && (plot3.isDenied(pp.getUUID()) || (!plot3.isAdded(pp.getUUID())
                                && plot3.getFlag(HideFlag.class)))) ? plot3 : null;
                plot4 =
                        (plot4 != null && (plot4.isDenied(pp.getUUID()) || (!plot4.isAdded(pp.getUUID())
                                && plot4.getFlag(HideFlag.class)))) ? plot4 : null;
                if (plot1 == null && plot2 == null && plot3 == null
                        && plot4 == null) { // No plots to hide
                    return;
                }
                if (plot1 == plot4 && plot1 != null) { // Not allowed to see the entire chunk
                    event.setCancelled(true);
                    return;
                }
                StructureModifier<short[]> positionsArray =
                        packet.getShortArrays();
                StructureModifier<WrappedBlockData[]> blocksArray =
                        packet.getBlockDataArrays();
                // Hide some of the blocks (but maybe not all)
                List<Short> positions =
                        new ArrayList<>(Shorts.asList(positionsArray.read(0)));
                List<WrappedBlockData> blocks =
                        new ArrayList<>(Arrays.asList(blocksArray.read(0)));
                // Both lists have same length.
                Iterator<Short> positionsIter = positions.iterator();
                Iterator<WrappedBlockData> blocksIter = blocks.iterator();
                Plot denied =
                        plot1 != null ? plot1 : plot2 != null ? plot2 : plot3 != null ? plot3 : plot4;
                PlotArea area = denied.getArea();
                while (positionsIter.hasNext()) {
                    short change = positionsIter.next();
                    blocksIter.next();
                    // Binary operators give section-relative coordinates.
                    int x = bx + (change >>> 8 & 15);
                    int z = bz + (change >>> 4 & 15);
                    Plot current = area.getOwnedPlot(Location.at(world, x, 0, z));
                    if (current == null) {
                        continue;
                    }
                    if (current == plot1 || current == plot2 || current == plot3
                            || current == plot4) {
                        positionsIter.remove();
                        blocksIter.remove();
                    }
                }
                if (positions.size() == 0) {
                    event.setCancelled(true);
                    return;
                }
                positionsArray.write(0, Shorts.toArray(positions));
                blocksArray.write(0, blocks.toArray(new WrappedBlockData[0]));
                event.setPacket(packet);
            }
        });

        manager.addPacketListener(
                new PacketAdapter(main, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK) {
                    public void onPacketSending(PacketEvent event) {
                        Player player = event.getPlayer();
                        PlotPlayer<?> pp = BukkitUtil.adapt(player);
                        if (Permissions.hasPermission(pp, "plots.plothider.bypass")) { // Admin bypass
                            return;
                        }

                        World<?> world = pp.getLocation().getWorld();
                        if (!hasPlotArea(world)) { // Not a plot area
                            return;
                        }

                        PacketContainer packet = event.getPacket();
                        StructureModifier<Integer> ints = packet.getIntegers();
                        StructureModifier<byte[]> byteArrays = packet.getByteArrays();
                        StructureModifier<List<NbtBase<?>>> nbtLists = packet.getListNbtModifier();

                        // Chunk X,Z & Block X,Z
                        int cx = ints.read(0);
                        int cz = ints.read(1);
                        int bx = cx << 4;
                        int bz = cz << 4;

                        Location corner1 = Location.at(world, bx, 0, bz);
                        Location corner2 = Location.at(world, bx + 15, 0, bz);
                        Location corner3 = Location.at(world, bx, 0, bz + 15);
                        Location corner4 = Location.at(world, bx + 15, 0, bz + 15);
                        Plot plot1 = corner1.getOwnedPlot();
                        Plot plot2 = corner2.getOwnedPlot();
                        Plot plot3 = corner3.getOwnedPlot();
                        Plot plot4 = corner4.getOwnedPlot();

                        plot1 = (plot1 != null && (plot1.isDenied(pp.getUUID()) || (
                                !plot1.isAdded(pp.getUUID()) && plot1.getFlag(HideFlag.class)))) ?
                                plot1 :
                                null;
                        plot2 = (plot2 != null && (plot2.isDenied(pp.getUUID()) || (
                                !plot2.isAdded(pp.getUUID()) && plot2.getFlag(HideFlag.class)))) ?
                                plot2 :
                                null;
                        plot3 = (plot3 != null && (plot3.isDenied(pp.getUUID()) || (
                                !plot3.isAdded(pp.getUUID()) && plot3.getFlag(HideFlag.class)))) ?
                                plot3 :
                                null;
                        plot4 = (plot4 != null && (plot4.isDenied(pp.getUUID()) || (
                                !plot4.isAdded(pp.getUUID()) && plot4.getFlag(HideFlag.class)))) ?
                                plot4 :
                                null;

                        if (plot1 == null && plot2 == null && plot3 == null
                                && plot4 == null) { // No plots to hide
                            return;
                        }

                        if (plot1 == plot4 && plot1 != null) { // Not allowed to see the entire chunk
                            byteArrays.write(0, new byte[byteArrays.read(0).length]);
                            nbtLists.write(0, new ArrayList<>());
                            event.setPacket(packet);
                            return;
                        }

                        // Not allowed to see part of the chunk
                        Plot denied = plot1 != null ?
                                plot1 :
                                plot2 != null ? plot2 : plot3 != null ? plot3 : plot4;
                        PlotArea area = denied.getArea();

                        int AIR = 0;

                        /*
                         * With 1.17, block section count depends on variable world height
                         * represented as a BitSet internally. We just use a BitSet for older
                         * versions too because it performs good enough.
                         */
                        BitSet bitMask;
                        if (bitSetChunkSections) {
                            bitMask = packet.getSpecificModifier(BitSet.class).read(0);
                        } else {
                            int bitMaskLegacy = ints.read(2);
                            bitMask = intToBitSet(bitMaskLegacy);
                        }

                        byte[] sections = byteArrays.read(0);
                        ByteArrayInputStream buffer = new ByteArrayInputStream(sections);
                        int size = sections.length;

                        List<BlockStorage> array = new ArrayList<>();

                        try {
                            for (int layer = 0; layer < bitMask.size(); layer++) {
                                if (bitMask.get(layer)) {

                                    int blockCountHigh = buffer.read();
                                    int blockCountLow = buffer.read();

                                    short blockCount = (short) ((blockCountHigh & 0xFF) << 8 | (blockCountLow & 0xFF));


                                    int bitsPerBlock = buffer.read();

                                    ArrayList<Integer> states = new ArrayList<>();
                                    if (bitsPerBlock <= 8) {
                                        int paletteLength = readVarInt(buffer);
                                        states.ensureCapacity(paletteLength);
                                        for (int i = 0; i < paletteLength; i++) {
                                            states.add(readVarInt(buffer));
                                        }
                                    }

                                    int dataArrayLength = readVarInt(buffer);

                                    // read the actual (compressed) block data
                                    long[] data = readLongs(buffer, dataArrayLength);

                                    BlockStorage storage = new BlockStorage(blockCount, bitsPerBlock, states, data);
                                    array.add(storage);
                                }
                            }

                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z < 16; z++) {
                                    Location loc = Location.at(world, bx + x, 0, bz + z);
                                    Plot current = area.getOwnedPlot(loc);
                                    if (current != null) {
                                        if ((current == plot1) || (current == plot2) || (current
                                                == plot3) || (current == plot4)) {
                                            for (BlockStorage section1 : array) {
                                                for (int y = 0; y < 16; y++) {
                                                    // if (section1.get(x, y, z) != 0) {
                                                        section1.set(x, y, z, AIR);
                                                    // }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
                            for (BlockStorage section1 : array) {
                                section1.write(baos);
                            }
                            byteArrays.write(0, baos.toByteArray());

                            List<NbtBase<?>> nbtList = nbtLists.read(0);

                            event.setPacket(packet);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private boolean hasPlotArea(World<?> world) {
        return PlotSquared.get().getPlotAreaManager().hasPlotArea(world.getName());
    }

    private int readVarInt(InputStream stream) throws IOException {
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

    private static BitSet intToBitSet(int n) {
        BitSet bits = new BitSet(Integer.SIZE);
        for (int i = 0; i < Integer.SIZE; i++) {
            if (((n >> i) & 0x1) == 1) {
                bits.set(i);
            }
        }
        return bits;
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
}
