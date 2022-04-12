/*
 * MMDBot - https://github.com/MinecraftModDevelopment/MMDBot
 * Copyright (C) 2016-2022 <MMD - MinecraftModDevelopment>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * Specifically version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 * https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 */
package com.mcmoddev.updatinglauncher.agent;

import com.mcmoddev.updatinglauncher.MemoryUsage;
import com.mcmoddev.updatinglauncher.ProcessConnector;
import com.mcmoddev.updatinglauncher.ThreadInfo;

import java.lang.management.ManagementFactory;
import java.rmi.RemoteException;

public class ProcessConnectorServer implements ProcessConnector {
    @Override
    public ThreadInfo[] getThreads() throws RemoteException {
        final var all = Thread.getAllStackTraces();
        return all.entrySet().stream().map(e -> ThreadInfo.fromThread(e.getKey(), e.getValue())).toArray(ThreadInfo[]::new);
    }

    @Override
    public double getCPULoad() throws RemoteException {
        return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getCpuLoad();
    }

    @Override
    public MemoryUsage getMemoryUsage() throws RemoteException {
        final var runtime = Runtime.getRuntime();
        return new MemoryUsage(runtime.totalMemory(), runtime.freeMemory());
    }
}
