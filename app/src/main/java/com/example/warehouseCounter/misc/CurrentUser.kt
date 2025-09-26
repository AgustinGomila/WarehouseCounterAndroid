package com.example.warehouseCounter.misc

import com.example.warehouseCounter.data.room.entity.user.User

class CurrentUser {
    companion object {
        private var user: User = defaultUser()
        var isLogged: Boolean = false

        val password: String
            get() = user.password ?: ""

        val name: String
            get() = user.name

        val userId: Long
            get() = user.userId

        private fun defaultUser(): User {
            return User(userId = -1L, name = "", password = "")
        }

        fun set(user: User) {
            this.user = user
            this.user.apply { isLogged = true }
        }

        fun clear() {
            user = defaultUser()
            user.apply { isLogged = false }
        }
    }
}