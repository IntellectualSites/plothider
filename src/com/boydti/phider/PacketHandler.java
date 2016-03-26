package com.boydti.phider;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.bukkit.entity.Player;

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
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotBlock;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.Permissions;
import com.plotsquared.bukkit.util.BukkitUtil;

public class PacketHandler {
    public static ProtocolManager manager;
    
    public PacketHandler(Main main) {
        manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(new PacketAdapter(main, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PlotPlayer pp = BukkitUtil.getPlayer(player);
                if (Permissions.hasPermission(pp, "plots.plothider.bypass")) { // Admin bypass
                    return;
                }
                String world = pp.getLocation().getWorld();
                if (!PS.get().hasPlotArea(world)) { // Not a plot area
                    return;
                }
                PacketContainer packet = event.getPacket();
                StructureModifier<BlockPosition> positions = packet.getBlockPositionModifier();
                System.out.println("1SIZE: " + positions.size());
                BlockPosition position = positions.read(0);
                Location loc = new Location(world, position.getX(), 0, position.getZ());
                Plot plot = loc.getOwnedPlot();
                if (plot != null && (plot.isDenied(pp.getUUID()) || (!plot.isAdded(pp.getUUID()) && FlagManager.isPlotFlagTrue(plot, "hide")))) {
                    event.setCancelled(true);
                }
            }
        });
        
