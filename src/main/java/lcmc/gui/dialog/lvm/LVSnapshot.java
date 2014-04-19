/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.gui.dialog.lvm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.data.StringValue;
import lcmc.data.VMSXML;
import lcmc.data.Value;
import lcmc.gui.Browser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.widget.TextfieldWithUnit;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import lcmc.utilities.WidgetListener;
/**
 * This class implements LVM snapshot dialog.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class LVSnapshot extends LV {
    /** LV Snapshot timeout. */
    private static final int SNAPSHOT_TIMEOUT = 5000;
    /** Description LV snapshot. */
    private static final String SNAPSHOT_DESCRIPTION =
                                    "Create a snapshot of the logical volume.";
    /** Block device info object. */
    private final BlockDevInfo blockDevInfo;
    private final MyButton snapshotButton = new MyButton("Create Snapshot");
    private Widget lvNameWi;
    private Widget sizeWi;
    private Widget maxSizeWi;
    /** Create new LVSnapshot object. */
    public LVSnapshot(final BlockDevInfo blockDevInfo) {
        super(null);
        this.blockDevInfo = blockDevInfo;
    }

    /** Returns the title of the dialog. */
    @Override
    protected String getDialogTitle() {
        return "LV Snapshot ";
    }

    /** Returns the description of the dialog. */
    @Override
    protected String getDescription() {
        return SNAPSHOT_DESCRIPTION;
    }

    /** Inits the dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        makeDefaultAndRequestFocusLater(sizeWi.getComponent());
    }

    /** Enables and disabled buttons. */
    protected void checkButtons() {
        Tools.invokeLater(new EnableSnapshotRunnable(true));
    }

    private void setComboBoxes() {
        final String maxBlockSize = getMaxBlockSize();
        sizeWi.setValue(VMSXML.convertKilobytes(Long.toString(
                                     Long.parseLong(maxBlockSize) / 2)));
        maxSizeWi.setValue(VMSXML.convertKilobytes(maxBlockSize));
    }

    /** Returns the input pane. */
    @Override
    protected JComponent getInputPane() {
        snapshotButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Browser.BUTTON_PANEL_BACKGROUND);

        final String volumeGroup =
                            blockDevInfo.getBlockDevice().getVolumeGroup();
        inputPane.add(new JLabel("Group"));
        inputPane.add(new JLabel(volumeGroup));
        inputPane.add(new JLabel());
        /* find next free logical volume name */
        String defaultName;
        final Set<String> volumeGroups =
               blockDevInfo.getHost().getLogicalVolumesFromVolumeGroup(
                                                              volumeGroup);
        int i = 0;
        while (true) {
            defaultName = "lvol" + i;
            if (volumeGroups == null
                || !volumeGroups.contains(defaultName)) {
                break;
            }
            i++;
        }
        lvNameWi = WidgetFactory.createInstance(
                                      Widget.Type.TEXTFIELD,
                                      new StringValue(defaultName),
                                      Widget.NO_ITEMS,
                                      Widget.NO_REGEXP,
                                      250,
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.OP,
                                                     !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);
        inputPane.add(new JLabel("LV Name"));
        inputPane.add(lvNameWi.getComponent());
        inputPane.add(new JLabel());
        lvNameWi.addListeners(new WidgetListener() {
                                  @Override
                                  public void check(final Value value) {
                                      checkButtons();
                                  }
                              });

        final String maxBlockSize = getMaxBlockSize();
        /* size */
        final String newBlockSize = Long.toString(
                                      Long.parseLong(maxBlockSize) / 2);
        final JLabel sizeLabel = new JLabel("New Size");

        sizeWi = new TextfieldWithUnit(
                       VMSXML.convertKilobytes(newBlockSize),
                       getUnits(),
                       Widget.NO_REGEXP,
                       250,
                       Widget.NO_ABBRV,
                       new AccessMode(Application.AccessType.OP,
                                      !AccessMode.ADVANCED),
                       Widget.NO_BUTTON);
        inputPane.add(sizeLabel);
        inputPane.add(sizeWi.getComponent());
        snapshotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Tools.invokeAndWait(new EnableSnapshotRunnable(false));
                        disableComponents();
                        getProgressBar().start(SNAPSHOT_TIMEOUT);
                        final boolean ret = lvSnapshot(
                                                    lvNameWi.getStringValue(),
                                                    sizeWi.getStringValue());
                        final Host host = blockDevInfo.getHost();
                        host.getBrowser().getClusterBrowser().updateHWInfo(
                                                             host,
                                                             Host.UPDATE_LVM);
                        setComboBoxes();
                        if (ret) {
                            progressBarDone();
                            disposeDialog();
                        } else {
                            progressBarDoneError();
                        }
                        enableComponents();
                    }
                });
                thread.start();
            }
        });

        inputPane.add(snapshotButton);
        /* max size */
        final JLabel maxSizeLabel = new JLabel("Max Size");
        maxSizeLabel.setEnabled(false);
        maxSizeWi = new TextfieldWithUnit(
                        VMSXML.convertKilobytes(maxBlockSize),
                        getUnits(),
                        Widget.NO_REGEXP,
                        250,
                        Widget.NO_ABBRV,
                        new AccessMode(Application.AccessType.OP,
                                       !AccessMode.ADVANCED),
                        Widget.NO_BUTTON);
        maxSizeWi.setEnabled(false);
        inputPane.add(maxSizeLabel);
        inputPane.add(maxSizeWi.getComponent());
        inputPane.add(new JLabel());
        sizeWi.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Value value) {
                                    checkButtons();
                                }
                            });

        SpringUtilities.makeCompactGrid(inputPane, 4, 3,  /* rows, cols */
                                                   1, 1,  /* initX, initY */
                                                   1, 1); /* xPad, yPad */

        pane.add(inputPane);
        pane.add(getProgressBarPane(null));
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  /* rows, cols */
                                              0, 0,  /* initX, initY */
                                              0, 0); /* xPad, yPad */
        checkButtons();
        return pane;
    }

    /** LV Snapshot. */
    private boolean lvSnapshot(final String lvName, final String size) {
        final String volumeGroup =
                            blockDevInfo.getBlockDevice().getVolumeGroup();
        final boolean ret = blockDevInfo.lvSnapshot(lvName,
                                                    size,
                                                    Application.RunMode.LIVE);
        if (ret) {
            answerPaneSetText("Logical volume "
                              + lvName
                              + " was successfully created on "
                              + volumeGroup + '.');
        } else {
            answerPaneSetTextError("Creating of logical volume "
                                   + lvName
                                   + " failed.");
        }
        return ret;
    }

    /** Returns maximum block size available in the group. */
    private String getMaxBlockSize() {
        final String volumeGroup =
                            blockDevInfo.getBlockDevice().getVolumeGroup();
        final long free =
           blockDevInfo.getHost().getFreeInVolumeGroup(volumeGroup) / 1024;
        return Long.toString(free);
    }

    private class EnableSnapshotRunnable implements Runnable {
        private final boolean enable;
        EnableSnapshotRunnable(final boolean enable) {
            super();
            this.enable = enable;
        }

        @Override
        public void run() {
            boolean e = enable;
            if (enable) {
                final long size = VMSXML.convertToKilobytes(
                                                  sizeWi.getValue());
                final long maxSize = VMSXML.convertToKilobytes(
                                               maxSizeWi.getValue());
                if (size > maxSize) {
                    e = false;
                } else if (size <= 0) {
                    e = false;
                } else {
                    final Set<String> lvs =
                        blockDevInfo.getHost()
                                    .getLogicalVolumesFromVolumeGroup(
                           blockDevInfo.getBlockDevice().getVolumeGroup());
                    if (lvs != null
                        && lvs.contains(lvNameWi.getStringValue())) {
                        e = false;
                    }
                }
            }
            snapshotButton.setEnabled(e);
        }
    }
}
