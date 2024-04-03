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

import androidx.annotation.NonNull;

import org.apache.commons.httpclient.Credentials;

import java.util.Objects;

public class BearerCredentials implements Credentials {

    private final String accessToken;

    public BearerCredentials(String accessToken) {
        super();
        this.accessToken = accessToken;
    }

    @NonNull
    @Override
    public String toString() {
        return accessToken;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        BearerCredentials that = (BearerCredentials) object;
        return Objects.equals(accessToken, that.accessToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken);
    }
}