        manager.addPacketListener(new PacketAdapter(main, ListenerPriority.NORMAL, PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PlotPlayer pp = BukkitUtil.getPlayer(player);
                if (Permissions.hasPermission(pp, "plots.plothider.bypass")) { // Admin bypass
                    return;
                }
                String world = pp.getLocation().getWorld();
                if (!PS.get().hasPlotArea(world)) { // Not a plot area
                    return;
                }
                PacketContainer packet = event.getPacket();
                StructureModifier<ChunkCoordIntPair> chunkArray = packet.getChunkCoordIntPairs();
                ChunkCoordIntPair chunk = chunkArray.read(0);
                int cx = chunk.getChunkX();
                int cz = chunk.getChunkZ();
                int bx = cx << 4;
                int bz = cz << 4;
                Location corner1 = new Location(world, bx, 0, bz);
                Location corner2 = new Location(world, bx + 15, 0, bz);
                Location corner3 = new Location(world, bx, 0, bz + 15);
                Location corner4 = new Location(world, bx + 15, 0, bz + 15);
                Plot plot1 = corner1.getOwnedPlot();
                Plot plot2 = corner2.getOwnedPlot();
                Plot plot3 = corner3.getOwnedPlot();
                Plot plot4 = corner4.getOwnedPlot();
                plot1 = (plot1 != null && (plot1.isDenied(pp.getUUID()) || (!plot1.isAdded(pp.getUUID()) && FlagManager.isPlotFlagTrue(plot1, "hide")))) ? plot1 : null;
                plot2 = (plot2 != null && (plot2.isDenied(pp.getUUID()) || (!plot2.isAdded(pp.getUUID()) && FlagManager.isPlotFlagTrue(plot2, "hide")))) ? plot2 : null;
                plot3 = (plot3 != null && (plot3.isDenied(pp.getUUID()) || (!plot3.isAdded(pp.getUUID()) && FlagManager.isPlotFlagTrue(plot3, "hide")))) ? plot3 : null;
                plot4 = (plot4 != null && (plot4.isDenied(pp.getUUID()) || (!plot4.isAdded(pp.getUUID()) && FlagManager.isPlotFlagTrue(plot4, "hide")))) ? plot4 : null;
                if (plot1 == null && plot2 == null && plot3 == null && plot4 == null) { // No plots to hide
                    return;
                }
                StructureModifier<MultiBlockChangeInfo[]> changeArray = packet.getMultiBlockChangeInfoArrays();
                if (plot1 == plot4 && plot1 != null) { // Not allowed to see the entire chunk
                    event.setCancelled(true);
                    return;
                }
                // Hide some of the blocks (but maybe not all)
                List<MultiBlockChangeInfo> changes = new ArrayList<>(Arrays.asList(changeArray.read(0)));
                System.out.println("CHANGES: " + changes.size() + " | " + chunkArray.size() + " | " + changeArray.size());
                Iterator<MultiBlockChangeInfo> iter = changes.iterator();
                Plot denied = plot1 != null ? plot1 : plot2 != null ? plot2 : plot3 != null ? plot3 : plot4;
                PlotArea area = denied.getArea();
                while (iter.hasNext()) {
                    MultiBlockChangeInfo change = iter.next();
                    int x = change.getAbsoluteX();
                    int z = change.getAbsoluteZ();
                    Plot current = area.getOwnedPlot(new Location(world, x, 0, z));
                    if (current == null) {
                        continue;
                    }
                    if (current == plot1 || current == plot2 || current == plot3 || current == plot4) {
                        iter.remove();
                    }
                }
                if (changes.size() == 0) {
                    event.setCancelled(true);
                    return;
                }
                changeArray.write(0, changes.toArray(new MultiBlockChangeInfo[changes.size()]));
                event.setPacket(packet);
            }
        });

        manager.addPacketListener(new PacketAdapter(main, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PlotPlayer pp = BukkitUtil.getPlayer(player);
                if (Permissions.hasPermission(pp, "plots.plothider.bypass")) { // Admin bypass
                    return;
                }
                String world = pp.getLocation().getWorld();
                if (!PS.get().hasPlotArea(world)) { // Not a plot area
                    return;
                }
                PacketContainer packet = event.getPacket();
                StructureModifier<Integer> ints = packet.getIntegers();
                StructureModifier<byte[]> byteArray = packet.getByteArrays();
                int cx = ints.read(0);
                int cz = ints.read(1);
                int bx = cx << 4;
                int bz = cz << 4;
                Location corner1 = new Location(world, bx, 0, bz);
                Location corner2 = new Location(world, bx + 15, 0, bz);
                Location corner3 = new Location(world, bx, 0, bz + 15);
                Location corner4 = new Location(world, bx + 15, 0, bz + 15);
                Plot plot1 = corner1.getOwnedPlot();
                Plot plot2 = corner2.getOwnedPlot();
                Plot plot3 = corner3.getOwnedPlot();
                Plot plot4 = corner4.getOwnedPlot();
                plot1 = (plot1 != null && (plot1.isDenied(pp.getUUID()) || (!plot1.isAdded(pp.getUUID()) && FlagManager.isPlotFlagTrue(plot1, "hide")))) ? plot1 : null;
                plot2 = (plot2 != null && (plot2.isDenied(pp.getUUID()) || (!plot2.isAdded(pp.getUUID()) && FlagManager.isPlotFlagTrue(plot2, "hide")))) ? plot2 : null;
                plot3 = (plot3 != null && (plot3.isDenied(pp.getUUID()) || (!plot3.isAdded(pp.getUUID()) && FlagManager.isPlotFlagTrue(plot3, "hide")))) ? plot3 : null;
                plot4 = (plot4 != null && (plot4.isDenied(pp.getUUID()) || (!plot4.isAdded(pp.getUUID()) && FlagManager.isPlotFlagTrue(plot4, "hide")))) ? plot4 : null;
                if (plot1 == null && plot2 == null && plot3 == null && plot4 == null) { // No plots to hide
                    return;
                }
                if (plot1 == plot4 && plot1 != null) { // Not allowed to see the entire chunk
                    byteArray.write(0, new byte[0]);
                    event.setPacket(packet);
                    return;
                }
                // Not allowed to see part of the chunk
                Plot denied = plot1 != null ? plot1 : plot2 != null ? plot2 : plot3 != null ? plot3 : plot4;
                PlotArea area = denied.getArea();
                PlotBlock AIR = new PlotBlock((short) 0, (byte) 0);
                byte[] sections = byteArray.read(0);
                int size = sections.length;
                try {
                    byte[] biomes = Arrays.copyOfRange(sections, sections.length - 256, sections.length);
                    sections = Arrays.copyOfRange(sections, 0, sections.length - 256);
                    List<BlockStorage> array = new ArrayList<>();
                    while (sections.length > 0) {
                        if (sections[0] < 0) {
                            break;
                        }
                        BlockStorage storage = new BlockStorage(sections);
                        array.add(storage);
                        sections = Arrays.copyOfRange(sections, Math.min(storage.getSize() + storage.getLight().length, sections.length), sections.length);
                    }
                    // Trim chunk
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            Location loc = new Location(world, bx + x, 0, bz + z);
                            Plot current = area.getOwnedPlot(loc);
                            if (current == null) {
                                continue;
                            }
                            if (current == plot1 || current == plot2 || current == plot3 || current == plot4) {
                                for (BlockStorage section : array) {
                                    for (int y = 0; y < 16; y++) {
                                        if (section.get(x, y, z).id != 0) {
                                            section.set(x, y, z, AIR);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Write
                    {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
                        for (BlockStorage section : array) {
                            section.write(baos);
                        }
                        baos.write(sections);
                        baos.write(biomes);
                        byteArray.write(0, baos.toByteArray());
                        event.setPacket(packet);
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                }

                
            }
        });
    }
}
