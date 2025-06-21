package com.dacosys.warehouseCounter.data.room.database

import android.content.Context
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dacosys.warehouseCounter.data.room.dao.itemCategory.ItemCategoryDao
import com.github.javafaker.Faker
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.dacosys.warehouseCounter.data.room.entity.itemCategory.ItemCategory as ItemCategoryRoom

@RunWith(AndroidJUnit4::class)
class ItemCategoryDaoTest {
    private lateinit var itemCategoryDao: ItemCategoryDao
    private lateinit var database: WcDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        database = inMemoryDatabaseBuilder(context, WcDatabase::class.java)
            // Allowing the main thread queries, just for testing.
            .allowMainThreadQueries().build()
        itemCategoryDao = database.itemCategoryDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun inserting_an_item_category_should_return_a_total_of_one_item_category() = runTest {
        val fakeItemCategory = createFakeItemCategory()
        itemCategoryDao.insert(fakeItemCategory)
        assert(itemCategoryDao.count() == 1)
    }

    @Test
    fun inserting_an_item_category_should_return_a_equal_item_category_id() = runTest {
        val fakeItemCategory = createFakeItemCategory()
        val id = itemCategoryDao.insert(fakeItemCategory)
        assert(fakeItemCategory.itemCategoryId == id)
    }

    @Test
    fun inserting_an_item_category_should_return_a_equal_item_category() = runTest {
        val fakeItemCategory = createFakeItemCategory()
        val id = itemCategoryDao.insert(fakeItemCategory)
        val itemCategory = itemCategoryDao.getById(id!!)
        assert(fakeItemCategory == itemCategory)
    }

    @Test
    fun inserting_an_item_category_should_return_a_same_hashcode_item_category() = runTest {
        val fakeItemCategory = createFakeItemCategory()
        val id = itemCategoryDao.insert(fakeItemCategory)
        val itemCategory = itemCategoryDao.getById(id!!)
        assert(fakeItemCategory.hashCode() == itemCategory!!.hashCode())
    }

    companion object {
        fun createFakeItemCategory(): ItemCategoryRoom {
            val faker = Faker()

            return ItemCategoryRoom(
                itemCategoryId = faker.number().randomNumber(),
                description = faker.commerce().productName(),
                active = faker.random().nextInt(0, 1),
                parentId = 0,
                parentStr = ""
            )
        }
    }
}
