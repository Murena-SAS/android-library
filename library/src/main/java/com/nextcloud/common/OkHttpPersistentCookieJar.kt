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

package com.nextcloud.common

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class OkHttpPersistentCookieJar(context: Context, private val account: Account) : CookieJar {

    private val accountManager = AccountManager.get(context)

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return getCookieMap(url).values.filter {
            it.matches(url)
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val cookieList = cookies.filter {
            it.expiresAt > System.currentTimeMillis()
        }

        if (cookieList.isEmpty()) {
            return
        }

        val cookieMap = getCookieMap(url)

        // replace old cookie with new one
        cookieList.forEach {
            cookieMap[it.name] = it
        }

        val cookieString =
            cookieMap.values.joinToString(separator = Constants.OKHTTP_COOKIE_SEPARATOR)
        
        accountManager.setUserData(account, Constants.KEY_OKHTTP_COOKIES, cookieString)
    }

    private fun getCookieMap(url: HttpUrl): HashMap<String, Cookie> {
        val result = HashMap<String, Cookie>()
        val cookiesString =
            accountManager.getUserData(account, Constants.KEY_OKHTTP_COOKIES) ?: return HashMap()

        val cookies = cookiesString.split(Constants.OKHTTP_COOKIE_SEPARATOR.toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()

        cookies.forEach {
            val cookie = Cookie.parse(url, it) ?: return@forEach

            if (cookie.expiresAt > System.currentTimeMillis()) {
                result[cookie.name] = cookie
            }
        }

        return result
    }
}
