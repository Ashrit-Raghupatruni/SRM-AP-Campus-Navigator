package com.srmap.campusnavigator.search

import com.srmap.campusnavigator.data.datasource.StaticCampusDataSource
import com.srmap.campusnavigator.data.local.dao.CampusDao
import com.srmap.campusnavigator.data.local.dao.RoomWithDetails
import com.srmap.campusnavigator.data.local.entities.*
import com.srmap.campusnavigator.data.repository.CampusRepositoryImpl
import com.srmap.campusnavigator.data.repository.SearchRepositoryImpl
import com.srmap.campusnavigator.presentation.components.SearchCategoryFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeCampusDao : CampusDao {
    override fun getAllLocations(): Flow<List<LocationEntity>> = flowOf(emptyList())
    override fun getFavoriteLocations(): Flow<List<LocationEntity>> = flowOf(emptyList())
    override suspend fun searchLocations(query: String): List<LocationEntity> = emptyList()
    override suspend fun getLocationById(id: String): LocationEntity? = null
    override suspend fun insertLocations(locations: List<LocationEntity>) {}
    override suspend fun updateFavorite(id: String, isFav: Boolean) {}
    override suspend fun insertRecentSearch(search: RecentSearchEntity) {}
    override suspend fun getRecentSearches(): List<String> = emptyList()
    override suspend fun clearRecentSearches() {}
    override suspend fun insertBlocks(blocks: List<BlockEntity>) {}
    override suspend fun insertRooms(rooms: List<RoomEntity>) {}
    override suspend fun insertFaculty(facultyList: List<FacultyEntity>) {}
    override suspend fun insertEquipment(equipmentList: List<EquipmentEntity>) {}
    override suspend fun getRoomsByBlockAndFloor(blockId: String, floor: Int): List<RoomEntity> = emptyList()
    override suspend fun getRoomsByBlock(blockId: String): List<RoomEntity> = emptyList()
    override suspend fun searchCampusRoomsUnified(query: String): List<RoomWithDetails> = emptyList()
    override suspend fun getRoomCount(): Int = 0
    override suspend fun getRoomsForBlock(blockId: String): List<RoomEntity> = emptyList()
    override suspend fun getFacultyForBlock(blockId: String): List<FacultyEntity> = emptyList()
    override suspend fun getEquipmentForBlock(blockId: String): List<EquipmentEntity> = emptyList()
    override suspend fun getAllFaculty(): List<FacultyEntity> = emptyList()
    override suspend fun getAllEquipment(): List<EquipmentEntity> = emptyList()
}

class SearchAndRepositoryTest {

    private lateinit var staticSource: StaticCampusDataSource
    private lateinit var searchRepository: SearchRepositoryImpl

    @Before
    fun setUp() {
        staticSource = StaticCampusDataSource
        val fakeDao = FakeCampusDao()
        val campusRepo = CampusRepositoryImpl(fakeDao, geoJsonParser = null, staticSource = staticSource)
        searchRepository = SearchRepositoryImpl(campusRepo, fakeDao)
    }

    @Test
    fun testStaticCampusDataSourceIntegrity() {
        val blocks = staticSource.getBlocks()
        val rooms = staticSource.getRooms()
        val faculty = staticSource.getFaculty()
        val equipment = staticSource.getEquipment()

        assertTrue("Static blocks should not be empty", blocks.isNotEmpty())
        assertTrue("Static rooms should cover multiple floors", rooms.size > 20)
        assertTrue("Faculty directory should contain entries", faculty.isNotEmpty())
        assertTrue("Equipment catalog should contain entries", equipment.isNotEmpty())

        val cvRooms = staticSource.getRoomsByBlock("CV")
        val cvFloors = cvRooms.map { it.floor }.distinct().sorted()
        assertEquals("CV Raman Block should have 12 floors", 12, cvFloors.size)
    }

    @Test
    fun testSearchExactMatchPrioritization() = runBlocking {
        val results = searchRepository.searchCampus("S-201", SearchCategoryFilter.ALL)
        assertTrue("Search for S-201 should return matches", results.isNotEmpty())
        assertEquals("Exact room match should be ranked first", "SR - S-201 (Lecture Hall)", results.first().title)
    }

    @Test
    fun testSearchCategoryFiltering() = runBlocking {
        val facultyOnly = searchRepository.searchCampus("Kumar", SearchCategoryFilter.FACULTY)
        assertTrue("Faculty filter should return faculty records", facultyOnly.isNotEmpty())
        assertTrue("Result should be faculty type", facultyOnly.first() is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.FacultyResult)

        val equipmentOnly = searchRepository.searchCampus("Microscope", SearchCategoryFilter.EQUIPMENT)
        assertTrue("Equipment filter should return equipment records", equipmentOnly.isNotEmpty())
        assertTrue("Result should be equipment type", equipmentOnly.first() is com.srmap.campusnavigator.domain.models.UnifiedSearchResult.EquipmentResult)
    }

    @Test
    fun testSearchDeduplication() = runBlocking {
        val allResults = searchRepository.searchCampus("", SearchCategoryFilter.ALL)
        val titles = allResults.map { it.title + it.subtitle }
        val uniqueTitles = titles.distinct()
        assertEquals("Search results should contain zero duplicates", uniqueTitles.size, titles.size)
    }
}
