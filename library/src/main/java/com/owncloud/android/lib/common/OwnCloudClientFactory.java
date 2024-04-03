/*
 * Nextcloud Android Library
 *
 * SPDX-FileCopyrightText: 2018-2024 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2019-2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2020-2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2014 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 masensio <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: MIT
 */
package com.owncloud.android.lib.common;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.nextcloud.common.NextcloudClient;
import com.nextcloud.common.OkHttpPersistentCookieJar;
import com.nextcloud.common.User;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import okhttp3.CookieJar;
import okhttp3.Credentials;

public class OwnCloudClientFactory {
    
    final private static String TAG = OwnCloudClientFactory.class.getSimpleName();
    
    /** Default timeout for waiting data from the server */
    public static final int DEFAULT_DATA_TIMEOUT = 60000;
    public static final long DEFAULT_DATA_TIMEOUT_LONG = 60000;
    
    /** Default timeout for establishing a connection */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
    public static final long DEFAULT_CONNECTION_TIMEOUT_LONG = 60000;


    /**
     * Creates a OwnCloudClient setup for an ownCloud account
     * 
     * Do not call this method from the main thread.
     * 
     * @param account                       The ownCloud account
     * @param appContext                    Android application context
     * @return                              A OwnCloudClient object ready to be used
     * @throws AuthenticatorException       If the authenticator failed to get the authorization
     *                                      token for the account.
     * @throws OperationCanceledException   If the authenticator operation was cancelled while
     *                                      getting the authorization token for the account.
     * @throws IOException                  If there was some I/O error while getting the
     *                                      authorization token for the account.
     * @throws AccountNotFoundException     If 'account' is unknown for the AccountManager
     */
    public static OwnCloudClient createOwnCloudClient(Account account, Context appContext)
            throws OperationCanceledException, AuthenticatorException, IOException,
            AccountNotFoundException {
        //Log_OC.d(TAG, "Creating OwnCloudClient associated to " + account.name);
        Uri baseUri = Uri.parse(AccountUtils.getBaseUrlForAccount(appContext, account));
        AccountManager am = AccountManager.get(appContext);
        // TODO avoid calling to getUserData here
        String userId = AccountUtils.getUpdatedUserId(am, account);

        OwnCloudClient client = createOwnCloudClient(baseUri, appContext, true);
        client.setUserId(userId);

        OwnCloudCredentials credentials = AccountUtils.getCredentialsForAccount(appContext, account);
        client.setCredentials(credentials);

        // Restore cookies
        AccountUtils.restoreCookies(account, client, appContext);
        
        return client;
    }


    public static OwnCloudClient createOwnCloudClient(Account account, Context appContext, Activity currentActivity)
            throws OperationCanceledException, AuthenticatorException, IOException,
            AccountNotFoundException {
        Uri baseUri = Uri.parse(AccountUtils.getBaseUrlForAccount(appContext, account));
        AccountManager am = AccountManager.get(appContext);
        // TODO avoid calling to getUserData here
        String userId = AccountUtils.getUpdatedUserId(am, account);

        OwnCloudClient client = createOwnCloudClient(baseUri, appContext, true);
        client.setUserId(userId);

        OwnCloudCredentials credentials = AccountUtils.getCredentialForAccount(appContext, account, currentActivity);

        client.setCredentials(credentials);
        
        // Restore cookies
        AccountUtils.restoreCookies(account, client, appContext);
        
        return client;
    }
    
