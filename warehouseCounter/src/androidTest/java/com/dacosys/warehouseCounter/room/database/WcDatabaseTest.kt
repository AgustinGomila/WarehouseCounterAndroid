package com.dacosys.warehouseCounter.room.database


import android.content.Context
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dacosys.warehouseCounter.room.dao.item.ItemDao
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private var itemDao: ItemDao? = null
    private var database: WcDatabase? = null

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        database =
            inMemoryDatabaseBuilder(context, WcDatabase::class.java) // Allowing main thread queries, just for testing.
                .allowMainThreadQueries().build()
        itemDao = database!!.itemDao()
    }

    @After
    fun closeDb() {
        database!!.close()
    }
}