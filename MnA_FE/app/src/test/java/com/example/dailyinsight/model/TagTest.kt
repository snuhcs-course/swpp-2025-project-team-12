package com.example.dailyinsight.model

import org.junit.Assert.*
import org.junit.Test

class TagTest {

    @Test
    fun tag_hasCorrectKoreanNames() {
        // Test all tags have correct Korean names
        assertEquals("IT서비스", Tag.IT_SERVICES.korean)
        assertEquals("건설", Tag.CONSTRUCTION.korean)
        assertEquals("금속", Tag.METALS.korean)
        assertEquals("기계.장비", Tag.MACHINERY_EQUIPMENT.korean)
        assertEquals("기타금융", Tag.OTHER_FINANCIAL_COMPANIES.korean)
        assertEquals("기타제조", Tag.OTHER_MANUFACTURING.korean)
        assertEquals("농업, 임업 및 어업", Tag.AGRICULTURE_FORESTRY_FISHING.korean)
        assertEquals("보험", Tag.INSURANCE.korean)
        assertEquals("부동산", Tag.REAL_ESTATE.korean)
        assertEquals("비금속", Tag.NON_METALS.korean)
        assertEquals("섬유.의류", Tag.TEXTILES_APPAREL.korean)
        assertEquals("오락.문화", Tag.ENTERTAINMENT_CULTURE.korean)
        assertEquals("운송.창고", Tag.TRANSPORTATION_WAREHOUSING.korean)
        assertEquals("운송장비.부품", Tag.TRANSPORTATION_EQUIPMENT_COMPONENTS.korean)
        assertEquals("유통", Tag.DISTRIBUTION.korean)
        assertEquals("은행", Tag.BANK.korean)
        assertEquals("음식료.담배", Tag.FOODS_BEVERAGES_TOBACCO.korean)
        assertEquals("의료.정밀기기", Tag.MEDICAL_PRECISION_INSTRUMENTS.korean)
        assertEquals("일반서비스", Tag.GENERAL_SERVICES.korean)
        assertEquals("전기.가스", Tag.ELECTRICITY_GAS.korean)
        assertEquals("전기.전자", Tag.ELECTRICAL_EQUIPMENT_ELECTRONICS.korean)
        assertEquals("제약", Tag.PHARMACEUTICALS.korean)
        assertEquals("종이.목재", Tag.PAPER_FOREST_PRODUCTS.korean)
        assertEquals("증권", Tag.SECURITIES.korean)
        assertEquals("통신", Tag.TELECOMMUNICATIONS.korean)
        assertEquals("화학", Tag.CHEMICALS.korean)
    }

    @Test
    fun tag_allValuesAccessible() {
        // Test all enum values are accessible
        val allTags = Tag.values()
        assertEquals(26, allTags.size)
    }

    @Test
    fun tag_valueOfWorks() {
        // Test valueOf works correctly
        assertEquals(Tag.IT_SERVICES, Tag.valueOf("IT_SERVICES"))
        assertEquals(Tag.BANK, Tag.valueOf("BANK"))
        assertEquals(Tag.CHEMICALS, Tag.valueOf("CHEMICALS"))
    }
}