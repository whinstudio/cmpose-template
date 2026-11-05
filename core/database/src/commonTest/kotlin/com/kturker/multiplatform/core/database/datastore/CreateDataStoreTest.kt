package com.kturker.multiplatform.core.database.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.SYSTEM
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateDataStoreTest {

    private val fileSystem = FileSystem.SYSTEM
    private val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "createDataStoreTest"
    private val file = directory / DATA_STORE_FILE_NAME

    @BeforeTest
    fun setUp() {
        fileSystem.deleteRecursively(directory)
        fileSystem.createDirectories(directory)
    }

    @AfterTest
    fun cleanUp() {
        fileSystem.deleteRecursively(directory)
    }

    @Test
    fun aCorruptPreferencesFileReadsAsEmptyAndAcceptsWrites() = runTest {
        // Not a valid preferences protobuf.
        fileSystem.write(file) { writeUtf8("not a protobuf") }
        val key = stringPreferencesKey("key")
        val dataStore = createDataStore { file.toString() }

        assertTrue(dataStore.data.first().asMap().isEmpty(), "a corrupt file reads as empty")
        dataStore.edit { it[key] = "x" }
        assertEquals("x", dataStore.data.first()[key])
    }
}
