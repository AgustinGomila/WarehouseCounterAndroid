package com.example.warehouseCounter.data.room.database

import android.content.Context
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.warehouseCounter.data.room.dao.user.UserDao
import com.github.javafaker.Faker
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.warehouseCounter.data.room.entity.user.User as UserRoom

@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private lateinit var userDao: UserDao
    private lateinit var database: WcDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        database = inMemoryDatabaseBuilder(context, WcDatabase::class.java)
            // Allowing the main thread queries, just for testing.
            .allowMainThreadQueries().build()
        userDao = database.userDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun inserting_an_user_should_return_a_total_of_one_user() = runTest {
        val fakeUser = createFakeUser()
        userDao.insert(fakeUser)
        assert(userDao.count() == 1)
    }

    @Test
    fun inserting_an_user_should_return_a_equal_user_id() = runTest {
        val fakeUser = createFakeUser()
        val id = userDao.insert(fakeUser)
        assert(fakeUser.userId == id)
    }

    @Test
    fun inserting_an_user_should_return_a_equal_user() = runTest {
        val fakeUser = createFakeUser()
        val id = userDao.insert(fakeUser)
        val user = userDao.getById(id!!)
        assert(fakeUser == user)
    }

    @Test
    fun inserting_an_user_should_return_a_same_hashcode_user() = runTest {
        val fakeUser = createFakeUser()
        val id = userDao.insert(fakeUser)
        val user = userDao.getById(id!!)
        assert(fakeUser.hashCode() == user!!.hashCode())
    }

    companion object {
        fun createFakeUser(): UserRoom {
            val faker = Faker()

            return UserRoom(
                userId = faker.number().randomNumber(),
                name = faker.name().fullName(),
                active = faker.number().randomDigit(),
                password = faker.internet().password()
            )
        }
    }
}
