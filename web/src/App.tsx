import React, { useState, useEffect, useMemo } from 'react';
import { Search, Compass, Box, Map as MapIcon, Sun, Moon } from 'lucide-react';
import { CampusMap2D } from './components/map/CampusMap2D';
import { CampusMap3D } from './components/map3d/CampusMap3D';
import { FloorSelector } from './components/map/FloorSelector';
import { TimeControlBar } from './components/solar/TimeControlBar';
import { SearchModal } from './components/directory/SearchModal';
import { NavigationSheet } from './components/navigation/NavigationSheet';
import { BlockInspectionSheet } from './components/directory/BlockInspectionSheet';
import { getSolarTimes, CAMPUS_LAT, CAMPUS_LNG } from './core/solar';
import { PedestrianGraph } from './core/routing';
import { CampusSearchIndex, SearchResultItem } from './core/search';
import { RouteResult } from './types/campus';

export function App() {
  // GeoJSON Data states
  const [buildingsData, setBuildingsData] = useState<any>(null);
  const [pathsData, setPathsData] = useState<any>(null);
  const [roadsData, setRoadsData] = useState<any>(null);
  const [groundsData, setGroundsData] = useState<any>(null);
  const [sportsData, setSportsData] = useState<any>(null);
  const [roomsData, setRoomsData] = useState<any>(null);
  const [facultyData, setFacultyData] = useState<any[]>([]);

  // Navigation & View Mode
  const [viewMode, setViewMode] = useState<'2D' | '3D'>('2D');
  const [selectedFloor, setSelectedFloor] = useState<number | null>(null);
  const [isSearchOpen, setIsSearchOpen] = useState(false);

  // Active navigation & selected building
  const [activeRoute, setActiveRoute] = useState<RouteResult | null>(null);
  const [navigationTargetName, setNavigationTargetName] = useState<string>('');
  const [selectedBuilding, setSelectedBuilding] = useState<{ name: string; feature: any } | null>(null);

  // Solar & Local Time Engine
  const [simulatedTime, setSimulatedTime] = useState<Date>(new Date());
  const [isLiveTime, setIsLiveTime] = useState<boolean>(true);

  // A* Routing Graph & Search Index
  const routingGraph = useMemo(() => new PedestrianGraph(), []);
  const searchIndex = useMemo(() => new CampusSearchIndex(), []);

  // Update clock every second when in live sync mode
  useEffect(() => {
    if (!isLiveTime) return;
    const interval = setInterval(() => {
      setSimulatedTime(new Date());
    }, 1000);
    return () => clearInterval(interval);
  }, [isLiveTime]);

  const solarInfo = useMemo(() => getSolarTimes(simulatedTime), [simulatedTime]);

  // Sync dark class to root HTML element for Tailwind dark mode
  useEffect(() => {
    if (solarInfo.isNight) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [solarInfo.isNight]);

  // Load campus GIS & directory data on startup
  useEffect(() => {
    async function loadCampusAssets() {
      try {
        const [bRes, pRes, rRes, gRes, sRes, rmRes, facRes] = await Promise.all([
          fetch('/data/buildings.geojson').then((r) => r.json()).catch(() => null),
          fetch('/data/paths.geojson').then((r) => r.json()).catch(() => null),
          fetch('/data/roads.geojson').then((r) => r.json()).catch(() => null),
          fetch('/data/grounds.geojson').then((r) => r.json()).catch(() => null),
          fetch('/data/sports.geojson').then((r) => r.json()).catch(() => null),
          fetch('/data/srmap_rooms_all.geojson').then((r) => r.json()).catch(() => null),
          fetch('/data/srmap_faculty_directory_verified.json').then((r) => r.json()).catch(() => []),
        ]);

        setBuildingsData(bRes);
        setPathsData(pRes);
        setRoadsData(rRes);
        setGroundsData(gRes);
        setSportsData(sRes);
        setRoomsData(rmRes);
        setFacultyData(facRes);

        // Populate routing graph
        if (pRes) {
          routingGraph.loadFromGeoJSON(pRes, rRes);
        }

        // Populate search index
        searchIndex.loadData(rmRes, facRes);
      } catch (err) {
        console.error('Failed to load campus GIS assets:', err);
      }
    }

    loadCampusAssets();
  }, [routingGraph, searchIndex]);

  // Trigger pedestrian navigation towards a target coordinate
  const navigateToLocation = (targetLat: number, targetLng: number, targetName: string) => {
    // Campus entrance / main gate as default origin
    const originLat = 16.5645;
    const originLng = 80.4985;

    const route = routingGraph.findRoute(originLat, originLng, targetLat, targetLng);
    if (route) {
      setActiveRoute(route);
      setNavigationTargetName(targetName);
    }
  };

  // Rooms belonging to selected building
  const buildingRooms = useMemo(() => {
    if (!selectedBuilding || !roomsData || !roomsData.features) return [];
    const bName = selectedBuilding.name.toLowerCase();
    return roomsData.features.filter((f: any) => {
      const b = (f.properties?.block || f.properties?.building || '').toLowerCase();
      return b.includes(bName) || bName.includes(b);
    });
  }, [selectedBuilding, roomsData]);

  return (
    <div className="relative w-screen h-screen overflow-hidden font-sans select-none bg-slate-100 dark:bg-slate-950">
      {/* 2D or 3D Map Viewport */}
      <div className="absolute inset-0">
        {viewMode === '2D' ? (
          <CampusMap2D
            buildingsGeoJSON={buildingsData}
            pathsGeoJSON={pathsData}
            roadsGeoJSON={roadsData}
            groundsGeoJSON={groundsData}
            facilitiesGeoJSON={null}
            sportsGeoJSON={sportsData}
            roomsGeoJSON={roomsData}
            selectedFloor={selectedFloor}
            activeRoute={activeRoute}
            isNight={solarInfo.isNight}
            onSelectBuilding={(name, feature) => setSelectedBuilding({ name, feature })}
            onSelectRoom={(room) => navigateToLocation(room.lat, room.lng, room.room_number || room.room_id)}
          />
        ) : (
          <CampusMap3D
            buildingsGeoJSON={buildingsData}
            pathsGeoJSON={pathsData}
            roadsGeoJSON={roadsData}
            solarInfo={solarInfo}
            activeRoute={activeRoute}
            cameraMode="orbit"
          />
        )}
      </div>

      {/* Top Floating App Bar */}
      <div className="absolute top-4 left-4 right-4 z-40 flex items-center justify-between pointer-events-none gap-3">
        {/* Brand & Search Trigger */}
        <div className="flex items-center gap-2 pointer-events-auto">
          <div className="flex items-center gap-2.5 bg-srm-navy text-white px-3.5 py-2.5 rounded-2xl shadow-xl border border-white/10">
            <Compass className="w-5 h-5 text-teal-400 animate-pulse" />
            <div className="hidden sm:block">
              <div className="font-extrabold text-xs tracking-wide">SRM UNIVERSITY-AP</div>
              <div className="text-[10px] text-teal-300 font-medium">Campus Navigator</div>
            </div>
          </div>

          <button
            onClick={() => setIsSearchOpen(true)}
            className="flex items-center gap-2.5 bg-white/90 dark:bg-slate-900/90 backdrop-blur-md px-4 py-2.5 rounded-2xl shadow-lg border border-slate-200/80 dark:border-slate-800 text-slate-500 hover:text-slate-800 dark:hover:text-slate-100 transition-all cursor-pointer group"
          >
            <Search className="w-4 h-4 text-teal-600 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-semibold hidden md:inline">Search rooms, faculty cabins, labs...</span>
            <span className="text-xs font-semibold md:hidden">Search...</span>
            <kbd className="hidden sm:inline text-[10px] bg-slate-100 dark:bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded font-mono">
              /
            </kbd>
          </button>
        </div>

        {/* 2D / 3D Mode Switcher */}
        <div className="flex items-center bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-lg p-1 pointer-events-auto gap-1">
          <button
            onClick={() => setViewMode('2D')}
            className={`flex items-center gap-1.5 text-xs font-bold px-3 py-1.5 rounded-xl transition-all cursor-pointer ${
              viewMode === '2D'
                ? 'bg-srm-navy text-white shadow-sm'
                : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            <MapIcon className="w-3.5 h-3.5" />
            <span>2D</span>
          </button>

          <button
            onClick={() => setViewMode('3D')}
            className={`flex items-center gap-1.5 text-xs font-bold px-3 py-1.5 rounded-xl transition-all cursor-pointer ${
              viewMode === '3D'
                ? 'bg-teal-600 text-white shadow-sm'
                : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            <Box className="w-3.5 h-3.5" />
            <span>3D World</span>
          </button>
        </div>
      </div>

      {/* Right Floating Floor Selector & View Controls */}
      {viewMode === '2D' && (
        <div className="absolute right-4 top-20 z-50 pointer-events-auto">
          <FloorSelector selectedFloor={selectedFloor} onSelectFloor={setSelectedFloor} />
        </div>
      )}

      {/* Bottom Floating Control Bar (Solar Day/Night Cycle + Time) */}
      <div className="absolute bottom-4 left-4 right-4 sm:left-6 sm:w-96 z-50 pointer-events-auto">
        <TimeControlBar
          solarInfo={solarInfo}
          simulatedTime={simulatedTime}
          isLiveTime={isLiveTime}
          onTimeChange={(t) => {
            setIsLiveTime(false);
            setSimulatedTime(t);
          }}
          onToggleLive={() => {
            setIsLiveTime(true);
            setSimulatedTime(new Date());
          }}
        />
      </div>

      {/* Navigation Turn-by-Turn Card */}
      {activeRoute && (
        <div className="absolute bottom-24 sm:bottom-4 right-4 z-50 pointer-events-auto">
          <NavigationSheet
            route={activeRoute}
            destinationTitle={navigationTargetName}
            onCancel={() => setActiveRoute(null)}
          />
        </div>
      )}

      {/* Building Inspection Sheet */}
      {selectedBuilding && (
        <div className="absolute bottom-24 sm:bottom-4 right-4 z-50 pointer-events-auto">
          <BlockInspectionSheet
            buildingName={selectedBuilding.name}
            properties={selectedBuilding.feature.properties}
            rooms={buildingRooms}
            onClose={() => setSelectedBuilding(null)}
            onNavigateToRoom={(r) => navigateToLocation(r.lat, r.lng, r.room_number || r.room_id)}
          />
        </div>
      )}

      {/* Universal Search Modal */}
      <SearchModal
        isOpen={isSearchOpen}
        onClose={() => setIsSearchOpen(false)}
        onSearch={(q, cat) => searchIndex.search(q, cat)}
        onSelectItem={(item) => navigateToLocation(item.lat, item.lng, item.title)}
        onNavigateTo={(item) => navigateToLocation(item.lat, item.lng, item.title)}
      />
    </div>
  );
}

export default App;
