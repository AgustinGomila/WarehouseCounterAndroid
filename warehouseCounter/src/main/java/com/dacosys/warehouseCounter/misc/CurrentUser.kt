package com.dacosys.warehouseCounter.misc

import com.dacosys.warehouseCounter.data.room.dao.user.UserCoroutines
import com.dacosys.warehouseCounter.data.room.entity.user.User

class CurrentUser {
    companion object {
        private var currentUser: User? = null

        var userId: Long = -1L
        var password: String = ""
        var name: String = ""
        var isLogged = false

        fun getUser(onResult: (User?) -> Unit = {}) {
            if (currentUser == null) {
                UserCoroutines.getById(userId) {
                    currentUser = it
                    onResult(currentUser)
                }
            } else onResult(currentUser)
        }

        fun setUser(userId: Long, userName: String, pass: String, isLogged: Boolean) {
            cleanUser()

            this.userId = userId
            this.password = pass
            this.name = userName
            this.isLogged = isLogged
        }

        fun cleanUser() {
            currentUser = null

            userId = -1L
            password = ""
            name = ""
            isLogged = false
        }
    }
}