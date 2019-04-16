package com.boydti.phider;

import com.github.intellectualsites.plotsquared.bukkit.util.BukkitUtil;
import com.github.intellectualsites.plotsquared.plot.flag.BooleanFlag;
import com.github.intellectualsites.plotsquared.plot.flag.Flags;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.github.intellectualsites.plotsquared.plot.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

    public static BooleanFlag HIDE_FLAG = new BooleanFlag("hide");

    public void onEnable() {
        new PacketHandler(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        Flags.registerFlag(HIDE_FLAG);
    }

    @EventHandler public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlotPlayer pp = BukkitUtil.getPlayer(player);
        if (Permissions.hasPermission(pp, "plots.plothider.bypass")) {
            return;
        }
        Plot plot = pp.getCurrentPlot();
        if ((plot != null) && ((plot.isDenied(pp.getUUID())) || ((!plot.isAdded(pp.getUUID()))
            && (HIDE_FLAG.isTrue(plot))))) {
            Location to = event.getTo();
            Location from = event.getFrom();
            if ((to.getWorld().equals(from.getWorld())) && (to.distanceSquared(from) < 8.0D)) {
                event.setTo(player.getLocation());
                event.setCancelled(true);
                player.setVelocity(player.getVelocity());
            }
        }
    }
}
