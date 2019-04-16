package com.boydti.phider;

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
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.github.intellectualsites.plotsquared.bukkit.util.BukkitUtil;
import com.github.intellectualsites.plotsquared.plot.PlotSquared;
import com.github.intellectualsites.plotsquared.plot.object.Location;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.github.intellectualsites.plotsquared.plot.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class PacketHandler {
    PacketHandler(Main main) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(
            new PacketAdapter(main, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_CHANGE) {
                public void onPacketSending(PacketEvent event) {
                    Player player = event.getPlayer();
                    PlotPlayer pp = BukkitUtil.getPlayer(player);
                    if (Permissions.hasPermission(pp, "plots.plothider.bypass")) { // Admin bypass
                        return;
                    }
                    String world = pp.getLocation().getWorld();
                    if (!PlotSquared.get().hasPlotArea(world)) { // Not a plot area
                        return;
                    }
                    PacketContainer packet = event.getPacket();
                    StructureModifier<BlockPosition> positions = packet.getBlockPositionModifier();
                    BlockPosition position = positions.read(0);
                    Location loc = new Location(world, position.getX(), 0, position.getZ());
                    Plot plot = loc.getOwnedPlot();
                    if (plot != null && (plot.isDenied(pp.getUUID()) || (!plot.isAdded(pp.getUUID()) && Main.HIDE_FLAG.isTrue(plot)))) {
                        event.setCancelled(true);
                    }
                }
        });

        manager.addPacketListener(new PacketAdapter(main, ListenerPriority.NORMAL,
            PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PlotPlayer pp = BukkitUtil.getPlayer(player);
                if (Permissions.hasPermission(pp, "plots.plothider.bypass")) { // Admin bypass
                    return;
                }
                String world = pp.getLocation().getWorld();
                if (!PlotSquared.get().hasPlotArea(world)) { // Not a plot area
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
                plot1 = (plot1 != null && (plot1.isDenied(pp.getUUID()) || (!plot1.isAdded(pp.getUUID()) && Main.HIDE_FLAG.isTrue(plot1)))) ? plot1 : null;
                plot2 = (plot2 != null && (plot2.isDenied(pp.getUUID()) || (!plot2.isAdded(pp.getUUID()) && Main.HIDE_FLAG.isTrue(plot2)))) ? plot2 : null;
                plot3 = (plot3 != null && (plot3.isDenied(pp.getUUID()) || (!plot3.isAdded(pp.getUUID()) && Main.HIDE_FLAG.isTrue(plot3)))) ? plot3 : null;
                plot4 = (plot4 != null && (plot4.isDenied(pp.getUUID()) || (!plot4.isAdded(pp.getUUID()) && Main.HIDE_FLAG.isTrue(plot4)))) ? plot4 : null;
                if (plot1 == null && plot2 == null && plot3 == null && plot4 == null) { // No plots to hide
                    return;
                }
                StructureModifier<MultiBlockChangeInfo[]> changeArray = packet.getMultiBlockChangeInfoArrays();
                if (plot1 == plot4 && plot1 != null) { // Not allowed to see the entire chunk
                    event.setCancelled(true);
                    return;
                }
                // Hide some of the blocks (but maybe not all)
                List<MultiBlockChangeInfo> changes = new ArrayList(Arrays.asList((Object[]) changeArray.read(0)));
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
//                changeArray.write(0, changes.toArray(new MultiBlockChangeInfo[changes.size()]));
                changeArray.write(0, changes.toArray(new MultiBlockChangeInfo[0]));
                event.setPacket(packet);
            }
        });

        manager.addPacketListener(
            new PacketAdapter(main, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK) {
                public void onPacketSending(PacketEvent event) {
                    Player player = event.getPlayer();
                    PlotPlayer pp = BukkitUtil.getPlayer(player);
                    if (Permissions.hasPermission(pp, "plots.plothider.bypass")) { // Admin bypass
                        return;
                    }
                    String world = pp.getLocation().getWorld();
                    if (!PlotSquared.get().hasPlotArea(world)) { // Not a plot area
                        return;
                    }
                    PacketContainer packet = event.getPacket();
                    StructureModifier<Integer> ints = packet.getIntegers();
                    StructureModifier<byte[]> byteArray = packet.getByteArrays();
                    StructureModifier<Boolean> booleans = packet.getBooleans();
                    int cx = ints.read(0);
                    int cz = ints.read(1);
                    int bx = cx << 4;
                    int bz = cz << 4;
                    boolean biomes = booleans.read(0);
                    Location corner1 = new Location(world, bx, 0, bz);
                    Location corner2 = new Location(world, bx + 15, 0, bz);
                    Location corner3 = new Location(world, bx, 0, bz + 15);
                    Location corner4 = new Location(world, bx + 15, 0, bz + 15);
                    Plot plot1 = corner1.getOwnedPlot();
                    Plot plot2 = corner2.getOwnedPlot();
                    Plot plot3 = corner3.getOwnedPlot();
                    Plot plot4 = corner4.getOwnedPlot();
                    plot1 = (plot1 != null && (plot1.isDenied(pp.getUUID()) || (!plot1.isAdded(pp.getUUID()) && Main.HIDE_FLAG.isTrue(plot1)))) ? plot1 : null;
                    plot2 = (plot2 != null && (plot2.isDenied(pp.getUUID()) || (!plot2.isAdded(pp.getUUID()) && Main.HIDE_FLAG.isTrue(plot2)))) ? plot2 : null;
                    plot3 = (plot3 != null && (plot3.isDenied(pp.getUUID()) || (!plot3.isAdded(pp.getUUID()) && Main.HIDE_FLAG.isTrue(plot3)))) ? plot3 : null;
                    plot4 = (plot4 != null && (plot4.isDenied(pp.getUUID()) || (!plot4.isAdded(pp.getUUID()) && Main.HIDE_FLAG.isTrue(plot4)))) ? plot4 : null;
                    if (plot1 == null && plot2 == null && plot3 == null && plot4 == null) { // No plots to hide
                        return;
                    }
                    if (plot1 == plot4 && plot1 != null) { // Not allowed to see the entire chunk
                        byteArray.write(0, new byte[byteArray.read(0).length]);
                        StructureModifier<List<NbtBase<?>>> list = packet.getListNbtModifier();
                        list.write(0, new ArrayList<>());
                        event.setPacket(packet);
                        return;
                    }
                    // Not allowed to see part of the chunk
                    Plot denied = plot1 != null ? plot1 : plot2 != null ? plot2 : plot3 != null ? plot3 : plot4;
                    PlotArea area = denied.getArea();
                    int AIR = 0;
                    byte[] sections = byteArray.read(0);
                    int size = sections.length;
                    int datasize = sections.length;
                    List<BlockStorage> array = new ArrayList<>();
                    int layer = 0;
                    byte[] biomearray;
                    ByteArrayInputStream buffer;
                    int bitMask = ints.read(2);
                    if (biomes) {
                        buffer = new ByteArrayInputStream(Arrays.copyOfRange(sections, 0,
                                sections.length - 256 * Integer.bitCount(bitMask)));
                        biomearray = Arrays.copyOfRange(sections,
                                sections.length - 256 * Integer.bitCount(bitMask), sections.length);
                        datasize -= 256 * Integer.bitCount(bitMask);
                    } else {
                        buffer = new ByteArrayInputStream(sections);
                        biomearray = null;
                    }

                    try {
                        boolean sky =
                                Bukkit.getWorld(world).getEnvironment() == World.Environment.NORMAL;

                        byte[] section;
                        for (layer = 0; layer < 16; layer++) {
                            if ((bitMask >> layer & 0x1) == 1) {
                                int start = datasize - buffer.available();

                                int bitsperBlock = readVarInt(buffer);

                                int paletteLength = readVarInt(buffer);

                                for (int i = 0; i < paletteLength; i++) {
                                    readVarInt(buffer);
                                }

                                int dataArrayLength = readVarInt(buffer);

                                for (int i = 0; i < bitsperBlock * 512; i++) {
                                    readVarInt(buffer);
                                }

                                buffer.skip(sky ? 4096 : 2048);

                                int end = datasize - buffer.available();

                                section = Arrays.copyOfRange(sections, start, end);

                                BlockStorage storage = new BlockStorage(section, sky);
                                array.add(storage);
                            }
                        }
                        int z;
                        for (int x = 0; x < 16; x++) {
                            for (z = 0; z < 16; z++) {
                                Location loc = new Location(world, bx + x, 0, bz + z);
                                Plot current = area.getOwnedPlot(loc);
                                if (current != null) {
                                    if ((current == plot1) || (current == plot2) || (current
                                            == plot3) || (current == plot4)) {
                                        for (BlockStorage section1 : array) {
                                            for (int y = 0; y < 16; y++) {
                                                if (section1.get(x, y, z) != 0) {
                                                    section1.set(x, y, z, AIR);
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
                        if (biomearray != null) {
                            baos.write(biomearray);
                        }
                        byteArray.write(0, baos.toByteArray());

                        event.setPacket(packet);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
    }

    private int readVarInt(InputStream stream) throws IOException {
        int i = 0;
        int j = 0;

        byte b0;
        do {
            b0 = (byte) stream.read();
            i |= (b0 & 127) << j++ * 7;
            if(j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while((b0 & 128) == 128);

        return i;
    }
}
