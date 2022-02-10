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
package com.mcmoddev.mmdbot.logging;

import com.mcmoddev.mmdbot.core.bot.Bot;
import com.mcmoddev.mmdbot.core.bot.BotType;
import com.mcmoddev.mmdbot.core.bot.RegisterBotType;
import com.mcmoddev.mmdbot.logging.events.MessageEvents;
import com.mcmoddev.mmdbot.logging.util.ListenerAdapter;
import com.mcmoddev.mmdbot.logging.util.Utils;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class LoggingBot implements Bot {

    public static final Logger LOGGER = LoggerFactory.getLogger("TheListener");

    @RegisterBotType(name = "logging")
    public static final BotType<LoggingBot> BOT_TYPE = new BotType<>() {
        @Override
        public LoggingBot createBot(final Path runPath) {
            return new LoggingBot(runPath);
        }

        @Override
        public Logger getLogger() {
            return LOGGER;
        }
    };

    private static LoggingBot instance;

    private DiscordClient client;
    private final Path runPath;

    public LoggingBot(final Path runPath) {
        this.runPath = runPath;
    }

    @Override
    public void start() {
        instance = this;

        /*final var token = ""; // TODO config, yes

        client = DiscordClient.create(token);

        final var gateway = client.gateway().setEnabledIntents(IntentSet.of(Intent.values())).login().block();

        Utils.subscribe(gateway, new ListenerAdapter() {
            @Override
            public void onReady(final ReadyEvent event) {
                LOGGER.warn("I am ready to work! Logged in as {}",
                    event.getSelf().getTag());
            }
        }, new MessageEvents());*/

        // TODO a proper thingy
        new Thread(() -> {
            while (true) {
                // Just need to do something, so the program doesn't stop
                int i = 0;
                i = i + 1;
                i = i - 1;
            }
        }).start();
    }

    @Override
    public BotType<?> getType() {
        return BOT_TYPE;
    }

    public Path getRunPath() {
        return runPath;
    }

    public static LoggingBot getInstance() {
        return instance;
    }

    public static DiscordClient getClient() {
        return instance == null ? null : instance.client;
    }

}
