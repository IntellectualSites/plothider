/*
 *              _____  _       _   _    _ _     _
 *              |  __ \| |     | | | |  | (_)   | |
 *              | |__) | | ___ | |_| |__| |_  __| | ___ _ __
 *              |  ___/| |/ _ \| __|  __  | |/ _` |/ _ \ '__|
 *              | |    | | (_) | |_| |  | | | (_| |  __/ |
 *              |_|    |_|\___/ \__|_|  |_|_|\__,_|\___|_|
 *               PlotHider PlotSquared addon for Minecraft
 *                  Copyright (C) 2021 IntellectualSites
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

import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.plot.flag.types.BooleanFlag;
import org.jetbrains.annotations.NotNull;

public class HideFlag extends BooleanFlag<HideFlag> {

    public static final HideFlag HIDE_FLAG_TRUE = new HideFlag(true);
    public static final HideFlag HIDE_FLAG_FALSE = new HideFlag(false);

    private HideFlag(boolean value) {
        super(value, TranslatableCaption.of(PlotHiderPlugin.PLOT_HIDER_NAMESPACE, "flags.flag_description_hide"));
    }

    @Override
    protected HideFlag flagOf(@NotNull Boolean value) {
        return value ? HIDE_FLAG_TRUE : HIDE_FLAG_FALSE;
    }

}
