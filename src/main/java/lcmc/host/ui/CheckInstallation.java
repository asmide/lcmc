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

package lcmc.host.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.cluster.ui.widget.Widget;
import lcmc.host.domain.Host;
import lcmc.drbd.domain.DrbdInstallation;
import lcmc.common.domain.ExecCallback;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.domain.util.Tools;
import lcmc.cluster.service.ssh.ExecCommandConfig;

/**
 * An implementation of a dialog where
 * drbd/heartbeat/pacemaker/openais/corosync etc. installation is checked.
 */
@Named
final class CheckInstallation extends DialogHost {
    private static final Logger LOG = LoggerFactory.getLogger(CheckInstallation.class);

    private static final ImageIcon CHECKING_ICON = Tools.createImageIcon(
                                                      Tools.getDefault("Dialog.Host.CheckInstallation.CheckingIcon"));
    private static final ImageIcon NOT_INSTALLED_ICON =
                            Tools.createImageIcon(Tools.getDefault("Dialog.Host.CheckInstallation.NotInstalledIcon"));
    private static final ImageIcon ALREADY_INSTALLED_ICON =
                               Tools.createImageIcon(Tools.getDefault("Dialog.Host.CheckInstallation.InstalledIcon"));
    private static final ImageIcon UPGRADE_AVAILABLE_ICON =
                               Tools.createImageIcon(Tools.getDefault("Dialog.Host.CheckInstallation.UpgrAvailIcon"));

    private static final String PACEMAKER_PREFIX = "PmInst";
    private static final String HEARTBEAT_PACEMAKER_PREFIX = "HbPmInst";
    private static final String DRBD_PREFIX = "DrbdInst";

    private static final String PACEMAKER_AUTOTEST_OPTION = "pminst";
    private static final String HEARTBEAT_PACEMAKER_AUTOTEST_OPTION = "hbinst";
    private static final String DRBD_AUTOTEST_OPTION = "drbdinst";

    private DialogHost nextDialogObject = null;

    private final JLabel checkingDrbdLabel =
                                  new JLabel(": " + Tools.getString("Dialog.Host.CheckInstallation.CheckingDrbd"));
    private final JLabel checkingPacemakerLabel =
                                  new JLabel(": " + Tools.getString("Dialog.Host.CheckInstallation.CheckingPm"));
    private final JLabel checkingHeartbeatPacemakerLabel =
                                  new JLabel(": " + Tools.getString("Dialog.Host.CheckInstallation.CheckingHbPm"));
    @Inject
    private WidgetFactory widgetFactory;
    private MyButton installDrbdButton;
    private MyButton installPacemakerButton;
    private MyButton installHeartbeatPacemakerButton;

    private Widget pacemakerInstMethodWidget;
    private Widget heartbeatPacemakerInstMethodWidget;
    private Widget drbdInstMethodWidget;

    private final JLabel checkingDrbdIcon = new JLabel(CHECKING_ICON);
    private final JLabel checkingPacemakerIcon = new JLabel(CHECKING_ICON);
    private final JLabel heartbeatPacemakerIcon = new JLabel(CHECKING_ICON);
    private boolean drbdInstallationOk = false;
    private boolean pacemakerInstallationOk = false;
    private boolean heartbeatPacemakerInstallationOk = false;
    private final JLabel heartbeatPacemakerLabel = new JLabel("Pcmk/Heartbeat");

    private final JLabel pacemakerLabel = new JLabel("Pcmk/Corosync");
    @Inject
    private HostFinish hostFinishDialog;
    @Inject
    private DrbdLinbitAvailPackages drbdLinbitAvailPackagesDialog;
    @Inject
    private DrbdAvailSourceFiles drbdAvailSourceFilesDialog;
    @Inject
    private DrbdCommandInst drbdCommandInstDialog;
    @Inject
    private LinbitLogin linbitLoginDialog;
    @Inject
    private HeartbeatInst heartbeatInstDialog;
    @Inject
    private PacemakerInst pacemakerInstDialog;
    @Inject
    private Application application;

