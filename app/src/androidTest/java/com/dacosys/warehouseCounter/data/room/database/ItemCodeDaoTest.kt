package com.example.warehouseCounter.data.room.database

import android.content.Context
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.warehouseCounter.data.room.dao.itemCode.ItemCodeDao
import com.example.warehouseCounter.data.room.entity.itemCode.ItemCode
import com.github.javafaker.Faker
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemCodeCodeDaoTest {
    private lateinit var itemCodeDao: ItemCodeDao
    private lateinit var database: WcDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        database = inMemoryDatabaseBuilder(context, WcDatabase::class.java)
            // Allowing the main thread queries, just for testing.
            .allowMainThreadQueries().build()
        itemCodeDao = database.itemCodeDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun inserting_an_item_code_should_return_a_total_of_one_item_code() = runTest {
        val fakeItemCode = createFakeItemCode(1)
        itemCodeDao.insert(fakeItemCode)
        assert(itemCodeDao.count() == 1)
    }

    @Test
    fun inserting_an_item_code_should_return_a_equal_item_code_id() = runTest {
        val fakeItemCode = createFakeItemCode(1)
        val id = itemCodeDao.insert(fakeItemCode)
        assert(fakeItemCode.id == id)
    }

    @Test
    fun inserting_an_item_code_should_return_a_equal_item_code() = runTest {
        val fakeItemCode = createFakeItemCode(1)
        val id = itemCodeDao.insert(fakeItemCode)
        val itemCode = itemCodeDao.getByItemId(fakeItemCode.itemId).firstOrNull()
        assert(fakeItemCode == itemCode)
    }

    @Test
    fun inserting_an_item_code_should_return_a_same_hashcode_item_code() = runTest {
        val fakeItemCode = createFakeItemCode(1)
        itemCodeDao.insert(fakeItemCode)
        val itemCode = itemCodeDao.getByItemId(fakeItemCode.itemId).firstOrNull()
        assert(fakeItemCode.hashCode() == itemCode.hashCode())
    }

    companion object {
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
