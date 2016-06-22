package com.boydti.phider;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.intellectualcrafters.plot.flag.BooleanFlag;
import com.intellectualcrafters.plot.flag.Flags;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.Permissions;
import com.plotsquared.bukkit.util.BukkitUtil;

public class Main extends JavaPlugin implements Listener {
    
    public static BooleanFlag HIDE_FLAG = new BooleanFlag("hide");

    @Override
    public void onEnable() {
        new PacketHandler(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        Flags.registerFlag(HIDE_FLAG);
    }
    
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        final PlotPlayer pp = BukkitUtil.getPlayer(player);
        if (Permissions.hasPermission(pp, "plots.plothider.bypass")) { // Admin bypass
            return;
        }
        Plot plot = pp.getCurrentPlot();
        if (plot != null && (plot.isDenied(pp.getUUID()) || (!plot.isAdded(pp.getUUID()) && HIDE_FLAG.isTrue(plot)))) {
            Location to = event.getTo();
            Location from = event.getFrom();
            if (to.getWorld().equals(from.getWorld()) && to.distanceSquared(from) < 8) {
                event.setTo(player.getLocation());
                event.setCancelled(true);
                player.setVelocity(player.getVelocity());
            }
        }
    }
    
    
}
