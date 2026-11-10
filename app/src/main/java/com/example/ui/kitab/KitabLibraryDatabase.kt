package com.example.ui.kitab

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "kitab_metadata")
data class KitabMetadataEntity(
    @PrimaryKey val bookId: String,
    val seriesId: String,
    val titleBangla: String,
    val arabicTitle: String,
    val authorBangla: String,
    val translatorOrCompilerBangla: String,
    val publisherBangla: String,
    val categoryId: String,
    val volumeNumber: Int,
    val totalVolumes: Int,
    val fileSizeBytes: Long,
    val languageBangla: String,
    val shortDescriptionBangla: String,
    val marhalaBangla: String,
    val downloadUrl: String,
    val sha256Checksum: String,
    val isOpenLicenseVerified: Boolean,
    val isAdminApproved: Boolean,
    val licenseNoteBangla: String,
    val coverPrimaryHex: Long,
    val coverAccentHex: Long,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "kitab_local_state")
data class KitabLocalStateEntity(
    @PrimaryKey val bookId: String,
    val downloadStatus: String = KitabDownloadStatus.NOT_DOWNLOADED.name,
    val localFilePath: String = "",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val sha256Verified: Boolean = false,
    val verifiedSha256Hex: String = "",
    val downloadedAtMillis: Long = 0L,
    val lastReadPage: Int = 1,
    val totalPageCount: Int = 0,
    val lastReadTimestampMillis: Long = 0L,
    val errorMessageBangla: String = ""
)

@Entity(tableName = "kitab_bookmarks")
data class KitabBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val bookId: String,
    val bookTitleBangla: String,
    val volumeNumber: Int,
    val pageNumber: Int,
    val note: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface KitabLibraryDao {

    @Query("SELECT * FROM kitab_metadata WHERE isAdminApproved = 1 ORDER BY categoryId ASC, seriesId ASC, volumeNumber ASC")
    fun observeAllApprovedCatalog(): Flow<List<KitabMetadataEntity>>

    @Query("SELECT * FROM kitab_metadata WHERE isAdminApproved = 1 ORDER BY categoryId ASC, seriesId ASC, volumeNumber ASC")
    suspend fun getAllApprovedCatalogOnce(): List<KitabMetadataEntity>

    @Query("SELECT * FROM kitab_metadata WHERE bookId = :bookId LIMIT 1")
    suspend fun getCatalogItemById(bookId: String): KitabMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCatalogItems(items: List<KitabMetadataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCatalogItem(item: KitabMetadataEntity)

    @Query("SELECT * FROM kitab_local_state")
    fun observeAllLocalStates(): Flow<List<KitabLocalStateEntity>>

    @Query("SELECT * FROM kitab_local_state")
    suspend fun getAllLocalStatesOnce(): List<KitabLocalStateEntity>

    @Query("SELECT * FROM kitab_local_state WHERE bookId = :bookId LIMIT 1")
    suspend fun getLocalStateById(bookId: String): KitabLocalStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalState(state: KitabLocalStateEntity)

    @Query("DELETE FROM kitab_local_state WHERE bookId = :bookId")
    suspend fun deleteLocalStateById(bookId: String)

    @Query(
        """
        UPDATE kitab_local_state 
        SET lastReadPage = :pageNumber, 
            totalPageCount = CASE WHEN :totalPages > 0 THEN :totalPages ELSE totalPageCount END,
            lastReadTimestampMillis = :timestampMillis 
        WHERE bookId = :bookId
        """
    )
    suspend fun updateLastReadPage(
        bookId: String,
        pageNumber: Int,
        totalPages: Int,
        timestampMillis: Long
    )

    @Query("SELECT * FROM kitab_bookmarks ORDER BY createdAtMillis DESC")
    fun observeAllBookmarks(): Flow<List<KitabBookmarkEntity>>

    @Query("SELECT * FROM kitab_bookmarks ORDER BY createdAtMillis DESC")
    suspend fun getAllBookmarksOnce(): List<KitabBookmarkEntity>

    @Query("SELECT * FROM kitab_bookmarks WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun observeBookmarksForBook(bookId: String): Flow<List<KitabBookmarkEntity>>

    @Query("SELECT * FROM kitab_bookmarks WHERE bookId = :bookId ORDER BY pageNumber ASC")
    suspend fun getBookmarksForBookOnce(bookId: String): List<KitabBookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: KitabBookmarkEntity)

    @Query("DELETE FROM kitab_bookmarks WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteBookmarkForPage(bookId: String, pageNumber: Int)

    @Query("DELETE FROM kitab_bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)
}

@Database(
    entities = [
        KitabMetadataEntity::class,
        KitabLocalStateEntity::class,
        KitabBookmarkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KitabLibraryDatabase : RoomDatabase() {
    abstract fun kitabLibraryDao(): KitabLibraryDao

    companion object {
        @Volatile
        private var INSTANCE: KitabLibraryDatabase? = null

        fun getInstance(context: Context): KitabLibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KitabLibraryDatabase::class.java,
                    "qawmi_kitab_library_room_v1.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun overrideInstanceForTesting(db: KitabLibraryDatabase) {
            INSTANCE = db
        }
    }
}
