/*
 * PlotHider, an addon to hide plots for the PlotSquared plugin for Minecraft.
 * Copyright (C) IntellectualSites <https://intellectualsites.com>
 * Copyright (C) IntellectualSites team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import com.comphenix.protocol.wrappers.WrappedLevelChunkData;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.google.common.primitives.Shorts;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.location.World;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.plothider.storage.BlockStorage;
import com.plotsquared.plothider.storage.palette.PalettedContainer;
import com.plotsquared.plothider.storage.palette.PalettedContainerType;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class PacketHandler {

    private static final int SECTIONS_PER_CHUNK = (PlotSquared.platform().versionMaxHeight() + 1 - PlotSquared.platform().versionMinHeight()) >> 4;

    PacketHandler(PlotHiderPlugin main) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(
                new PacketAdapter(main, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_CHANGE) {
                    public void onPacketSending(PacketEvent event) {
                        Player player = event.getPlayer();
                        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
                        World<?> world = plotPlayer.getLocation().getWorld();
                        if (!canHide(plotPlayer, world)) {
                            return;
                        }

                        PacketContainer packet = event.getPacket();
                        StructureModifier<BlockPosition> positions = packet.getBlockPositionModifier();
                        BlockPosition position = positions.read(0);
                        Location loc = Location.at(world, position.getX(), 0, position.getZ());
                        Plot plot = loc.getOwnedPlot();
                        if (shouldHide(plot, plotPlayer)) {
                            event.setCancelled(true);
                        }
                    }
                });

        manager.addPacketListener(new PacketAdapter(main, ListenerPriority.NORMAL,
                PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
                World<?> world = plotPlayer.getLocation().getWorld();
                if (!canHide(plotPlayer, world)) {
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
                plot1 = shouldHide(plot1, plotPlayer) ? plot1 : null;
                plot2 = shouldHide(plot2, plotPlayer) ? plot2 : null;
                plot3 = shouldHide(plot3, plotPlayer) ? plot3 : null;
                plot4 = shouldHide(plot4, plotPlayer) ? plot4 : null;
                if (plot1 == null && plot2 == null && plot3 == null && plot4 == null) { // No plots to hide
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
                Plot denied = plot1 != null ? plot1 : plot2 != null ? plot2 : plot3 != null ? plot3 : plot4;
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
                        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
                        World<?> world = plotPlayer.getLocation().getWorld();
                        if (!canHide(plotPlayer, world)) {
                            return;
                        }

                        PacketContainer packet = event.getPacket();
                        StructureModifier<Integer> ints = packet.getIntegers();

                        // 1.18+
                        StructureModifier<WrappedLevelChunkData.ChunkData> chunkDataModifier = packet.getLevelChunkData();
                        WrappedLevelChunkData.ChunkData chunkData = chunkDataModifier.read(0);

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

                        plot1 = shouldHide(plot1, plotPlayer) ? plot1 : null;
                        plot2 = shouldHide(plot2, plotPlayer) ? plot2 : null;
                        plot3 = shouldHide(plot3, plotPlayer) ? plot3 : null;
                        plot4 = shouldHide(plot4, plotPlayer) ? plot4 : null;

                        if (plot1 == null && plot2 == null && plot3 == null
                                && plot4 == null) { // No plots to hide
                            return;
                        }

                        if (plot1 == plot4 && plot1 != null) { // Not allowed to see the entire chunk
                            if (chunkData != null) {
                                // 1.18+
                                // Replaces buffer, i.e. chunk data.
                                chunkData.setBuffer(new byte[chunkData.getBuffer().length]);
                                chunkData.setBlockEntityInfo(Collections.emptyList());
                            } else if (byteArrays != null) {
                                // 1.17-
                                byteArrays.write(0, new byte[byteArrays.read(0).length]);
                                nbtLists.write(0, new ArrayList<>());
                            }
                            event.setPacket(packet);
                            return;
                        }

                        // Not allowed to see part of the chunk
                        Plot denied = plot1 != null ?
                                plot1 :
                                plot2 != null ? plot2 : plot3 != null ? plot3 : plot4;
                        PlotArea area = denied.getArea();

                        /*
                         * With 1.17, block section count depends on variable world height
                         * represented as a BitSet internally. We just use a BitSet for older
                         * versions too because it performs good enough.
                         */
                        BitSet bitMask;
                        int sectionsSize = 0;
                        if (PlotSquared.platform().serverVersion()[1] >= 17) {
                            if (chunkData != null) {
                                // 1.18+
                                sectionsSize = SECTIONS_PER_CHUNK;
                            } else {
                                // 1.17 unique BitSet behavior.
                                bitMask = packet.getSpecificModifier(BitSet.class).read(0);

                                for (int layer = 0; layer < bitMask.size(); layer++) {
                                    if (bitMask.get(layer))
                                        sectionsSize += 1;
                                }
                            }
                        } else {
                            int bitMaskLegacy = ints.read(2);
                            bitMask = intToBitSet(bitMaskLegacy);

                            for (int layer = 0; layer < bitMask.size(); layer++) {
                                if (bitMask.get(layer))
                                    sectionsSize += 1;
                            }
                        }

                        byte[] sections;
                        if (chunkData != null) {
                            // 1.18+
                            sections = chunkData.getBuffer();
                        } else {
                            // 1.17-
                            sections = byteArrays.read(0);
                        }

                        ByteArrayInputStream buffer = new ByteArrayInputStream(sections);
                        int size = sections.length;

                        List<BlockStorage> array = new ArrayList<>();

                        try {
                            for (int layer = 0; layer < sectionsSize; layer++) {
                                // Both values represent a short.
                                int blockCountHigh = buffer.read();
                                int blockCountLow = buffer.read();

                                short blockCount = (short) ((blockCountHigh & 0xFF) << 8 | (blockCountLow & 0xFF));

                                BlockStorage storage = new BlockStorage(blockCount);

                                storage.read(buffer, PalettedContainerType.BLOCKS);
                                if (chunkData != null) {
                                    // 1.18+, also contains biomes.
                                    storage.read(buffer, PalettedContainerType.BIOMES);
                                }

                                array.add(storage);
                            }

                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z < 16; z++) {
                                    Location loc = Location.at(world, bx + x, 0, bz + z);
                                    Plot current = area.getOwnedPlot(loc);
                                    if (current != null) {
                                        if ((current == plot1) || (current == plot2) || (current == plot3)
                                                || (current == plot4)) {
                                            for (BlockStorage section1 : array) {
                                                for (int y = 0; y < 16; y++) {
                                                    if (section1.getBlock(x, y, z) != PalettedContainer.AIR) {
                                                        section1.setBlock(x, y, z, PalettedContainer.AIR);
                                                    }
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

                            if (chunkData != null) {
                                // 1.18+
                                chunkData.setBuffer(baos.toByteArray());

                                // Also remove block entities.
                                List<WrappedLevelChunkData.BlockEntityInfo> newBlockEntities = new ArrayList<>();
                                for (WrappedLevelChunkData.BlockEntityInfo blockEntity : chunkData.getBlockEntityInfo()) {
                                    Location loc = Location.at(world, bx + blockEntity.getSectionX(), blockEntity.getY(), bz + blockEntity.getSectionZ());
                                    Plot current = area.getOwnedPlot(loc);
                                    if (current == null || ((current != plot1) && (current != plot2) && (current
                                            != plot3) && (current != plot4))) {
                                        newBlockEntities.add(blockEntity);
                                    }
                                }

                                chunkData.setBlockEntityInfo(newBlockEntities);
                            } else {
                                // 1.17-
                                byteArrays.write(0, baos.toByteArray());
                            }

                            event.setPacket(packet);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });

        manager.addPacketListener(new PacketAdapter(main, ListenerPriority.NORMAL,
                PacketType.Play.Server.SPAWN_ENTITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
                World<?> world = plotPlayer.getLocation().getWorld();
                if (!canHide(plotPlayer, world)) {
                    return;
                }

                PacketContainer packet = event.getPacket();
                StructureModifier<Double> doubles = packet.getDoubles();
                double x = doubles.read(0);
                double y = doubles.read(1);
                double z = doubles.read(2);

                Location loc = Location.at(world, (int) x, (int) y, (int) z);
                Plot plot = loc.getOwnedPlot();
                if (shouldHide(plot, plotPlayer)) {
                    event.setCancelled(true);
                }
            }
        });
    }

    private boolean canHide(PlotPlayer<?> plotPlayer, World<?> world) {
        // Check admin bypass and existing plot area.
        return !plotPlayer.hasPermission("plots.plothider.bypass") && hasPlotArea(world);
    }

    private boolean hasPlotArea(World<?> world) {
        return PlotSquared.get().getPlotAreaManager().hasPlotArea(world.getName());
    }

    private boolean shouldHide(Plot plot, PlotPlayer<?> plotPlayer) {
        return plot != null && (plot.isDenied(plotPlayer.getUUID()) || (!plot.isAdded(plotPlayer.getUUID())
                && plot.getFlag(HideFlag.class)));
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

}
