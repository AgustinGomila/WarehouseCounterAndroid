package com.dacosys.warehouseCounter.data.room.database

import android.content.Context
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dacosys.warehouseCounter.data.room.dao.itemRegex.ItemRegexDao
import com.github.javafaker.Faker
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.dacosys.warehouseCounter.data.room.entity.itemRegex.ItemRegex as ItemRegexRoom

@RunWith(AndroidJUnit4::class)
class ItemRegexDaoTest {
    private lateinit var itemRegexDao: ItemRegexDao
    private lateinit var database: WcDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        database = inMemoryDatabaseBuilder(context, WcDatabase::class.java)
            // Allowing the main thread queries, just for testing.
            .allowMainThreadQueries().build()
        itemRegexDao = database.itemRegexDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun inserting_an_item_regex_should_return_a_total_of_one_item_regex() = runTest {
        val fakeItemRegex = createFakeItemRegex()
        itemRegexDao.insert(fakeItemRegex)
        assert(itemRegexDao.count() == 1)
    }

    @Test
    fun inserting_an_item_regex_should_return_a_equal_item_regex_id() = runTest {
        val fakeItemRegex = createFakeItemRegex()
        val id = itemRegexDao.insert(fakeItemRegex)
        assert(fakeItemRegex.itemRegexId == id)
    }

    @Test
    fun inserting_an_item_regex_should_return_a_equal_item_regex() = runTest {
        val fakeItemRegex = createFakeItemRegex()
        val id = itemRegexDao.insert(fakeItemRegex)
        val itemRegex = itemRegexDao.getByItemRegexId(id!!)
        assert(fakeItemRegex == itemRegex)
    }

    @Test
    fun inserting_an_item_regex_should_return_a_same_hashcode_item_regex() = runTest {
        val fakeItemRegex = createFakeItemRegex()
        val id = itemRegexDao.insert(fakeItemRegex)
        val itemRegex = itemRegexDao.getByItemRegexId(id!!)
        assert(fakeItemRegex.hashCode() == itemRegex!!.hashCode())
    }

    companion object {
        fun createFakeItemRegex(): ItemRegexRoom {
            val faker = Faker()

            return ItemRegexRoom(
                itemRegexId = faker.number().randomNumber(),
                description = faker.lorem().sentence(),
                regex = faker.lorem().word(),
                jsonConfig = if (faker.random().nextBoolean()) faker.lorem().sentence() else null,
                codeLength = if (faker.random().nextBoolean()) faker.number().numberBetween(1, 100) else null,
                active = faker.random().nextInt(0, 2)
            )
        }
    }
}
