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

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.CaptionMap;
import com.plotsquared.core.configuration.caption.load.CaptionLoader;
import com.plotsquared.core.configuration.caption.load.DefaultCaptionProvider;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.GlobalFlagContainer;
import com.plotsquared.core.util.Permissions;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class PlotHiderPlugin extends JavaPlugin implements Listener {

    public static final String PLOT_HIDER_NAMESPACE = "plothider";
    private static final int BSTATS_ID = 6412;

    @Override
    public void onEnable() {
        new PacketHandler(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        GlobalFlagContainer.getInstance().addFlag(HideFlag.HIDE_FLAG_FALSE);
        try {
            loadCaptions();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to load captions", e);
        }
        new Metrics(this, BSTATS_ID);
    }

    private void loadCaptions() throws IOException {
        Path msgFilePath = getDataFolder().toPath().resolve("lang").resolve("messages_en.json");
        if (!Files.exists(msgFilePath)) {
            this.saveResource("lang/messages_en.json", false);
        }
        CaptionLoader captionLoader = CaptionLoader.of(
                Locale.ENGLISH,
                CaptionLoader.patternExtractor(Pattern.compile("messages_(.*)\\.json")),
                DefaultCaptionProvider.forClassLoaderFormatString(
                        this.getClass().getClassLoader(),
                        "lang/messages_%s.json"
                ),
                PLOT_HIDER_NAMESPACE
        );
        CaptionMap captionMap;
        if (Settings.Enabled_Components.PER_USER_LOCALE) {
            captionMap = captionLoader.loadAll(getDataFolder().toPath().resolve("lang"));
        } else {
            String fileName = "messages_" + Settings.Enabled_Components.DEFAULT_LOCALE + ".json";
            captionMap = captionLoader.loadSingle(getDataFolder().toPath().resolve("lang").resolve(fileName));
        }
        PlotSquared.get().registerCaptionMap(PLOT_HIDER_NAMESPACE, captionMap);
        getLogger().info("Loaded caption map for namespace '" + PLOT_HIDER_NAMESPACE + "': "
                + captionMap.getClass().getCanonicalName());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlotPlayer<?> pp = BukkitUtil.adapt(player);
        if (Permissions.hasPermission(pp, "plots.plothider.bypass")) {
            return;
        }
        Plot plot = pp.getCurrentPlot();
        if (plot != null && (plot.isDenied(pp.getUUID()) || (!plot.isAdded(pp.getUUID()) && plot
                .getFlag(HideFlag.class)))) {
            Location to = event.getTo();
            Location from = event.getFrom();
            if (to.getWorld() == from.getWorld() && to.distanceSquared(from) < 8.0D) {
                event.setTo(player.getLocation());
                event.setCancelled(true);
                player.setVelocity(player.getVelocity());
            }
        }
    }
}