    /**
     * Creates a OwnCloudClient to access a URL and sets the desired parameters for ownCloud
     * client connections.
     * 
     * @param uri       URL to the ownCloud server; BASE ENTRY POINT, not WebDavPATH
     * @param context   Android context where the OwnCloudClient is being created.
     * @return          A OwnCloudClient object ready to be used
     */
    public static OwnCloudClient createOwnCloudClient(Uri uri, Context context, boolean followRedirects) {
        try {
            NetworkUtils.registerAdvancedSslContext(true, context);
        }  catch (GeneralSecurityException e) {
            Log_OC.e(TAG, "Advanced SSL Context could not be loaded. Default SSL management in" +
                    " the system will be used for HTTPS connections", e);

        } catch (IOException e) {
            Log_OC.e(TAG, "The local server truststore could not be read. Default SSL management" +
                    " in the system will be used for HTTPS connections", e);
        }
        OwnCloudClient client = new OwnCloudClient(uri, NetworkUtils.getMultiThreadedConnManager(), context);
        client.setDefaultTimeouts(DEFAULT_DATA_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
        client.setFollowRedirects(followRedirects);

        return client;
    }

    /**
     * Creates a OwnCloudClient to access a URL and sets the desired parameters for ownCloud
     * client connections.
     *
     * @param uri     URL to the ownCloud server; BASE ENTRY POINT, not WebDavPATH
     * @param context Android context where the OwnCloudClient is being created.
     * @return A OwnCloudClient object ready to be used
     */
    public static NextcloudClient createNextcloudClient(Uri uri,
                                                        String userId,
                                                        String credentials,
                                                        Context context,
                                                        boolean followRedirects,
                                                        boolean loginWithToken,
                                                        CookieJar cookieJar) {
        try {
            NetworkUtils.registerAdvancedSslContext(true, context);
        } catch (GeneralSecurityException e) {
            Log_OC.e(TAG, "Advanced SSL Context could not be loaded. Default SSL management in" +
                    " the system will be used for HTTPS connections", e);

        } catch (IOException e) {
            Log_OC.e(TAG, "The local server truststore could not be read. Default SSL management" +
                    " in the system will be used for HTTPS connections", e);
        }

        NextcloudClient client = new NextcloudClient(uri, userId, credentials, context, cookieJar);
        client.setFollowRedirects(followRedirects);
        client.setOidcLoginWithToken(loginWithToken);

        return client;
    }

    public static NextcloudClient createNextcloudClient(User user, Context appContext) throws AccountNotFoundException {
        return createNextcloudClient(user.toPlatformAccount(), appContext);
    }

    /**
     * Creates a NextcloudClient
     *
     * Do not call this method from the main thread.
     *
     * @param account                       The nextcloud account
     * @param appContext                    Android application context
     * @return                              A Nextcloud object ready to be used
     * @throws AccountNotFoundException     If 'account' is unknown for the AccountManager
     */
    public static NextcloudClient createNextcloudClient(Account account, Context appContext)
            throws AccountNotFoundException {
        //Log_OC.d(TAG, "Creating OwnCloudClient associated to " + account.name);
        Uri baseUri = Uri.parse(AccountUtils.getBaseUrlForAccount(appContext, account));
        AccountManager am = AccountManager.get(appContext);
        // TODO avoid calling to getUserData here
        String userId = AccountUtils.getUpdatedUserId(am, account);
        String username = AccountUtils.getUsernameForAccount(account);

        if (username == null || username.isEmpty()) {
            throw new AccountNotFoundException(
                    account,
                    "Username could not be retrieved",
                    null);
        }

        final CookieJar cookieJar = new OkHttpPersistentCookieJar(appContext, account);

        final String accessToken = AccountUtils.getAccessToken(am, account);
        if (accessToken != null) {
            String credential = "Bearer " + accessToken;
            return createNextcloudClient(baseUri,
                    userId,
                    credential,
                    appContext,
                    true,
                    true,
                    cookieJar);
        }

        String password;
        try {
            password = AccountUtils.getPassword(am, account);
            if (password == null || password.isEmpty()) {
                Log_OC.e(TAG, "Error receiving password token (password==null || empty)");
                throw new AccountNotFoundException(account, "Error receiving password token (password==null || empty)", null);
            }
        } catch (Exception e) {
            Log_OC.e(TAG, "Error receiving password token", e);
            throw new AccountNotFoundException(account, "Error receiving password token", e);
        }

        // Restore cookies
        // TODO v2 cookie handling
        // AccountUtils.restoreCookies(account, client, appContext);

        return createNextcloudClient(baseUri,
                                     userId,
                                     Credentials.basic(username, password, StandardCharsets.UTF_8),
                                     appContext,
                                     true,
                                     false,
                                     cookieJar);
    }
}
