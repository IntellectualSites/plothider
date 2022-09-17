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

import com.google.common.eventbus.Subscribe;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.events.*;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.Plot;
import com.sk89q.worldedit.math.BlockVector2;
import org.bukkit.World;
import org.bukkit.event.Listener;

public class PlotSquaredListener implements Listener {

    public static final PlotAPI PLOT_API;

    static {
        PLOT_API = new PlotAPI();
    }

    public PlotSquaredListener() {
        PLOT_API.registerListener(this);
    }

    @Subscribe
    public void onPlayerPlotDeniedEvent(PlayerPlotDeniedEvent event) {
        Plot plot = event.getPlot();
        if (plot.getFlag(HideFlag.class)) {
            refreshPlot(event.getPlot());
        }
    }

    @Subscribe
    public void onPlayerPlotTrusted(PlayerPlotTrustedEvent event) {
        Plot plot = event.getPlot();
        if (plot.getFlag(HideFlag.class)) {
            refreshPlot(plot);
        }
    }

    @Subscribe
    public void onPlayerPlotHelperEvent(PlayerPlotHelperEvent event) {
        Plot plot = event.getPlot();
        if (plot.getFlag(HideFlag.class)) {
            refreshPlot(plot);
        }
    }

    @Subscribe
    public void onPlotFlagAddEvent(PlotFlagAddEvent event) {
        Plot plot = event.getPlot();
        if (event.getFlag() instanceof HideFlag) {
            refreshPlot(plot);
        }
    }

    @Subscribe
    public void onPlotFlagRemoveEvent(PlotFlagRemoveEvent event) {
        Plot plot = event.getPlot();
        if (event.getFlag() instanceof HideFlag) {
            refreshPlot(plot);
        }
    }

    private void refreshPlot(Plot plot) {
        for (Plot connectedPlot : plot.getConnectedPlots()) {
            Location bottomCorner = connectedPlot.getBottomAbs();
            Location topCorner = connectedPlot.getTopAbs();
            World world = (World) bottomCorner.getWorld().getPlatformWorld();

            BlockVector2 fromChunk = bottomCorner.getChunkLocation();
            BlockVector2 toChunk = topCorner.getChunkLocation();

            for (int x = fromChunk.getX(); x <= toChunk.getX(); x++) {
                for (int z = fromChunk.getZ(); z <= toChunk.getZ(); z++) {
                    // Triggers a MapChunk packet, later handled by PlotHider's packet listener.
                    world.refreshChunk(x, z);
                }
            }
        }
    }

}
