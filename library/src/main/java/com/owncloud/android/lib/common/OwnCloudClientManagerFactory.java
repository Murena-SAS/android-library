/*
 * Nextcloud Android Library
 *
 * SPDX-FileCopyrightText: 2018-2024 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2019-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2014-2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 masensio <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: MIT
 */
package com.owncloud.android.lib.common;

public class OwnCloudClientManagerFactory {
    private static OwnCloudClientManager sDefaultSingleton;
    private static String ocUserAgent = "/e/OS v2 (Android) Owncloud-android";
    private static String ncUserAgent = "/e/OS v2 (Android) Nextcloud-android";
    private static String proxyHost = "";
    private static int proxyPort = -1;

    public static OwnCloudClientManager getDefaultSingleton() {
        if (sDefaultSingleton == null) {
            sDefaultSingleton = new OwnCloudClientManager();
        }
        return sDefaultSingleton;
    }

    public static void setUserAgent(String userAgent) {
        ocUserAgent = userAgent;
    }

    public static String getUserAgent() {
        return ocUserAgent;
    }

    public static String getNextCloudUserAgent() {
        return ncUserAgent;
    }

    public static void setNextCloudUserAgent(String userAgent) {
        ncUserAgent = userAgent;
    }

    public static void setProxyHost(String host) {
        proxyHost = host;
    }

    public static String getProxyHost() {
        return proxyHost;
    }

    public static void setProxyPort(int port) {
        proxyPort = port;
    }

    public static int getProxyPort() {
        return proxyPort;
    }
}
