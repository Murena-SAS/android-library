/*
 * Nextcloud Android Library
 *
 * SPDX-FileCopyrightText: 2017-2024 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2014-2016 ownCloud Inc. and Nextcloud contributors
 * SPDX-FileCopyrightText: 2017-2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2014-2016 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2014-2015 masensio <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: MIT
 */
package com.owncloud.android.lib.common.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AccountsException;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nextcloud.common.OkHttpPersistentCookieJar;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import net.openid.appauth.AuthState;

import org.apache.commons.httpclient.Cookie;
import org.json.JSONException;

import java.io.IOException;

import okhttp3.CookieJar;

public class AccountUtils {

    private static final String TAG = AccountUtils.class.getSimpleName();

    public static final String WEBDAV_PATH_9_0 = "/remote.php/dav";
    public static final String DAV_UPLOAD = "/remote.php/dav/uploads";
    public static final String STATUS_PATH = "/status.php";

    /**
     * Extracts url server from the account
     *
     * @param context
     * @param account
     * @return url server or null on failure
     * @throws AccountNotFoundException When 'account' is unknown for the AccountManager
     * @deprecated This method will be removed in version 1.0.
     * Use {@link #getBaseUrlForAccount(Context, Account)}
     * instead.
     */
    @Deprecated
    public static String constructBasicURLForAccount(Context context, Account account)
            throws AccountNotFoundException {
        return getBaseUrlForAccount(context, account);
    }

    /**
     * Extracts url server from the account
     *
     * @param context
     * @param account
     * @return url server or null on failure
     * @throws AccountNotFoundException When 'account' is unknown for the AccountManager
     */
    public static String getBaseUrlForAccount(Context context, Account account)
            throws AccountNotFoundException {
        AccountManager ama = AccountManager.get(context.getApplicationContext());
        String baseurl = ama.getUserData(account, Constants.KEY_OC_BASE_URL);

        if (baseurl == null)
            throw new AccountNotFoundException(account, "Account not found", null);

        return baseurl;
    }


    /**
     * Get the username corresponding to an OC account.
     *
     * @param account An OC account
     * @return Username for the given account, extracted from the account.name
     */
    @Nullable
    public static String getUsernameForAccount(@Nullable Account account) {
        if (account == null) {
            return null;
        }

        String username = account.name;
        if (username == null) {
            return null;
        }

        return username.trim();
    }

    /**
     * Get the stored server version corresponding to an OC account.
     *
     * @param account An OC account
     * @param context Application context
     * @return Version of the OC server, according to last check
     */
    public static OwnCloudVersion getServerVersionForAccount(Account account, Context context) {
        AccountManager ama = AccountManager.get(context);
        OwnCloudVersion version = null;
        try {
            String versionString = ama.getUserData(account, Constants.KEY_OC_VERSION);
            version = new OwnCloudVersion(versionString);

        } catch (Exception e) {
            Log_OC.e(TAG, "Couldn't get a the server version for an account", e);
        }
        return version;
    }

    /**
     * @return
     * @throws IOException
     * @throws AuthenticatorException
     * @throws OperationCanceledException
     */
    public static OwnCloudCredentials getCredentialsForAccount(Context context, Account account)
            throws OperationCanceledException, AuthenticatorException, IOException {
        AccountManager am = AccountManager.get(context);

        String username = getUsernameForAccount(account);
        String accessToken = getAccessToken(am, account);

        if (accessToken != null) {
            return OwnCloudCredentialsFactory.newBearerCredentials(username, accessToken);
        }

        String password = getPassword(am, account);

        return OwnCloudCredentialsFactory.newBasicCredentials(username, password);
    }

    @NonNull
    public static OwnCloudCredentials getCredentialForAccount(@NonNull Context context, @NonNull Account account, @NonNull Activity activity) throws OperationCanceledException, AuthenticatorException, IOException {
        AccountManager accountManager = AccountManager.get(context);

        String username = getUsernameForAccount(account);

        String accessToken = getAccessToken(accountManager, account);
        if (accessToken != null) {
            return OwnCloudCredentialsFactory.newBearerCredentials(username, accessToken);
        }

        AccountManagerFuture<Bundle> future = accountManager.getAuthToken(account,
                AccountTypeUtils.getAuthTokenTypePass(account.type), null,
                activity, null, null);

        Bundle result = future.getResult();
        String password = result.getString(AccountManager.KEY_AUTHTOKEN);

        if (password == null) {
            password = accountManager.getPassword(account);
        }

        return OwnCloudCredentialsFactory.newBasicCredentials(username, password);
    }


