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

import com.mcmoddev.mmdbot.MMDBot;
import com.mcmoddev.mmdbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mcmoddev.mmdbot.MMDBot.LOGGER;
import static com.mcmoddev.mmdbot.MMDBot.getConfig;
import static com.mcmoddev.mmdbot.utilities.console.MMDMarkers.REQUESTS;

/**
 * The type Event reaction added.
 *
 * @author
 */
public final class EventReactionAdded extends ListenerAdapter {

    /**
     * A cache containing all the roles that have been used as reaction roles
     */
    public static final Set<Long> REACTION_ROLES = Collections.synchronizedSet(new HashSet<>());

    /**
     * The Warned messages.
     */
    private final Set<Message> warnedMessages = new HashSet<>();

    /**
     * The set of messages that have passed the reaction threshold required for request deletion, but are awaiting a
     * staff member (a user with {@link Permission#KICK_MEMBERS}) to sign-off on the deletion by giving their own
     * reaction.
     *
     * <p>If a message has been added previously to this set, and the message falls back below the request deletion
     * threshold, it will be removed from this set.</p>
     */
    private final Set<Long> messagesAwaitingSignoff = new HashSet<>();

    /**
     * On message reaction add.
     *
     * @param event the event
     */
    @Override
    public void onMessageReactionAdd(final MessageReactionAddEvent event) {
        handleRolePanels(event);
        if (!event.isFromGuild() || !event.isFromType(ChannelType.TEXT)) return;
        final var channel = event.getTextChannel();
        final MessageHistory history = MessageHistory.getHistoryAround(channel,
            event.getMessageId()).limit(1).complete();
        final var message = history.getMessageById(event.getMessageId());
        if (message == null) {
            return;
        }
        final double removalThreshold = getConfig().getRequestsRemovalThreshold();
        final double warningThreshold = getConfig().getRequestsWarningThreshold();
        if (removalThreshold == 0 || warningThreshold == 0) {
            return;
        }

        final var guild = event.getGuild();
        final var guildId = guild.getIdLong();
        final var discussionChannel = guild.getTextChannelById(getConfig()
            .getChannel("requests.discussion"));
        if (getConfig().getGuildID() == guildId && getConfig().getChannel("requests.main")
            == channel.getIdLong()) {

            final int freshnessDuration = getConfig().getRequestFreshnessDuration();
            if (freshnessDuration > 0) {
                final OffsetDateTime creationTime = message.getTimeCreated();
                final var now = OffsetDateTime.now();
                if (now.minusDays(freshnessDuration).isAfter(creationTime)) {
                    return; // Do nothing if the request has gone past the freshness duration
                }
            }

            final List<Long> badReactionsList = getConfig().getBadRequestsReactions();
            final List<Long> goodReactionsList = getConfig().getGoodRequestsReactions();
            final List<Long> needsImprovementReactionsList = getConfig().getRequestsNeedsImprovementReactions();
            final var badReactions = Utils.getMatchingReactions(message, badReactionsList::contains);

            final List<Member> signedOffStaff = badReactions.stream()
                .map(MessageReaction::retrieveUsers)
                .flatMap(PaginationAction::stream)
                .map(guild::getMember)
                .filter(Objects::nonNull)
                .filter(member -> member.hasPermission(Permission.KICK_MEMBERS))
                .toList();
            final var hasStaffSignoff = signedOffStaff.size() > 0;

            final int badReactionsCount = badReactions.stream().mapToInt(MessageReaction::getCount).sum();
            final int goodReactionsCount = Utils.getNumberOfMatchingReactions(message, goodReactionsList::contains);
            final int needsImprovementReactionsCount = Utils.getNumberOfMatchingReactions(message,
                needsImprovementReactionsList::contains);

            final double requestScore = (badReactionsCount + needsImprovementReactionsCount * 0.5) - goodReactionsCount;

            final User messageAuthor = message.getAuthor();
            if (requestScore >= removalThreshold) {
                // If the message has no staff signing off, skip the rest of the code
                if (!hasStaffSignoff) {

                    // If it hasn't been logged about yet, log about it
                    if (messagesAwaitingSignoff.add(message.getIdLong())) {
                        LOGGER.info(REQUESTS, "Request from {} has a score of {}, reaching removal threshold {}, "
                                + "awaiting moderation approval.",
                            messageAuthor, requestScore, removalThreshold);

                        final var logChannel = guild.getTextChannelById(getConfig()
                            .getChannel("events.requests_deletion"));
                        if (logChannel != null) {
                            final EmbedBuilder builder = new EmbedBuilder();
                            builder.setAuthor(messageAuthor.getAsTag(), messageAuthor.getEffectiveAvatarUrl());
                            builder.setTitle("Request awaiting moderator approval");
                            builder.appendDescription("Request from ")
                                .appendDescription(messageAuthor.getAsMention())
                                .appendDescription(" has a score of " + requestScore)
                                .appendDescription(", reaching removal threshold of " + removalThreshold)
                                .appendDescription(" and is now awaiting moderator approval before deletion.");
                            builder.addField("Jump to Message",
                                MarkdownUtil.maskedLink("Message in " + message.getTextChannel().getAsMention(),
                                    message.getJumpUrl()), true);
                            builder.setTimestamp(Instant.now());
                            builder.setColor(Color.YELLOW);
                            builder.setFooter("User ID: " + messageAuthor.getId());

                            logChannel.sendMessageEmbeds(builder.build())
                                .allowedMentions(Collections.emptySet())
                                .queue();
                        }
                    }
                    return;
                }

                LOGGER.info(REQUESTS, "Removed request from {} due to score of {} reaching removal threshold {}",
                    messageAuthor, requestScore, removalThreshold);

                final Message response = new MessageBuilder().append(messageAuthor.getAsMention()).append(", ")
                    .append("your request has been found to be low quality by community review and has been removed.\n")
                    .append("Please see other requests for how to do it correctly.\n")
                    .appendFormat("It received %d 'bad' reactions, %d 'needs improvement' reactions, and %d "
                            + "'good' reactions.",
                        badReactionsCount, needsImprovementReactionsCount, goodReactionsCount)
                    .build();

                warnedMessages.remove(message);

                final var logChannel = guild.getTextChannelById(getConfig()
                    .getChannel("events.requests_deletion"));
                if (logChannel != null) {
                    final EmbedBuilder builder = new EmbedBuilder();
                    builder.setAuthor(messageAuthor.getAsTag(), messageAuthor.getEffectiveAvatarUrl());
                    builder.setTitle("Deleted request by community review");
                    builder.appendDescription("Deleted request from ")
                        .appendDescription(messageAuthor.getAsMention())
                        .appendDescription(" which has a score of " + requestScore)
                        .appendDescription(", reaching removal threshold of " + removalThreshold)
                        .appendDescription(", and has been approved by moderators for deletion.");

                    final String approvingMods = signedOffStaff.stream()
                        .map(s -> "%s (%s, id `%s`)".formatted(s.getAsMention(), s.getUser().getAsTag(), s.getId()))
                        .collect(Collectors.joining("\n"));
                    builder.addField("Approving moderators", approvingMods, true);

                    builder.setTimestamp(Instant.now());
                    builder.setColor(Color.RED);
                    builder.setFooter("User ID: " + messageAuthor.getId());

                    logChannel.sendMessage(message.getContentRaw())
                        .setEmbeds(builder.build())
                        .allowedMentions(Collections.emptySet())
                        .queue();
                }

                channel.deleteMessageById(event.getMessageId())
                    .reason(String.format(
                        "Bad request: %d bad reactions, %d needs improvement reactions, %d good reactions",
                        badReactionsCount, needsImprovementReactionsCount, goodReactionsCount))
                    .flatMap(v -> {
                        RestAction<Message> action = messageAuthor.openPrivateChannel()
                            .flatMap(privateChannel -> privateChannel.sendMessage(response));
                        //If we can't DM the user, send it in the discussions channel instead.
                        if (discussionChannel != null) {
                            action = action.onErrorFlatMap(throwable -> discussionChannel.sendMessage(response));
                        }
                        return action;
                    })
                    .queue();

            } else if (!warnedMessages.contains(message) && requestScore >= warningThreshold) {
                LOGGER.info(REQUESTS, "Warned user {} due to their request (message id: {}) score of {} reaching "
                    + "warning threshold {}", messageAuthor, message.getId(), requestScore, warningThreshold);

                final Message response = new MessageBuilder()
                    .append(messageAuthor.getAsMention()).append(", ")
                    .append("your request is close to being removed by community review.\n")
                    .append("Please edit your message to bring it to a higher standard.\n")
                    .appendFormat("It has so far received %d 'bad' reactions, %d 'needs improvement' reactions, "
                            + "and %d 'good' reactions.",
                        badReactionsCount, needsImprovementReactionsCount, goodReactionsCount)
                    .build();

                warnedMessages.add(message);

                RestAction<Message> action = messageAuthor.openPrivateChannel()
                    .flatMap(privateChannel -> privateChannel.sendMessage(response));
                //If we can't DM the user, send it in the thread.
                if (discussionChannel != null) {
                    action = action.onErrorFlatMap(throwable -> event.getGuild().getThreadChannelById(event.getMessageIdLong()).sendMessage(response));
                }
                action.queue();
            }
            // Remove messages under the removal threshold from the awaiting sign-off set
            if (requestScore < removalThreshold) {
                messagesAwaitingSignoff.remove(message.getIdLong());
            }
        }
    }

