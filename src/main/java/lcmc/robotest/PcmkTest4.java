/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.robotest;

import static lcmc.robotest.RoboTest.CONFIRM_REMOVE;
import static lcmc.robotest.RoboTest.aborted;
import static lcmc.robotest.RoboTest.addConstraint;
import static lcmc.robotest.RoboTest.checkTest;
import static lcmc.robotest.RoboTest.chooseDummy;
import static lcmc.robotest.RoboTest.disableStonith;
import static lcmc.robotest.RoboTest.leftClick;
import static lcmc.robotest.RoboTest.moveTo;
import static lcmc.robotest.RoboTest.moveToMenu;
import static lcmc.robotest.RoboTest.removeEverything;
import static lcmc.robotest.RoboTest.removePlaceHolder;
import static lcmc.robotest.RoboTest.rightClick;
import static lcmc.robotest.RoboTest.slowFactor;
import static lcmc.robotest.RoboTest.stopEverything;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class PcmkTest4 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTest4.class);

    static void start() {
        slowFactor = 0.6f;
        aborted = false;

        disableStonith();
        checkTest("test4", 1);
        /* create 6 dummies */
        final int dummy1X = 235;
        final int dummy1Y = 207;
        chooseDummy(dummy1X, dummy1Y, false, true);
        final int dummy2X = 545;
        final int dummy2Y = 207;
        chooseDummy(dummy2X, dummy2Y, false, true);
        final int dummy3X = 235;
        final int dummy3Y = 346;
        chooseDummy(dummy3X, dummy3Y, false, true);
        final int dummy4X = 545;
        final int dummy4Y = 346;
        chooseDummy(dummy4X, dummy4Y, false, true);
        final int dummy5X = 235;
        final int dummy5Y = 505;
        chooseDummy(dummy5X, dummy5Y, false, true);
        final int dummy6X = 545;
        final int dummy6Y = 505;
        chooseDummy(dummy6X, dummy6Y, false, true);
        checkTest("test4", 2);

        /* 2 placeholders */
        final int count = 1;
        final int ph1X = 445;
        final int ph1Y = 266;
        final int ph2X = 445;
        final int ph2Y = 425;
        for (int i = 0; i < count; i++) {
            moveTo(ph1X, ph1Y);
            rightClick();
            moveTo("Placeholder (AND)");
            leftClick();

            moveTo(ph2X, ph2Y);
            rightClick();
            moveTo("Placeholder (AND)");
            leftClick();
            checkTest("test4", 2);

            /* constraints */
            /* menu dummy 5 with ph2 */
            moveToMenu("Dummy (5)");
            addConstraint(7);
            /* menu dummy 6 with ph2 */
            moveToMenu("Dummy (6)");
            addConstraint(7);

            /* with dummy 3 */
            moveTo(ph2X, ph2Y);
            addConstraint(3);
            /* with dummy 4 */
            moveTo(ph2X, ph2Y);
            addConstraint(3);

            /* with ph1 */
            moveTo(dummy3X, dummy3Y);
            addConstraint(4);
            /* with ph1 */
            moveTo(dummy4X, dummy4Y);
            addConstraint(4);

            moveTo(ph1X, ph1Y);
            addConstraint(1); /* with dummy 1 */
            moveTo(ph1X, ph1Y);
            addConstraint(1); /* with dummy 2 */
            checkTest("test4", 2);

        }
        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick();

        checkTest("test4", 3);
        stopEverything();
        checkTest("test4", 4);
        removeEverything();
        removePlaceHolder(ph1X, ph1Y, !CONFIRM_REMOVE);
        removePlaceHolder(ph2X, ph2Y, !CONFIRM_REMOVE);
    }

    /** Private constructor, cannot be instantiated. */
    private PcmkTest4() {
        /* Cannot be instantiated. */
    }
}
