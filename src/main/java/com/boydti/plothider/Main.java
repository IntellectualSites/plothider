package com.boydti.plothider;

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.GlobalFlagContainer;
import com.plotsquared.core.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

    private static final int BSTATS_ID = 6412;

    public void onEnable() {
        new PacketHandler(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        GlobalFlagContainer.getInstance().addFlag(new HideFlag(false));
        new Metrics(this, BSTATS_ID);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlotPlayer pp = BukkitUtil.getPlayer(player);
        if (Permissions.hasPermission(pp, "plots.plothider.bypass")) {
            return;
        }
        Plot plot = pp.getCurrentPlot();
        if (plot != null && (plot.isDenied(pp.getUUID()) || (!plot.isAdded(pp.getUUID()) && plot
            .getFlag(HideFlag.class)))) {
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