    @Override
    public void init(final WizardDialog previousDialog, final Host host, final DrbdInstallation drbdInstallation) {
        super.init(previousDialog, host, drbdInstallation);
        installDrbdButton =
                widgetFactory.createButton(Tools.getString("Dialog.Host.CheckInstallation.DrbdInstallButton"));
        installPacemakerButton =
                widgetFactory.createButton(Tools.getString("Dialog.Host.CheckInstallation.PmInstallButton"));
        installHeartbeatPacemakerButton =
                widgetFactory.createButton(Tools.getString("Dialog.Host.CheckInstallation.HbPmInstallButton"));
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
    }

    @Override
    protected void initDialogAfterVisible() {
        drbdInstallationOk = false;
        pacemakerInstallationOk = false;
        heartbeatPacemakerInstallationOk = false;

        hostFinishDialog.init(this, getHost(), getDrbdInstallation());
        nextDialogObject = hostFinishDialog;

        final CheckInstallation thisClass = this;
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                installDrbdButton.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
                installDrbdButton.setEnabled(false);
                drbdInstMethodWidget.setEnabled(false);
                installPacemakerButton.setEnabled(false);
                pacemakerInstMethodWidget.setEnabled(false);
                installHeartbeatPacemakerButton.setEnabled(false);
                heartbeatPacemakerInstMethodWidget.setEnabled(false);
            }
        });
        installDrbdButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (drbdInstallationOk) {
                        getDrbdInstallation().setDrbdWillBeUpgraded(true);
                    }
                    final InstallMethods im = (InstallMethods) drbdInstMethodWidget.getValue();
                    getDrbdInstallation().setDrbdInstallMethodIndex(im.getIndex());
                    final String button = e.getActionCommand();
                    if (!drbdInstallationOk || button.equals(Tools.getString(
                                                    "Dialog.Host.CheckInstallation.DrbdCheckForUpgradeButton"))) {
                        if (im.isLinbitMethod()) {
                            nextDialogObject = drbdLinbitAvailPackagesDialog;
                        } else if (im.isSourceMethod()) {
                            nextDialogObject = drbdAvailSourceFilesDialog;
                        } else {
                            // TODO: this only when there is no drbd installed
                            nextDialogObject = drbdCommandInstDialog;
                            getDrbdInstallation().setDrbdInstallMethodIndex(im.getIndex());
                        }
                        nextDialogObject.init(thisClass, getHost(), getDrbdInstallation());
                        application.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                buttonClass(nextButton()).pressButton();
                            }
                        });
                    } else {
                        nextDialogObject = linbitLoginDialog;
                        nextDialogObject.init(thisClass, getHost(), getDrbdInstallation());
                        application.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                buttonClass(nextButton()).pressButton();
                            }
                        });
                    }
                }
            }
        );

        installHeartbeatPacemakerButton.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
        installHeartbeatPacemakerButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    nextDialogObject = heartbeatInstDialog;
                    nextDialogObject.init(thisClass, getHost(), getDrbdInstallation());
                    final InstallMethods im = (InstallMethods) heartbeatPacemakerInstMethodWidget.getValue();
                    getHost().setHeartbeatPacemakerInstallMethodIndex(im.getIndex());
                    application.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            buttonClass(nextButton()).pressButton();
                        }
                    });
                }
            }
        );

        installPacemakerButton.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
        installPacemakerButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    nextDialogObject = pacemakerInstDialog;
                    nextDialogObject.init(thisClass, getHost(), getDrbdInstallation());
                    final InstallMethods im = (InstallMethods) pacemakerInstMethodWidget.getValue();
                    getHost().setPacemakerInstallMethodIndex(im.getIndex());
                    application.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            buttonClass(nextButton()).pressButton();
                        }
                    });
                }
            }
        );

        getHost().execCommand(new ExecCommandConfig().commandString("DrbdCheck.version")
                                                     .progressBar(getProgressBar())
                                                     .execCallback(new ExecCallback() {
                                                         @Override
                                                         public void done(final String answer) {
                                                             checkDrbd(answer);
                                                         }
                                                         @Override
                                                         public void doneError(final String answer,
                                                                               final int errorCode) {
                                                             checkDrbd(""); // not installed
                                                         }
                                                     })
                                                     .silentCommand()
                                                     .silentOutput());
    }

    /**
     * Checks whether drbd is installed and starts heartbeat/pacemaker check.
     */
    void checkDrbd(final String ans) {
        if (ans != null && ans.isEmpty() || "\n".equals(ans)) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    checkingDrbdLabel.setText(": " + Tools.getString("Dialog.Host.CheckInstallation.DrbdNotInstalled"));
                    checkingDrbdIcon.setIcon(NOT_INSTALLED_ICON);
                    final String toolTip = getInstToolTip(DRBD_PREFIX, "1");
                    drbdInstMethodWidget.setToolTipText(toolTip);
                    installDrbdButton.setToolTipText(toolTip);
                    installDrbdButton.setEnabled(true);
                    drbdInstMethodWidget.setEnabled(true);
                }
            });
        } else {
            drbdInstallationOk = true;
            application.invokeLater(new Runnable() {
                @Override
                @SuppressWarnings("DeadBranch")
                public void run() {
                    checkingDrbdLabel.setText(": " + ans.trim());
                    if (getDrbdInstallation().isDrbdUpgradeAvailable(ans.trim())) {
                        checkingDrbdIcon.setIcon(UPGRADE_AVAILABLE_ICON);
                        installDrbdButton.setText(Tools.getString("Dialog.Host.CheckInstallation.DrbdUpgradeButton"));
                        installDrbdButton.setEnabled(true);
                        drbdInstMethodWidget.setEnabled(true);
                    } else {
                        installDrbdButton.setText(
                                          Tools.getString("Dialog.Host.CheckInstallation.DrbdCheckForUpgradeButton"));
                        if (false) {
                            // TODO: disabled
                            installDrbdButton.setEnabled(true);
                            drbdInstMethodWidget.setEnabled(true);
                        }
                        checkingDrbdIcon.setIcon(ALREADY_INSTALLED_ICON);
                    }
                }
            });
        }
        getHost().execCommand(new ExecCommandConfig().commandString("HbCheck.version")
                                                     .progressBar(getProgressBar())
                                                     .execCallback(new ExecCallback() {
                                                         @Override
                                                         public void done(final String answer) {
                                                             checkAisHbPm(answer);
                                                         }

                                                         @Override
                                                         public void doneError(final String answer, final int errorCode) {
                                                             done("");
                                                         }
                                                     })
                                                     .silentCommand()
                                                     .silentOutput());
    }

    void checkAisHbPm(final String ans) {
        getHost().setPacemakerVersion(null);
        getHost().setOpenaisVersion(null);
        getHost().setHeartbeatVersion(null);
        getHost().setCorosyncVersion(null);
        if (ans != null && !ans.isEmpty() && !"\n".equals(ans)) {
            for (final String line : ans.split("\n")) {
                getHost().parseInstallationInfo(line);
            }
        }
        final String aisVersion = getHost().getOpenaisVersion();
        final String corosyncVersion = getHost().getCorosyncVersion();
        String hbVersion = getHost().getHeartbeatVersion();
        if (hbVersion == null
            && (getHost().getPacemakerVersion() == null || (corosyncVersion == null && aisVersion == null))) {
            final InstallMethods hbim = (InstallMethods) heartbeatPacemakerInstMethodWidget.getValue();
            if (hbim != null) {
                installHeartbeatPacemakerButton.setEnabled(true);
                heartbeatPacemakerInstMethodWidget.setEnabled(true);
                final String toolTip = getInstToolTip(HEARTBEAT_PACEMAKER_PREFIX, hbim.getIndex());
                heartbeatPacemakerInstMethodWidget.setToolTipText(toolTip);
                installHeartbeatPacemakerButton.setToolTipText(toolTip);
            }
            final InstallMethods pmim = (InstallMethods) pacemakerInstMethodWidget.getValue();
            if (pmim != null) {
                installPacemakerButton.setEnabled(true);
                pacemakerInstMethodWidget.setEnabled(true);
                final String aisToolTip = getInstToolTip(PACEMAKER_PREFIX, pmim.getIndex());
                pacemakerInstMethodWidget.setToolTipText(aisToolTip);
                installPacemakerButton.setToolTipText(aisToolTip);
            }
        }
        if (hbVersion == null) {
            /* hb */
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    heartbeatPacemakerIcon.setIcon(NOT_INSTALLED_ICON);
                    checkingHeartbeatPacemakerLabel.setText(
                                            ": " + Tools.getString("Dialog.Host.CheckInstallation.HbPmNotInstalled"));
                }
            });
        } else {
            heartbeatPacemakerInstallationOk = true;
            final String text;
            if ("2.1.3".equals(hbVersion)
                && "sles10".equals(getHost().getDistributionVersion())) {
                /* sles10 heartbeat 2.1.3 looks like hb 2.1.4 */
                hbVersion = "2.1.4";
                text = "2.1.3 (2.1.4)";
            } else {
                text = hbVersion;
            }
            getHost().setHeartbeatVersion(hbVersion);
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (getHost().getPacemakerVersion() == null
                        || getHost().getHeartbeatVersion().equals(getHost().getPacemakerVersion())) {
                        heartbeatPacemakerLabel.setText("Heartbeat");
                        checkingHeartbeatPacemakerLabel.setText(": " + text);
                    } else {
                        checkingHeartbeatPacemakerLabel.setText(": " + getHost().getPacemakerVersion() + '/' + text);
                    }
                    heartbeatPacemakerIcon.setIcon(ALREADY_INSTALLED_ICON);
                }
            });
        }
        if (getHost().getPacemakerVersion() == null || (aisVersion == null && corosyncVersion == null)) {
            /* corosync */
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    checkingPacemakerIcon.setIcon(NOT_INSTALLED_ICON);
                    checkingPacemakerLabel.setText(
                                            ": " + Tools.getString("Dialog.Host.CheckInstallation.PmNotInstalled"));
                    pacemakerLabel.setText("Pcmk/Corosync");
                }
            });
        } else {
            pacemakerInstallationOk = true;
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    checkingPacemakerIcon.setIcon(ALREADY_INSTALLED_ICON);
                    String coroAisVersion = "no";
                    if (corosyncVersion != null) {
                        pacemakerLabel.setText("Pcmk/Corosync");
                        coroAisVersion = corosyncVersion;
                    } else if (aisVersion != null) {
                        pacemakerLabel.setText("Pcmk/AIS");
                        coroAisVersion = aisVersion;
                    }
                    pacemakerLabel.repaint();
                    checkingPacemakerLabel.setText(": " + getHost().getPacemakerVersion() + '/' + coroAisVersion);
                }
            });
        }

        final List<String> incorrect = new ArrayList<String>();
        if (drbdInstallationOk && (heartbeatPacemakerInstallationOk || pacemakerInstallationOk)) {
            progressBarDone();
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    answerPaneSetText(Tools.getString("Dialog.Host.CheckInstallation.AllOk"));
                }
            });
            if (application.getAutoOptionHost("drbdinst") != null
                || application.getAutoOptionHost("hbinst") != null
                || application.getAutoOptionHost("pminst") != null) {
                Tools.sleep(1000);
                application.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pressNextButton();
                    }
                });
            }
        } else {
            progressBarDoneError();
            LOG.debug2("checkAisHbPm: drbd: "
                       + drbdInstallationOk
                       + ", ais/pm: "
                       + pacemakerInstallationOk
                       + ", hb/pm: "
                       + heartbeatPacemakerInstallationOk);
            final String error = Tools.getString("Dialog.Host.CheckInstallation.SomeFailed");
            printErrorAndRetry(error);
            incorrect.add(error);
        }
        final List<String> changed = new ArrayList<String>();
        enableComponents();
        enableNextButtons(incorrect, changed);
        makeDefaultAndRequestFocus(buttonClass(nextButton()));
        if (!drbdInstallationOk && application.getAutoOptionHost("drbdinst") != null) {
            Tools.sleep(1000);
            installDrbdButton.pressButton();
        } else if (!heartbeatPacemakerInstallationOk && application.getAutoOptionHost("hbinst") != null) {
            Tools.sleep(1000);
            installHeartbeatPacemakerButton.pressButton();
        } else if (!pacemakerInstallationOk && application.getAutoOptionHost("pminst") != null) {
            Tools.sleep(1000);
            installPacemakerButton.pressButton();
        }
    }

    @Override
    public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.CheckInstallation.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.CheckInstallation.Description");
    }

    /**
     * Returns the pane, that checks the installation of different
     * components and provides buttons to update or upgrade.
     */
    private JPanel getInstallationPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pacemakerInstMethodWidget = getInstallationMethods(PACEMAKER_PREFIX,
                                                           application.isStagingPacemaker(),
                                                           application.getLastHbPmInstalledMethod(),
                                                           PACEMAKER_AUTOTEST_OPTION,
                                                           installPacemakerButton);

        heartbeatPacemakerInstMethodWidget = getInstallationMethods(HEARTBEAT_PACEMAKER_PREFIX,
                                                                    application.isStagingPacemaker(),
                                                                    application.getLastHbPmInstalledMethod(),
                                                                    HEARTBEAT_PACEMAKER_AUTOTEST_OPTION,
                                                                    installHeartbeatPacemakerButton);
        drbdInstMethodWidget = getInstallationMethods(DRBD_PREFIX,
                                                      application.isStagingDrbd(),
                                                      application.getLastDrbdInstalledMethod(),
                                                      DRBD_AUTOTEST_OPTION,
                                                      installDrbdButton);
        final String lastInstalled = application.getLastInstalledClusterStack();
        if (lastInstalled != null) {
            if (Application.HEARTBEAT_NAME.equals(lastInstalled)) {
                pacemakerLabel.setForeground(Color.LIGHT_GRAY);
                checkingPacemakerLabel.setForeground(Color.LIGHT_GRAY);
            } else if (Application.COROSYNC_NAME.equals(lastInstalled)) {
                heartbeatPacemakerLabel.setForeground(Color.LIGHT_GRAY);
                checkingHeartbeatPacemakerLabel.setForeground(Color.LIGHT_GRAY);
            }
        }
        pane.add(heartbeatPacemakerLabel);
        pane.add(checkingHeartbeatPacemakerLabel);
        pane.add(heartbeatPacemakerIcon);
        pane.add(heartbeatPacemakerInstMethodWidget.getComponent());
        pane.add(installHeartbeatPacemakerButton);
        pane.add(pacemakerLabel);
        pane.add(checkingPacemakerLabel);
        pane.add(checkingPacemakerIcon);
        pane.add(pacemakerInstMethodWidget.getComponent());
        pane.add(installPacemakerButton);
        pane.add(new JLabel("Drbd"));
        pane.add(checkingDrbdLabel);
        pane.add(checkingDrbdIcon);
        pane.add(drbdInstMethodWidget.getComponent());
        pane.add(installDrbdButton);

        SpringUtilities.makeCompactGrid(pane, 3, 5,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad
        return pane;
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getInstallationPane());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.CheckInstallation.Checking")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  //rows, cols
                                              0, 0,  //initX, initY
                                              0, 0); //xPad, yPad

        return pane;
    }

    @Override
    protected boolean skipButtonEnabled() {
        return true;
    }
}
