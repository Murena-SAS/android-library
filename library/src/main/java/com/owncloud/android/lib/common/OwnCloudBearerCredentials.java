/*
 * Copyright MURENA SAS 2024
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.lib.common;

import android.os.Parcel;

import androidx.annotation.NonNull;

import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OwnCloudBearerCredentials implements OwnCloudCredentials {

    private final String userName;
    private final String accessToken;

    public OwnCloudBearerCredentials(String userName, String accessToken) {
        this.accessToken = accessToken != null ? accessToken : "";
        this.userName = userName != null ? userName : "";
    }

    @Override
    public void applyTo(OwnCloudClient client) {
        List<String> authPrefs = new ArrayList<>(1);
        authPrefs.add("Bearer");

        client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
        client.getParams().setAuthenticationPreemptive(false);
        client.getParams().setCredentialCharset(OwnCloudCredentialsFactory.CREDENTIAL_CHARSET);
        client.getState().setCredentials(AuthScope.ANY, new BearerCredentials(accessToken));
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public String getAuthToken() {
        return accessToken;
    }

    @Override
    public boolean authTokenExpires() {
        return false;
    }

    @Override
    public String toOkHttpCredentials() {
        return "Bearer " + accessToken;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(userName);
        dest.writeString(accessToken);
    }

    protected OwnCloudBearerCredentials(Parcel in) {
        userName = in.readString();
        accessToken = in.readString();
    }

    public static final Creator<OwnCloudBearerCredentials> CREATOR = new Creator<>() {
        @Override
        public OwnCloudBearerCredentials createFromParcel(Parcel source) {
            return new OwnCloudBearerCredentials(source);
        }

        @Override
        public OwnCloudBearerCredentials[] newArray(int size) {
            return new OwnCloudBearerCredentials[size];
        }
    };

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        OwnCloudBearerCredentials that = (OwnCloudBearerCredentials) object;
        return Objects.equals(userName, that.userName) && Objects.equals(accessToken, that.accessToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, accessToken);
    }
}
