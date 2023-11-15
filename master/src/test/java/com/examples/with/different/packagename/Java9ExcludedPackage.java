/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 * <p>
 * This file is part of EvoSuite.
 * <p>
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 * <p>
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package com.examples.with.different.packagename;

import sun.java2d.SunGraphics2D;

public class Java9ExcludedPackage {
    private SunGraphics2D sunGraphics2D;

    Java9ExcludedPackage() {

    }

    Java9ExcludedPackage(SunGraphics2D sunGraphics2D) {
        this.sunGraphics2D = sunGraphics2D;
    }

    public void drawLine(SunGraphics2D sunGraphics2D) {
        int transx = sunGraphics2D.transX;
        int transy = sunGraphics2D.transY;
        System.out.print("transx : " + transx + " , transy" + transy);
    }

    public int testMe(int x) {
        if (x == 5)
            return 1;
        else
            return 0;
    }
}
