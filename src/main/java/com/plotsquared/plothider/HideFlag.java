package com.plotsquared.plothider;

import com.plotsquared.core.configuration.caption.StaticCaption;
import com.plotsquared.core.plot.flag.types.BooleanFlag;
import org.jetbrains.annotations.NotNull;

public class HideFlag extends BooleanFlag<HideFlag> {

    public static final HideFlag HIDE_FLAG_TRUE = new HideFlag(true);
    public static final HideFlag HIDE_FLAG_FALSE = new HideFlag(false);

    private HideFlag(boolean value) {
        super(value, StaticCaption.of("hide"));
    }

    @Override
    protected HideFlag flagOf(@NotNull Boolean value) {
        return value ? HIDE_FLAG_TRUE : HIDE_FLAG_FALSE;
    }

}
