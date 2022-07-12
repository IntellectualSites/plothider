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

import com.plotsquared.plothider.storage.palette.PalettedContainer;
import com.plotsquared.plothider.storage.palette.PalettedContainerType;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;

public class FlexibleStorage {

    private PalettedContainer blocksPalletedContainer;
    private PalettedContainer biomesPalletedContainer;

    public FlexibleStorage() {
        this.blocksPalletedContainer = null;
        this.biomesPalletedContainer = null;
    }

    public FlexibleStorage(byte bitsPerEntry, List<Integer> states, int bytes) {
        this(bitsPerEntry, states, new long[(bytes + ((char) (64 / PalettedContainer.getRealUsedBits(bitsPerEntry))) - 1)
                / ((char) (64 / PalettedContainer.getRealUsedBits(bitsPerEntry)))]);
    }

    public FlexibleStorage(byte bitsPerEntry, List<Integer> states, long[] data) {
        this.blocksPalletedContainer = PalettedContainer.build(PalettedContainerType.BLOCKS, bitsPerEntry, states, data);
        this.biomesPalletedContainer = null;
    }

    public PalettedContainer getBlocksPalletedContainer() {
        return blocksPalletedContainer;
    }

    public PalettedContainer getBiomesPalletedContainer() {
        return biomesPalletedContainer;
    }

    public void setBlocksPalletedContainer(PalettedContainer blocksPalletedContainer) {
        this.blocksPalletedContainer = blocksPalletedContainer;
    }

    public void setBiomesPalletedContainer(PalettedContainer biomesPalletedContainer) {
        this.biomesPalletedContainer = biomesPalletedContainer;
    }

    public void write(ByteArrayOutputStream out) {
        this.blocksPalletedContainer.write(out);

        if (biomesPalletedContainer != null)
            this.biomesPalletedContainer.write(out);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FlexibleStorage that))
            return false;
        return blocksPalletedContainer.equals(that.blocksPalletedContainer)
                && biomesPalletedContainer.equals(that.biomesPalletedContainer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocksPalletedContainer, biomesPalletedContainer);
    }
}
