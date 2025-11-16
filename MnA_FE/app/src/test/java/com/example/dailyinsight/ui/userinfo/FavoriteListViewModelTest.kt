package com.example.dailyinsight.ui.userinfo

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FavoriteListViewModelTest {

    private lateinit var viewModel: FavoriteListViewModel

    @Before
    fun setup() {
        viewModel = FavoriteListViewModel()
    }

    @Test
    fun viewModel_canBeInstantiated() {
        // When/Then
        assertNotNull(viewModel)
    }

    @Test
    fun viewModel_isInstanceOfCorrectClass() {
        // When/Then
        assertTrue(viewModel is FavoriteListViewModel)
    }

    @Test
    fun viewModel_multipleInstances_areIndependent() {
        // Given
        val viewModel1 = FavoriteListViewModel()
        val viewModel2 = FavoriteListViewModel()

        // When/Then
        assertNotEquals(viewModel1, viewModel2)
    }
}