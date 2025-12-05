package com.example.dailyinsight.model

import org.junit.Assert.*
import org.junit.Test

class TagEnumTest {

    @Test
    fun tag_allValuesExist() {
        assertEquals(26, Tag.entries.size)
    }

    @Test
    fun tag_IT_SERVICES_hasCorrectKorean() {
        assertEquals("IT서비스", Tag.IT_SERVICES.korean)
    }

    @Test
    fun tag_CONSTRUCTION_hasCorrectKorean() {
        assertEquals("건설", Tag.CONSTRUCTION.korean)
    }

    @Test
    fun tag_METALS_hasCorrectKorean() {
        assertEquals("금속", Tag.METALS.korean)
    }

    @Test
    fun tag_BANK_hasCorrectKorean() {
        assertEquals("은행", Tag.BANK.korean)
    }

    @Test
    fun tag_PHARMACEUTICALS_hasCorrectKorean() {
        assertEquals("제약", Tag.PHARMACEUTICALS.korean)
    }

    @Test
    fun tag_CHEMICALS_hasCorrectKorean() {
        assertEquals("화학", Tag.CHEMICALS.korean)
    }

    @Test
    fun tag_valueOf_returnsCorrectEnum() {
        assertEquals(Tag.IT_SERVICES, Tag.valueOf("IT_SERVICES"))
        assertEquals(Tag.BANK, Tag.valueOf("BANK"))
        assertEquals(Tag.CHEMICALS, Tag.valueOf("CHEMICALS"))
    }

    @Test
    fun tag_name_returnsEnumName() {
        assertEquals("IT_SERVICES", Tag.IT_SERVICES.name)
        assertEquals("BANK", Tag.BANK.name)
        assertEquals("CONSTRUCTION", Tag.CONSTRUCTION.name)
    }

    @Test
    fun tag_ordinal_isConsistent() {
        val entries = Tag.entries
        for (i in entries.indices) {
            assertEquals(i, entries[i].ordinal)
        }
    }

    @Test
    fun tag_allEntriesHaveNonEmptyKorean() {
        Tag.entries.forEach { tag ->
            assertTrue("Tag ${tag.name} should have non-empty korean", tag.korean.isNotEmpty())
        }
    }

    @Test
    fun tag_koreanValuesAreUnique() {
        val koreanValues = Tag.entries.map { it.korean }
        assertEquals(koreanValues.size, koreanValues.distinct().size)
    }

    @Test
    fun tag_MACHINERY_EQUIPMENT_hasCorrectKorean() {
        assertEquals("기계.장비", Tag.MACHINERY_EQUIPMENT.korean)
    }

    @Test
    fun tag_OTHER_FINANCIAL_COMPANIES_hasCorrectKorean() {
        assertEquals("기타금융", Tag.OTHER_FINANCIAL_COMPANIES.korean)
    }

    @Test
    fun tag_OTHER_MANUFACTURING_hasCorrectKorean() {
        assertEquals("기타제조", Tag.OTHER_MANUFACTURING.korean)
    }

    @Test
    fun tag_AGRICULTURE_FORESTRY_FISHING_hasCorrectKorean() {
        assertEquals("농업, 임업 및 어업", Tag.AGRICULTURE_FORESTRY_FISHING.korean)
    }

    @Test
    fun tag_INSURANCE_hasCorrectKorean() {
        assertEquals("보험", Tag.INSURANCE.korean)
    }

    @Test
    fun tag_REAL_ESTATE_hasCorrectKorean() {
        assertEquals("부동산", Tag.REAL_ESTATE.korean)
    }

    @Test
    fun tag_NON_METALS_hasCorrectKorean() {
        assertEquals("비금속", Tag.NON_METALS.korean)
    }

    @Test
    fun tag_TEXTILES_APPAREL_hasCorrectKorean() {
        assertEquals("섬유.의류", Tag.TEXTILES_APPAREL.korean)
    }

    @Test
    fun tag_ENTERTAINMENT_CULTURE_hasCorrectKorean() {
        assertEquals("오락.문화", Tag.ENTERTAINMENT_CULTURE.korean)
    }

    @Test
    fun tag_TRANSPORTATION_WAREHOUSING_hasCorrectKorean() {
        assertEquals("운송.창고", Tag.TRANSPORTATION_WAREHOUSING.korean)
    }

    @Test
    fun tag_TRANSPORTATION_EQUIPMENT_COMPONENTS_hasCorrectKorean() {
        assertEquals("운송장비.부품", Tag.TRANSPORTATION_EQUIPMENT_COMPONENTS.korean)
    }

    @Test
    fun tag_DISTRIBUTION_hasCorrectKorean() {
        assertEquals("유통", Tag.DISTRIBUTION.korean)
    }

    @Test
    fun tag_FOODS_BEVERAGES_TOBACCO_hasCorrectKorean() {
        assertEquals("음식료.담배", Tag.FOODS_BEVERAGES_TOBACCO.korean)
    }

    @Test
    fun tag_MEDICAL_PRECISION_INSTRUMENTS_hasCorrectKorean() {
        assertEquals("의료.정밀기기", Tag.MEDICAL_PRECISION_INSTRUMENTS.korean)
    }

    @Test
    fun tag_GENERAL_SERVICES_hasCorrectKorean() {
        assertEquals("일반서비스", Tag.GENERAL_SERVICES.korean)
    }

    @Test
    fun tag_ELECTRICITY_GAS_hasCorrectKorean() {
        assertEquals("전기.가스", Tag.ELECTRICITY_GAS.korean)
    }

    @Test
    fun tag_ELECTRICAL_EQUIPMENT_ELECTRONICS_hasCorrectKorean() {
        assertEquals("전기.전자", Tag.ELECTRICAL_EQUIPMENT_ELECTRONICS.korean)
    }

    @Test
    fun tag_PAPER_FOREST_PRODUCTS_hasCorrectKorean() {
        assertEquals("종이.목재", Tag.PAPER_FOREST_PRODUCTS.korean)
    }

    @Test
    fun tag_SECURITIES_hasCorrectKorean() {
        assertEquals("증권", Tag.SECURITIES.korean)
    }

    @Test
    fun tag_TELECOMMUNICATIONS_hasCorrectKorean() {
        assertEquals("통신", Tag.TELECOMMUNICATIONS.korean)
    }

    @Test
    fun tag_findByKorean_works() {
        val found = Tag.entries.find { it.korean == "은행" }
        assertEquals(Tag.BANK, found)
    }

    @Test
    fun tag_filterByCategory_works() {
        val financialTags = Tag.entries.filter {
            it == Tag.BANK || it == Tag.SECURITIES || it == Tag.INSURANCE || it == Tag.OTHER_FINANCIAL_COMPANIES
        }
        assertEquals(4, financialTags.size)
    }
}
