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

package com.owncloud.android.lib.common.accounts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AccountUtilsTest {

    @Test
    public void test_retrieveUserId_with_empty_input() {
        var userName = "";
        var expected = "";
        var actual = AccountUtils.retrieveUserId(null, userName);
        assertEquals(expected, actual);
    }

    @Test
    public void test_retrieveUserId_with_input_without_url_part() {
        var userName = "username";
        var host = "murena.io";
        var expected = "username";
        var actual = AccountUtils.retrieveUserId(host, userName);
        assertEquals(expected, actual);
    }

    @Test
    public void test_retrieveUserId_with_case_sensitivity() {
        var userName = "User@E.EMAIL@MURENA.io";
        var host = "murena.io";
        var expected = "User@E.EMAIL";
        var actual = AccountUtils.retrieveUserId(host, userName);
        assertEquals(expected, actual);
    }

    @Test
    public void test_retrieveUserId_with_leading_or_trailing_spaces() {
        var userName = " user@domain.com ";
        var host = " domain.com  ";
        var expected = "user";
        var actual = AccountUtils.retrieveUserId(host, userName);
        assertEquals(expected, actual);
    }
}
