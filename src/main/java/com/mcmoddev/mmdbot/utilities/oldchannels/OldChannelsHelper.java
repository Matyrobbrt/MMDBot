/*
 * MMDBot - https://github.com/MinecraftModDevelopment/MMDBot
 * Copyright (C) 2016-2022 <MMD - MinecraftModDevelopment>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
package com.mcmoddev.mmdbot.utilities.oldchannels;

import net.dv8tion.jda.api.entities.TextChannel;

import java.util.HashMap;
import java.util.Map;

/**
 * The type Old channels helper.
 *
 * @author williambl The type Old channels helper.
 */
public class OldChannelsHelper {

    /**
     * The constant channelLastMessageMap.
     */
    private static final Map<Long, Long> CHANNEL_LAST_MESSAGE_MAP = new HashMap<>();

    /**
     * The constant ready.
     */
    private static boolean ready = false;

    /**
     * Gets last message time.
     *
     * @param channel the channel
     * @return the last message time
     */
    public static long getLastMessageTime(final TextChannel channel) {
        return CHANNEL_LAST_MESSAGE_MAP.getOrDefault(channel.getIdLong(), -1L);
    }

    /**
     * Clear.
     */
    public static void clear() {
        CHANNEL_LAST_MESSAGE_MAP.clear();
        setReady(false);
    }

    /**
     * Put.
     *
     * @param channel              the channel
     * @param timeSinceLastMessage the time since last message
     */
    public static void put(final TextChannel channel, final long timeSinceLastMessage) {
        CHANNEL_LAST_MESSAGE_MAP.put(channel.getIdLong(), timeSinceLastMessage);
    }

    /**
     * Is ready boolean.
     *
     * @return the boolean
     */
    public static boolean isReady() {
        return ready;
    }

    /**
     * Sets ready.
     *
     * @param ready the ready
     */
    public static void setReady(final boolean ready) {
        OldChannelsHelper.ready = ready;
    }
}
