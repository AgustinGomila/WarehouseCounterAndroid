package com.dacosys.warehouseCounter.data.room.database

import android.content.Context
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dacosys.warehouseCounter.data.room.dao.item.ItemDao
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCode
import com.github.javafaker.Faker
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.dacosys.warehouseCounter.data.room.entity.item.Item as ItemRoom

@RunWith(AndroidJUnit4::class)
class ItemDaoTest {
    private lateinit var itemDao: ItemDao
    private lateinit var database: WcDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        database = inMemoryDatabaseBuilder(context, WcDatabase::class.java)
            // Allowing the main thread queries, just for testing.
            .allowMainThreadQueries().build()
        itemDao = database.itemDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun inserting_an_item_should_return_a_total_of_one_item() = runTest {
        val fakeItem = createFakeItem()
        itemDao.insert(fakeItem)
        assert(itemDao.count() == 1)
    }

    @Test
    fun inserting_an_item_should_return_a_equal_item_id() = runTest {
        val fakeItem = createFakeItem()
        val id = itemDao.insert(fakeItem)
        assert(fakeItem.itemId == id)
    }

    @Test
    fun inserting_an_item_should_return_a_equal_item() = runTest {
        val fakeItem = createFakeItem()
        val id = itemDao.insert(fakeItem)
        val item = itemDao.getById(id!!)
        assert(fakeItem == item)
    }

    @Test
    fun inserting_an_item_should_return_a_same_hashcode_item() = runTest {
        val fakeItem = createFakeItem()
        val id = itemDao.insert(fakeItem)
        val item = itemDao.getById(id!!)
        assert(fakeItem.hashCode() == item!!.hashCode())
    }

    companion object {
        fun createFakeItem(): ItemRoom {
            val faker = Faker()

            return ItemRoom(
                itemId = faker.number().randomNumber(),
                description = faker.commerce().productName(),
                active = faker.random().nextInt(0, 1),
                price = faker.number().randomDouble(2, 0, 999).toFloat(),
                ean = faker.number().digits(13),
                itemCategoryId = faker.number().randomNumber(),
                externalId = faker.lorem().word(),
                lotEnabled = faker.random().nextInt(0, 1),
                itemCategoryStr = faker.commerce().department()
            )
        }

        fun createFakeItemCode(itemId: Long): ItemCode {
            val faker = Faker()

            return ItemCode(
                id = faker.number().randomNumber(),
                itemId = itemId,
                code = faker.lorem().word(),
                qty = faker.number().randomDouble(2, 0, 999),
                toUpload = 0
            )
        }
    }
}
