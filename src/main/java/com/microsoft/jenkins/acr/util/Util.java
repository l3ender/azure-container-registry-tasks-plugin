/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.acr.util;

import com.google.gson.Gson;

import java.util.concurrent.Callable;

public final class Util {

    private Util() {
    }

    public static boolean isGitHubRepo(String url) {
        return true;
    }

    public static boolean isLocalDirectory(String path) {
        return true;
    }

    public static String toJson(Object o) {
        return new Gson().toJson(o);
    }

    public static <T> T retry(Callable<T> action, int time) throws Exception {
        try {
            return action.call();
        } catch (Exception e) {
            if (time == 0) {
                throw e;
            }
            Thread.sleep(Constants.SLEEP_IN_MS);
            return retry(action, --time);
        }
    }
}

