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
package com.mcmoddev.mmdbot.commander.util;

import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@UtilityClass
public class TheCommanderUtilities {

    /**
     * Gets a cat fact.
     *
     * @return a cat fact
     */
    public static String getCatFact() {
        try {
            final var url = new URL("https://catfact.ninja/fact");
            final URLConnection connection = url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            final var reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            final String inputLine = reader.readLine();
            reader.close();
            final var objectArray = JsonParser.parseString(inputLine).getAsJsonObject();
            return ":cat:  " + objectArray.get("fact").toString();

        } catch (final RuntimeException ex) {
            throw ex;
        } catch (final Exception ex) {
            log.error("Error getting cat fact...", ex);
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * Checks if the given member any of the given roles
     *
     * @param member the member to check
     * @param roleId the IDs of the roles to check for
     * @return if the member has any of the role
     */
    public static boolean memberHasRoles(final Member member, final String... roleIds) {
        if (member == null) {
            return false;
        }
        final var roles = List.of(roleIds);
        return member.getRoles().stream().anyMatch(r -> roles.contains(r.getId()));
    }

}
