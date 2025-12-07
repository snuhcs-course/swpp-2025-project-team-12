package com.example.dailyinsight.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BriefingDao using Room in-memory database with Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BriefingDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: BriefingDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.briefingDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createCard(
        ticker: String,
        name: String = "Company",
        rank: Int? = 0,
        isFavorite: Boolean = false,
        marketCap: Long = 1000000L
    ) = BriefingCardCache(
        ticker = ticker,
        name = name,
        price = 100L,
        change = 5L,
        changeRate = 2.5,
        headline = "Headline",
        label = null,
        confidence = null,
        rank = rank,
        fetchedAt = System.currentTimeMillis(),
        marketCap = marketCap,
        industry = "Tech",
        isFavorite = isFavorite
    )

    // ===== insertCards & getAllCards Tests =====

    @Test
    fun insertCards_insertsSuccessfully() = runTest {
        val cards = listOf(createCard("AAPL"), createCard("GOOGL"))

        dao.insertCards(cards)

        val result = dao.getAllCards().first()
        assertEquals(2, result.size)
    }

    @Test
    fun getAllCards_orderedByFetchedAt() = runTest {
        val card1 = createCard("AAPL").copy(fetchedAt = 1000L)
        val card2 = createCard("GOOGL").copy(fetchedAt = 2000L)
        dao.insertCards(listOf(card2, card1))

        val result = dao.getAllCards().first()

        assertEquals("AAPL", result[0].ticker)
        assertEquals("GOOGL", result[1].ticker)
    }

    // ===== Favorite Operations Tests =====

    @Test
    fun insertFavorite_addsFavorite() = runTest {
        val favorite = FavoriteTicker("AAPL", "testuser")

        dao.insertFavorite(favorite)

        val tickers = dao.getFavoriteTickers("testuser")
        assertTrue(tickers.contains("AAPL"))
    }

    @Test
    fun deleteFavorite_removesFavorite() = runTest {
        dao.insertFavorite(FavoriteTicker("AAPL", "testuser"))
        dao.insertFavorite(FavoriteTicker("GOOGL", "testuser"))

        dao.deleteFavorite("AAPL", "testuser")

        val tickers = dao.getFavoriteTickers("testuser")
        assertFalse(tickers.contains("AAPL"))
        assertTrue(tickers.contains("GOOGL"))
    }

    @Test
    fun deleteFavorite_onlyDeletesForSpecificUser() = runTest {
        dao.insertFavorite(FavoriteTicker("AAPL", "user1"))
        dao.insertFavorite(FavoriteTicker("AAPL", "user2"))

        dao.deleteFavorite("AAPL", "user1")

        assertTrue(dao.getFavoriteTickers("user2").contains("AAPL"))
        assertFalse(dao.getFavoriteTickers("user1").contains("AAPL"))
    }

    @Test
    fun clearFavoritesForUser_clearsOnlyUserFavorites() = runTest {
        dao.insertFavorite(FavoriteTicker("AAPL", "user1"))
        dao.insertFavorite(FavoriteTicker("GOOGL", "user1"))
        dao.insertFavorite(FavoriteTicker("MSFT", "user2"))

        dao.clearFavoritesForUser("user1")

        assertTrue(dao.getFavoriteTickers("user1").isEmpty())
        assertEquals(1, dao.getFavoriteTickers("user2").size)
    }

    @Test
    fun getAllFavoriteTickers_returnsAllTickers() = runTest {
        dao.insertFavorite(FavoriteTicker("AAPL", "user1"))
        dao.insertFavorite(FavoriteTicker("GOOGL", "user2"))

        val all = dao.getAllFavoriteTickers()

        assertEquals(2, all.size)
        assertTrue(all.contains("AAPL"))
        assertTrue(all.contains("GOOGL"))
    }

    // ===== syncFavorites Tests =====

    @Test
    fun syncFavorites_updatesFavoriteStatus() = runTest {
        dao.insertCards(listOf(createCard("AAPL"), createCard("GOOGL")))
        dao.insertFavorite(FavoriteTicker("AAPL", "testuser"))

        dao.syncFavorites("testuser")

        val result = dao.getAllCards().first()
        val aapl = result.find { it.ticker == "AAPL" }
        val googl = result.find { it.ticker == "GOOGL" }
        assertTrue(aapl?.isFavorite == true)
        assertFalse(googl?.isFavorite == true)
    }

    // ===== updateFavorite Tests =====

    @Test
    fun updateFavorite_setsTrue() = runTest {
        dao.insertCards(listOf(createCard("AAPL", isFavorite = false)))

        dao.updateFavorite("AAPL", true)

        val card = dao.getCard("AAPL")
        assertTrue(card?.isFavorite == true)
    }

    @Test
    fun updateFavorite_setsFalse() = runTest {
        dao.insertCards(listOf(createCard("AAPL", isFavorite = true)))

        dao.updateFavorite("AAPL", false)

        val card = dao.getCard("AAPL")
        assertFalse(card?.isFavorite == true)
    }

    // ===== clearAllFavorites Tests =====

    @Test
    fun clearAllFavorites_removesAllFavorites() = runTest {
        dao.insertFavorite(FavoriteTicker("AAPL", "user1"))
        dao.insertFavorite(FavoriteTicker("GOOGL", "user2"))

        dao.clearAllFavorites()

        assertTrue(dao.getAllFavoriteTickers().isEmpty())
    }

    // ===== insertFavorites (batch) Tests =====

    @Test
    fun insertFavorites_insertsBatch() = runTest {
        val favorites = listOf(
            FavoriteTicker("AAPL", "testuser"),
            FavoriteTicker("GOOGL", "testuser"),
            FavoriteTicker("MSFT", "testuser")
        )

        dao.insertFavorites(favorites)

        assertEquals(3, dao.getFavoriteTickers("testuser").size)
    }

    // ===== getCard Tests =====

    @Test
    fun getCard_returnsCard() = runTest {
        dao.insertCards(listOf(createCard("AAPL", "Apple Inc")))

        val card = dao.getCard("AAPL")

        assertNotNull(card)
        assertEquals("Apple Inc", card?.name)
    }

    @Test
    fun getCard_nonExistent_returnsNull() = runTest {
        val card = dao.getCard("NONEXISTENT")

        assertNull(card)
    }

    // ===== replaceFavorites Tests =====

    @Test
    fun replaceFavorites_clearsAndInserts() = runTest {
        dao.insertFavorite(FavoriteTicker("OLD", "user"))

        val newFavorites = listOf(
            FavoriteTicker("NEW1", "user"),
            FavoriteTicker("NEW2", "user")
        )
        dao.replaceFavorites(newFavorites)

        val all = dao.getAllFavoriteTickers()
        assertFalse(all.contains("OLD"))
        assertTrue(all.contains("NEW1"))
        assertTrue(all.contains("NEW2"))
    }

    // ===== uncheckAllFavorites Tests =====

    @Test
    fun uncheckAllFavorites_setsAllToFalse() = runTest {
        dao.insertCards(listOf(
            createCard("AAPL", isFavorite = true),
            createCard("GOOGL", isFavorite = true)
        ))

        dao.uncheckAllFavorites()

        val cards = dao.getAllCards().first()
        assertTrue(cards.all { !it.isFavorite })
    }

    // ===== deleteNonFavorites Tests =====

    @Test
    fun deleteNonFavorites_keepsOnlyFavorited() = runTest {
        dao.insertCards(listOf(createCard("AAPL"), createCard("GOOGL")))
        dao.insertFavorite(FavoriteTicker("AAPL", "user"))

        dao.deleteNonFavorites()

        val cards = dao.getAllCards().first()
        assertEquals(1, cards.size)
        assertEquals("AAPL", cards[0].ticker)
    }

    // ===== getNormalListFlow Tests =====

    @Test
    fun getNormalListFlow_returnsOnlyRanked() = runTest {
        dao.insertCards(listOf(
            createCard("AAPL", rank = 1),
            createCard("GOOGL", rank = null),
            createCard("MSFT", rank = 2)
        ))

        val result = dao.getNormalListFlow().first()

        assertEquals(2, result.size)
        assertEquals("AAPL", result[0].ticker)
        assertEquals("MSFT", result[1].ticker)
    }

    // ===== getFavoriteListFlow Tests =====

    @Test
    fun getFavoriteListFlow_returnsFavoritesOrderedByMarketCap() = runTest {
        dao.insertCards(listOf(
            createCard("AAPL", isFavorite = true, marketCap = 1000L),
            createCard("GOOGL", isFavorite = true, marketCap = 3000L),
            createCard("MSFT", isFavorite = false, marketCap = 2000L)
        ))

        val result = dao.getFavoriteListFlow().first()

        assertEquals(2, result.size)
        assertEquals("GOOGL", result[0].ticker) // Higher marketCap first
        assertEquals("AAPL", result[1].ticker)
    }

    // ===== resetRanks Tests =====

    @Test
    fun resetRanks_setsAllRanksToNull() = runTest {
        dao.insertCards(listOf(
            createCard("AAPL", rank = 1),
            createCard("GOOGL", rank = 2)
        ))

        dao.resetRanks()

        val normalList = dao.getNormalListFlow().first()
        assertTrue(normalList.isEmpty())
    }

    // ===== deleteGarbage Tests =====

    @Test
    fun deleteGarbage_removesUnrankedNonFavorites() = runTest {
        dao.insertCards(listOf(
            createCard("AAPL", rank = null, isFavorite = false),
            createCard("GOOGL", rank = null, isFavorite = true),
            createCard("MSFT", rank = 1, isFavorite = false)
        ))

        dao.deleteGarbage()

        val all = dao.getAllCards().first()
        assertEquals(2, all.size)
        assertNull(all.find { it.ticker == "AAPL" })
    }

    // ===== clearAll Tests =====

    @Test
    fun clearAll_removesAllCards() = runTest {
        dao.insertCards(listOf(createCard("AAPL"), createCard("GOOGL")))

        dao.clearAll()

        val all = dao.getAllCards().first()
        assertTrue(all.isEmpty())
    }
}