    private void handleRolePanels(@Nonnull final MessageReactionAddEvent event) {
        doRolePanelStuff(event, false);
    }

    @Override
    public void onMessageReactionRemove(@Nonnull final MessageReactionRemoveEvent event) {
        doRolePanelStuff(event, true);
    }

    private void doRolePanelStuff(final GenericMessageReactionEvent event, final boolean isRemove) {
        if (!event.isFromGuild() || event.getUser() != null && event.getUser().isBot() || event.getUser().isSystem()) {
            return;
        }
        final var emote = getEmoteAsString(event.getReactionEmote());
        Utils.getRoleIfPresent(event.getGuild(), MMDBot.getConfig().getRoleForRolePanel(event.getChannel().getIdLong(), event.getMessageIdLong(), emote), role -> {
            REACTION_ROLES.add(role.getIdLong());
            final var member = event.getMember();
            if (isRemove && !MMDBot.getConfig().isRolePanelPermanent(event.getChannel().getIdLong(), event.getMessageIdLong())) {
                if (member.getRoles().contains(role)) {
                    event.getGuild().removeRoleFromMember(member, role).reason("Reaction Roles").queue();
                }
            } else if (!isRemove) {
                if (!member.getRoles().contains(role)) {
                    event.getGuild().addRoleToMember(member, role).reason("Reaction Roles").queue();
                }
            }
        });
    }

    public static String getEmoteAsString(final MessageReaction.ReactionEmote reactionEmote) {
        return reactionEmote.isEmoji() ? reactionEmote.getAsCodepoints() : reactionEmote.getEmote().getId();
    }
}
