package com.boydti.plothider;

import com.plotsquared.core.configuration.StaticCaption;
import com.plotsquared.core.plot.flag.types.BooleanFlag;

public class HideFlag extends BooleanFlag<HideFlag> {

    public static final HideFlag HIDE_FLAG_TRUE = new HideFlag(true);
    public static final HideFlag HIDE_FLAG_FALSE = new HideFlag(false);

    protected HideFlag(boolean value) {
        super(value, new StaticCaption("hide", false));
    }

    @Override
    protected HideFlag flagOf(Boolean value) {
        return value ? HIDE_FLAG_TRUE : HIDE_FLAG_FALSE;
    }

}
