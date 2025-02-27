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
package com.mcmoddev.mmdbot.modules.logging.misc;

import com.mcmoddev.mmdbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import java.awt.Color;

public final class ReferencingListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) { return; }
        final var originalMsg = event.getMessage();
        if (originalMsg.getMessageReference() != null && isStringReference(originalMsg.getContentRaw())) {
            final var referencedMessage = originalMsg.getMessageReference().getMessage();
            event.getChannel().sendMessageEmbeds(reference(referencedMessage, event.getMember())).queue();
            originalMsg.delete().reason("Quote successful").queue();
            return;
        }

        final String[] msg = originalMsg.getContentRaw().split(" ");
        if (msg.length < 1) { return; }

        final var matcher = Utils.MESSAGE_LINK_PATTERN.matcher(msg[0]);
        if (matcher.find()) {
            try {
                final var message = Utils.getMessageByLink(msg[0]);
                if (message != null) {
                    event.getChannel().sendMessageEmbeds(reference(message, event.getMember())).queue();
                    if (msg.length == 1) {
                        originalMsg.delete().reason("Quote successful").queue();
                    }
                }
            } catch (Utils.MessageLinkException e) {
                // Do nothing
            }
        }
    }

    private static boolean isStringReference(final String string) {
        // The zero-width space
        return string.equalsIgnoreCase(".") || string.equalsIgnoreCase("​");
    }

    public static MessageEmbed reference(final Message message, final Member quoter) {
        final var hasAuthor = !message.isWebhookMessage();
        final var msgLink = message.getJumpUrl();
        final var embed = new EmbedBuilder().setTimestamp(message.getTimeCreated())
            .setColor(Color.DARK_GRAY);
        if (hasAuthor) {
            embed.setAuthor(message.getAuthor().getAsTag(), msgLink, message.getAuthor().getEffectiveAvatarUrl());
        }
        if (!message.getContentRaw().isBlank()) {
            embed.appendDescription(MarkdownUtil.maskedLink("Reference ➤ ", msgLink))
                .appendDescription(message.getContentRaw());
        } else {
            embed.appendDescription(MarkdownUtil.maskedLink("Jump to referenced message.", msgLink));
        }
        if (quoter.getIdLong() != message.getMember().getIdLong()) {
            embed.setFooter(quoter.getUser().getAsTag() + " referenced", quoter.getEffectiveAvatarUrl());
        }
        if (!message.getAttachments().isEmpty()) {
            embed.setImage(message.getAttachments().get(0).getUrl());
        }
        return embed.build();
    }

}
