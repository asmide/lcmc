/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
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

package lcmc.model;

import lcmc.gui.HostBrowser;
import lcmc.gui.TerminalPanel;
import lcmc.model.drbd.DrbdHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Provider;

@Component
public class HostFactory {
    @Autowired
    private Provider<HostBrowser> hostBrowserProvider;
    @Autowired
    private Provider<TerminalPanel> terminalPanelProvider;

    public Host createInstance() {
        final TerminalPanel terminalPanel = terminalPanelProvider.get();
        final Host host = new Host(new DrbdHost(), terminalPanel);
        host.setBrowser(hostBrowserProvider.get());
        return host;
    }

    public Host createInstance(final String ipAddress) {
        final Host instance = createInstance();
        instance.setIpAddress(ipAddress);
        return instance;
    }
}
