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
package com.mcmoddev.mmdbot.modules.commands.bot.info;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.mcmoddev.mmdbot.MMDBot;
import com.mcmoddev.mmdbot.core.References;
import com.mcmoddev.mmdbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Shows information about the bot.
 * Includes:
 * - Short description
 * - Build information
 * - Issue tracker link
 * - List of maintainers
 * <p>
 * Takes no parameters.
 *
 * @author KiriCattus
 * @author Jriwanek
 * @author Curle
 */
public final class CmdAbout extends SlashCommand {

    /**
     * Instantiates a new Cmd about.
     */
    public CmdAbout() {
        super();
        name = "about";
        aliases = new String[]{"build"};
        help = "Gives info about this bot.";
        category = new Category("Info");
        guildOnly = false;
    }

    /**
     * Execute.
     *
     * @param event The {@link SlashCommandEvent event} that triggered this Command.
     */
    @Override
    protected void execute(final SlashCommandEvent event) {
        if (!Utils.checkCommand(this, event)) {
            return;
        }

        final var embed = new EmbedBuilder();

        embed.setTitle("Bot Build info");
        embed.setColor(Color.GREEN);
        embed.setThumbnail(MMDBot.getInstance().getSelfUser().getAvatarUrl());
        embed.setDescription("An in house bot to assists staff with daily tasks and provide fun and useful commands "
            + "for the community, please try ``/help`` for a list of commands!");
        embed.addField("Version:", References.VERSION, true);
        embed.addField("Issue Tracker:", Utils.makeHyperlink("MMDBot's Github", References.ISSUE_TRACKER),
            true);
        embed.addField("Current maintainers:", "jriwanek, WillBL, KiriCattus, sciwhiz12, Curle, matyrobbrt",
            true);
        embed.addField("I've been online for: ", Utils.getTimeDifference(Utils.getTimeFromUTC(
                    References.STARTUP_TIME), OffsetDateTime.now(ZoneOffset.UTC),
                ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS, ChronoUnit.HOURS, ChronoUnit.HOURS, ChronoUnit.SECONDS)
            , false);
        embed.setTimestamp(Instant.now());

        if (event.isFromGuild() && Utils.memberHasRole(event.getMember(), MMDBot.getConfig().getRole("bot_maintainer"))) {
            event.deferReply(false).queue(hook -> {
                event.getJDA().retrieveCommands().queue(commands -> {
                    embed.addField("Globally registered commands", String.valueOf(commands.size()), false);
                    hook.editOriginalEmbeds(embed.build()).queue();
                });
            });
        } else {
            event.replyEmbeds(embed.build()).queue();
        }
    }
}