    @Nullable
    public static String getAccessToken(@NonNull AccountManager accountManager, @NonNull Account account) {
        String authStateString = accountManager.getUserData(account, Constants.KEY_AUTH_STATE);

        if (authStateString == null || authStateString.trim().isBlank()) {
            return null;
        }

        try {
            AuthState authState = AuthState.jsonDeserialize(authStateString);
            return authState.getAccessToken();
        } catch (JSONException e) {
            Log_OC.e(TAG, e.getMessage());
        }

        return null;
    }

    @Nullable
    public static String getPassword(AccountManager accountManager, Account account) {
        String password = accountManager.getPassword(account);

        if (password != null && !password.isBlank()) {
            return password;
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            return password;
        }

        try {
            password = accountManager.blockingGetAuthToken(
                    account,
                    AccountTypeUtils.getAuthTokenTypePass(account.type),
                    false
            );
        } catch (AuthenticatorException | IOException | OperationCanceledException e) {
            Log_OC.w(TAG, "failed to retrieve authToken for account: " + account.name);
        }

        return password;
    }


    public static String buildAccountNameOld(Uri serverBaseUrl, String username) {
        if (serverBaseUrl.getScheme() == null) {
            serverBaseUrl = Uri.parse("https://" + serverBaseUrl.toString());
        }
        String accountName = username + "@" + serverBaseUrl.getHost();
        if (serverBaseUrl.getPort() >= 0) {
            accountName += ":" + serverBaseUrl.getPort();
        }
        return accountName;
    }

    public static String buildAccountName(Uri serverBaseUrl, String loginName) {
        if (serverBaseUrl.getScheme() == null) {
            serverBaseUrl = Uri.parse("https://" + serverBaseUrl.toString());
        }

        // Remove http:// or https://
        String url = serverBaseUrl.toString();
        if (url.contains("://")) {
            url = url.substring(serverBaseUrl.toString().indexOf("://") + 3);
        }

        return loginName + "@" + url;
    }

    public static void saveClient(OwnCloudClient client, Account savedAccount, Context context) {
        if (client == null) {
            return;
        }

        // Account Manager
        AccountManager ac = AccountManager.get(context.getApplicationContext());

        if (client.shouldResetCookie()) {
            client.resetCookie();
            ac.setUserData(savedAccount, Constants.KEY_COOKIES, "");
            return;
        }

        final String cookiesString = client.getCookiesString();
        if (!cookiesString.isBlank()) {
            ac.setUserData(savedAccount, Constants.KEY_COOKIES, cookiesString);
        }

    }


    /**
     * Restore the client cookies
     *
     * @param account
     * @param client
     * @param context
     */
    public static void restoreCookies(Account account, OwnCloudClient client, Context context) {

        Log_OC.d(TAG, "Restoring cookies for " + account.name);

        // Account Manager
        AccountManager am = AccountManager.get(context.getApplicationContext());

        Uri serverUri = (client.getBaseUri() != null) ? client.getBaseUri() : client.getDavUri();

        String cookiesString = null;
        try {
            cookiesString = am.getUserData(account, Constants.KEY_COOKIES);
        } catch (SecurityException e) {
            Log_OC.e(TAG, e.getMessage());
        }

        if (cookiesString != null) {
            String[] cookies = cookiesString.split(";");
            if (cookies.length > 0) {
                for (int i = 0; i < cookies.length; i++) {
                    int equalPos = cookies[i].indexOf('=');
                    if (equalPos <= 0) {
                        continue;
                    }

                    Cookie cookie = new Cookie();
                    cookie.setName(cookies[i].substring(0, equalPos));
                    cookie.setValue(cookies[i].substring(equalPos + 1));
                    cookie.setDomain(serverUri.getHost());    // VERY IMPORTANT
                    cookie.setPath(serverUri.getPath());    // VERY IMPORTANT

                    client.getState().addCookie(cookie);
                }
            }
        }
    }

    /**
     * Restore the client cookies from accountName
     *
     * @param accountName
     * @param client
     * @param context
     */
    public static void restoreCookies(String accountName, OwnCloudClient client, Context context) {
        Log_OC.d(TAG, "Restoring cookies for " + accountName);

        Account account = getAccount(context, accountName);

        // Restoring cookies
        if (account != null) {
            restoreCookies(account, client, context);
        }
    }

