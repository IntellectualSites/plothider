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
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.util.task.TaskTime;
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
            // Waits a bit because the flag is added after the event call.
            TaskManager.runTaskLater(() -> refreshPlot(plot), TaskTime.ticks(1));
        }
    }

    @Subscribe
    public void onPlotFlagRemoveEvent(PlotFlagRemoveEvent event) {
        Plot plot = event.getPlot();
        if (event.getFlag() instanceof HideFlag) {
            // Waits a bit because the flag is removed after the event call.
            TaskManager.runTaskLater(() -> refreshPlot(plot), TaskTime.ticks(1));
        }
    }

    private void refreshPlot(Plot plot) {
        int regionBottomCornerX = plot.getBottomAbs().getX();
        int regionBottomCornerZ = plot.getBottomAbs().getZ();
        int regionTopCornerX = plot.getTopAbs().getX();
        int regionTopCornerZ = plot.getTopAbs().getZ();

        for (Plot connectedPlot : plot.getConnectedPlots()) {
            Location bottomCorner = connectedPlot.getBottomAbs();
            Location topCorner = connectedPlot.getTopAbs();

            if (bottomCorner.getX() < regionBottomCornerX) {
                regionBottomCornerX = bottomCorner.getX();
            }
            if (bottomCorner.getZ() < regionBottomCornerZ) {
                regionBottomCornerZ = bottomCorner.getZ();
            }
            if (topCorner.getX() > regionTopCornerX) {
                regionTopCornerX = topCorner.getX();
            }
            if (topCorner.getZ() > regionTopCornerZ) {
                regionTopCornerZ = topCorner.getZ();
            }
        }

        World world = (World) plot.getBottomAbs().getWorld().getPlatformWorld();
        int fromChunkX = regionBottomCornerX >> 4;
        int fromChunkZ = regionBottomCornerZ >> 4;
        int toChunkX = regionTopCornerX >> 4;
        int toChunkZ = regionTopCornerZ >> 4;

        for (int x = fromChunkX; x <= toChunkX; x++) {
            for (int z = fromChunkZ; z <= toChunkZ; z++) {
                // Triggers a MapChunk packet, later handled by PlotHider's packet listener.
                world.refreshChunk(x, z);
            }
        }
    }

}
