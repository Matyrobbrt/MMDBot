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
package com.mcmoddev.mmdbot.gist;

import com.google.gson.JsonObject;
import com.mcmoddev.mmdbot.MMDBot;
import com.mcmoddev.mmdbot.core.References;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GistUtils {

    private static int lastCode = 0;
    private static String lastErrorMessage = "";

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    /**
     * Creates a new Gist
     *
     * @param token the token of the account to create a gist for
     * @param gist  the gist to create
     * @return the created gist
     * @throws GistException any exception that occurred while creating the gist (usually, and IO one)
     */
    public static ExistingGist create(final String token, final Gist gist) throws GistException {
        String newGist;
        try {
            newGist = post(token, "", gist.toString());
        } catch (IOException | InterruptedException ioe) {
            int code = lastCode;
            if (code == 404) {
                return null;
            }
            throw new GistException(lastErrorMessage, lastCode);
        }

        return ExistingGist.fromJson(References.GSON.fromJson(newGist, JsonObject.class));
    }

    /**
     * Posts a request to GitHub
     *
     * @param token       a token for accessing GitHub
     * @param operation   the operation to execute
     * @param postMessage the message to post
     * @return the response
     */
    private static String post(final String token, final String operation, final String postMessage) throws InterruptedException, IOException {
        final URL target = new URL("https://api.github.com/gists" + operation);

        var request = HttpRequest.newBuilder(URI.create(target.toString()))
            .header("Content-Type", "application/json")
            .header("Authorization", "token " + token)
            .POST(HttpRequest.BodyPublishers.ofString(postMessage))
            .build();

        var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assignLastCode(response);
        return response.body();
    }

    public static String readFile(File f) throws IOException {
        return readInputStream(new FileInputStream(f));
    }

    public static String readInputStream(InputStream stream) throws IOException {
        StringBuilder content = new StringBuilder();

        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8); BufferedReader buffer = new BufferedReader(reader)) {
            String line;
            while ((line = buffer.readLine()) != null) {
                content.append(line).append('\n');
            }
        }

        return content.toString();
    }

    private static void assignLastCode(HttpResponse<String> response) {
        lastCode = response.statusCode();
        lastErrorMessage = response.body();
    }

    public static boolean hasToken() {
        return !MMDBot.getConfig().getGithubToken().isBlank();
    }

    public static final class GistException extends Exception {

        private static final long serialVersionUID = -129081029829039102L;

        private final String error;
        private final int errorCode;

        GistException(final String error, final int errorCode) {
            this.error = error;
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getError() {
            return error;
        }

        @Override
        public String toString() {
            return "HTTP error: %s, message: %s".formatted(errorCode, error);
        }

    }

}
