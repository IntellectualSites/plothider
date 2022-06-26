/*
 *              _____  _       _   _    _ _     _
 *              |  __ \| |     | | | |  | (_)   | |
 *              | |__) | | ___ | |_| |__| |_  __| | ___ _ __
 *              |  ___/| |/ _ \| __|  __  | |/ _` |/ _ \ '__|
 *              | |    | | (_) | |_| |  | | | (_| |  __/ |
 *              |_|    |_|\___/ \__|_|  |_|_|\__,_|\___|_|
 *               PlotHider PlotSquared addon for Minecraft
 *              Copyright (C) 2016 - 2022 IntellectualSites
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
package com.plotsquared.plothider.storage;

import com.plotsquared.core.PlotSquared;
import com.plotsquared.plothider.storage.palette.APalettedContainer;
import com.plotsquared.plothider.storage.palette.PalettedContainerType;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class FlexibleStorage {

    private APalettedContainer blocksPalletedContainer;
    private APalettedContainer biomesPalletedContainer;

    public FlexibleStorage() {
        this.blocksPalletedContainer = null;
        this.biomesPalletedContainer = null;
    }

    public FlexibleStorage(byte bitsPerEntry, List<Integer> states, int bytes) {
        this(bitsPerEntry, states, new long[PlotSquared.platform().serverVersion()[1] >= 16
                ? (bytes + ((char) (64 / APalettedContainer.getRealUsedBits(bitsPerEntry))) - 1)
                / ((char) (64 / APalettedContainer.getRealUsedBits(bitsPerEntry)))
                : bytes * APalettedContainer.getRealUsedBits(bitsPerEntry) / 64]);
    }

    public FlexibleStorage(byte bitsPerEntry, List<Integer> states, long[] data) {
        this.blocksPalletedContainer = APalettedContainer.build(PalettedContainerType.BLOCKS, bitsPerEntry, states, data);
        this.biomesPalletedContainer = null;
    }

    public APalettedContainer getBlocksPalletedContainer() {
        return blocksPalletedContainer;
    }

    public APalettedContainer getBiomesPalletedContainer() {
        return biomesPalletedContainer;
    }

    public void setBlocksPalletedContainer(APalettedContainer blocksPalletedContainer) {
        this.blocksPalletedContainer = blocksPalletedContainer;
    }

    public void setBiomesPalletedContainer(APalettedContainer biomesPalletedContainer) {
        this.biomesPalletedContainer = biomesPalletedContainer;
    }

    public void write(ByteArrayOutputStream out) {
        this.blocksPalletedContainer.write(out);

        if (biomesPalletedContainer != null)
            this.biomesPalletedContainer.write(out);
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) || (((o instanceof FlexibleStorage))
                && (this.blocksPalletedContainer.equals(((FlexibleStorage) o).blocksPalletedContainer))
                && (this.biomesPalletedContainer.equals(((FlexibleStorage) o).biomesPalletedContainer)));
    }
}