    @Nullable
    private static Account getAccount(@NonNull Context context, @Nullable String accountName) {
        if (accountName == null) {
            return null;
        }

        AccountManager accountManager = AccountManager.get(context.getApplicationContext());

        Account[] accounts = accountManager.getAccounts();
        for (Account account : accounts) {
            if (account.name.equals(accountName)) {
                return account;
            }
        }

        return null;
    }

    @NonNull
    public static CookieJar getOkhttpCookieJar(@NonNull Context context, @Nullable String accountName) {
        Account account = getAccount(context, accountName);
        if (account == null) {
            return CookieJar.NO_COOKIES;
        }

        return new OkHttpPersistentCookieJar(context.getApplicationContext(), account);
    }

    public static class AccountNotFoundException extends AccountsException {

        /**
         * Generated - should be refreshed every time the class changes!!
         */
        private static final long serialVersionUID = -1684392454798508693L;

        private Account mFailedAccount;

        public AccountNotFoundException(Account failedAccount, String message, Throwable cause) {
            super(message, cause);
            mFailedAccount = failedAccount;
        }

        public Account getFailedAccount() {
            return mFailedAccount;
        }
    }

    @Nullable
    public static String getUpdatedUserId(@NonNull AccountManager accountManager, @NonNull Account account) {
        final String userId = accountManager.getUserData(account, Constants.KEY_USER_ID);

        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }

        final String userName = account.name;
        if (userName == null || userName.trim().isBlank()) {
            return userName;
        }

        final String host = getHostForAccount(accountManager, account);
        final String newUserId = retrieveUserId(host, userName);

        accountManager.setUserData(account, Constants.KEY_USER_ID, newUserId);
        return newUserId;
    }

    static String retrieveUserId(@Nullable String host, @NonNull String userName) {
        userName = userName.trim();

        if (host == null) {
            return userName;
        }

        final String userNameEndPart = ("@" + host.trim()).toLowerCase();

        if (!userName.toLowerCase().endsWith(userNameEndPart)) {
            return userName;
        }

        int lengthOfUserId = userName.length() - userNameEndPart.length();  // abc@ff.com - @ff.com = abc
        return userName.substring(0, lengthOfUserId);
    }

    @Nullable
    private static String getHostForAccount(@NonNull AccountManager accountManager, @NonNull Account account) {
        final String baseurl = accountManager.getUserData(account, Constants.KEY_OC_BASE_URL);

        if (baseurl == null) {
            return null;
        }

        final Uri baseUri = Uri.parse(baseurl);
        return baseUri.getHost();
    }


    public static class Constants {
        /**
         * Value under this key should handle path to webdav php script. Will be
         * removed and usage should be replaced by combining
         * {@link com.owncloud.android.authentication.AuthenticatorActivity.KEY_OC_BASE_URL} and
         * {@link com.owncloud.android.lib.resources.status.OwnCloudVersion}
         *
         * @deprecated
         */
        @Deprecated
        public static final String KEY_OC_URL = "oc_url";
        /**
         * Version should be 3 numbers separated by dot so it can be parsed by
         * {@link com.owncloud.android.lib.resources.status.OwnCloudVersion}
         */
        public static final String KEY_OC_VERSION = "oc_version";
        /**
         * Base url should point to owncloud installation without trailing / ie:
         * http://server/path or https://owncloud.server
         */
        public static final String KEY_OC_BASE_URL = "oc_base_url";
        /**
         * OC account cookies
         */
        public static final String KEY_COOKIES = "oc_account_cookies";

        /**
         * OC account version
         */
        public static final String KEY_OC_ACCOUNT_VERSION = "oc_account_version";

        /**
         * User's display name, name chosen by user, only displayed, no real usage
         */
        public static final String KEY_DISPLAY_NAME = "oc_display_name";

        /**
         * User ID, internally and never changing id of user, used by e.g. dav/trashbin/$userId/trash
         */
        public static final String KEY_USER_ID = "oc_id";

        public static final String KEY_AUTH_STATE = "auth_state";

        public static final String KEY_OKHTTP_COOKIES = "cookie_key";

        public static final String OKHTTP_COOKIE_SEPARATOR = "<end_cookie>";
    }

}
