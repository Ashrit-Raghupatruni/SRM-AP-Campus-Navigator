import React, { useState, useEffect, useMemo } from 'react';
import {
  Search,
  Compass,
  Box,
  Map as MapIcon,
  Navigation,
  Lock,
  Unlock,
  RotateCcw,
  Footprints,
  Info,
  Clock,
  Sparkles,
} from 'lucide-react';
import { CampusMap2D } from './components/map/CampusMap2D';
import { CampusMap3D, CampusLabel } from './components/map3d/CampusMap3D';
import { FloorSelector } from './components/map/FloorSelector';
import { TimeControlBar } from './components/solar/TimeControlBar';
import { SearchModal } from './components/directory/SearchModal';
import { NavigationSheet } from './components/navigation/NavigationSheet';
import { BlockInspectionSheet } from './components/directory/BlockInspectionSheet';
import { getSolarTimes, formatTime } from './core/solar';
import { PedestrianGraph } from './core/routing';
import { CampusSearchIndex } from './core/search';
import { RouteResult } from './types/campus';

export function App() {
  // GeoJSON Data
  const [buildingsData, setBuildingsData] = useState<any>(null);
  const [pathsData, setPathsData] = useState<any>(null);
  const [roadsData, setRoadsData] = useState<any>(null);
  const [groundsData, setGroundsData] = useState<any>(null);
  const [sportsData, setSportsData] = useState<any>(null);
  const [roomsData, setRoomsData] = useState<any>(null);
  const [facultyData, setFacultyData] = useState<any[]>([]);

  // Navigation & View Mode
  const [viewMode, setViewMode] = useState<'3D' | '2D'>('3D');
  const [selectedFloor, setSelectedFloor] = useState<number | null>(null);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [activeFilter, setActiveFilter] = useState<string>('All');
  const [isAngleLocked, setIsAngleLocked] = useState(false);
  const [isWalkMode, setIsWalkMode] = useState(false);
  const [isTimePickerOpen, setIsTimePickerOpen] = useState(false);

  // Projected 3D building labels
  const [campusLabels, setCampusLabels] = useState<CampusLabel[]>([]);

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

  // Update clock every second in live mode
  useEffect(() => {
    if (!isLiveTime) return;
    const interval = setInterval(() => {
      setSimulatedTime(new Date());
    }, 1000);
    return () => clearInterval(interval);
  }, [isLiveTime]);

  const solarInfo = useMemo(() => getSolarTimes(simulatedTime), [simulatedTime]);

  // Load campus GIS assets
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

        if (pRes) routingGraph.loadFromGeoJSON(pRes, rRes);
        searchIndex.loadData(rmRes, facRes);
      } catch (err) {
        console.error('Failed to load campus GIS assets:', err);
      }
    }
    loadCampusAssets();
  }, [routingGraph, searchIndex]);

  const navigateToLocation = (targetLat: number, targetLng: number, targetName: string) => {
    const originLat = 16.4635;
    const originLng = 80.5065;
    const route = routingGraph.findRoute(originLat, originLng, targetLat, targetLng);
    if (route) {
      setActiveRoute(route);
      setNavigationTargetName(targetName);
    }
  };

  const categories = [
    { name: 'All', color: '#f59e0b' },
    { name: 'Academic', color: '#38bdf8' },
    { name: 'Hostels', color: '#ec4899' },
    { name: 'Food', color: '#f97316' },
    { name: 'Shops', color: '#a855f7' },
    { name: 'Sports', color: '#10b981' },
    { name: 'Health', color: '#ef4444' },
    { name: 'Useful spots', color: '#06b6d4' },
  ];

  return (
    <div className="relative w-screen h-screen overflow-hidden font-sans select-none bg-[#040810] text-slate-100">
      {/* 2D or 3D Map Viewport */}
      <div className="absolute inset-0">
        {viewMode === '3D' ? (
          <CampusMap3D
            buildingsGeoJSON={buildingsData}
            pathsGeoJSON={pathsData}
            roadsGeoJSON={roadsData}
            groundsGeoJSON={groundsData}
            sportsGeoJSON={sportsData}
            solarInfo={solarInfo}
            activeRoute={activeRoute}
            activeFilter={activeFilter}
            isFirstPerson={isWalkMode}
            isAngleLocked={isAngleLocked}
            onSelectBuilding={(name, feature) => setSelectedBuilding({ name, feature })}
            onLabelsUpdate={setCampusLabels}
          />
        ) : (
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
        )}
      </div>

      {/* Floating 3D Building Pills across 3D screen space */}
      {viewMode === '3D' && (
        <div className="absolute inset-0 pointer-events-none z-20">
          {campusLabels.map((lbl) => {
            if (!lbl.visible) return null;
            if (activeFilter !== 'All' && lbl.category !== activeFilter) return null;

            return (
              <div
                key={lbl.id}
                style={{
                  transform: `translate3d(${lbl.screenX}px, ${lbl.screenY}px, 0) translate(-50%, -100%)`,
                }}
                onClick={() => setSelectedBuilding({ name: lbl.name, feature: null })}
                className="absolute pointer-events-auto cursor-pointer group transition-transform duration-75 hover:scale-110"
              >
                <div className="flex items-center gap-1.5 bg-[#09101d]/90 hover:bg-[#0f172a] border border-cyan-500/30 hover:border-cyan-400 text-slate-200 hover:text-white px-2.5 py-1 rounded-full text-[11px] font-bold shadow-xl backdrop-blur-md">
                  <span
                    className="w-2 h-2 rounded-full flex-shrink-0 animate-pulse"
                    style={{ backgroundColor: lbl.color }}
                  />
                  <span className="whitespace-nowrap tracking-wide">{lbl.name}</span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Top Left Title Card (Exact IITK 3D Card Style) */}
      <div className="absolute top-4 left-4 z-40 pointer-events-auto">
        <div className="bg-[#0b1324]/90 backdrop-blur-xl border border-slate-700/60 rounded-2xl p-3.5 shadow-2xl flex flex-col gap-0.5 min-w-[190px]">
          <div className="flex items-center gap-2">
            <h1 className="text-base font-extrabold tracking-tight text-white">
              SRM AP <span className="text-amber-400 font-bold">in 3D</span>
            </h1>
          </div>
          <div className="text-[11px] text-slate-400 flex items-center gap-1">
            <span>Production Campus Navigator</span> • <span className="text-amber-400 font-medium">Verified</span>
          </div>
        </div>
      </div>

      {/* Top Right Universal Search Bar (Matching IITK 3D UI) */}
      <div className="absolute top-4 right-4 z-40 flex items-center gap-2 pointer-events-auto">
        <button
          onClick={() => setIsSearchOpen(true)}
          className="flex items-center gap-3 bg-[#0b1324]/90 backdrop-blur-xl border border-slate-700/60 hover:border-slate-500 rounded-2xl px-4 py-2.5 shadow-2xl text-slate-400 hover:text-white transition-all w-72 sm:w-80 group cursor-pointer"
        >
          <Search className="w-4 h-4 text-slate-400 group-hover:text-amber-400 transition-colors" />
          <span className="text-xs font-medium text-slate-400">Search halls, faculty, food, roads...</span>
          <span className="ml-auto text-[10px] bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded font-mono">
            ⌘K
          </span>
        </button>

        <button
          onClick={() => setIsSearchOpen(true)}
          className="p-2.5 rounded-2xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold transition-all shadow-lg cursor-pointer"
          title="Direct search"
        >
          <Search className="w-4 h-4" />
        </button>
      </div>

      {/* Top Category Filter Pills (IITK 3D Horizontal Category Bar) */}
      <div className="absolute top-20 right-4 z-40 flex items-center gap-1.5 overflow-x-auto max-w-[calc(100vw-2rem)] py-1 pointer-events-auto scrollbar-none">
        {categories.map((cat) => (
          <button
            key={cat.name}
            onClick={() => setActiveFilter(cat.name)}
            className={`flex items-center gap-1.5 text-xs font-bold px-3 py-1.5 rounded-full transition-all cursor-pointer whitespace-nowrap backdrop-blur-xl border ${
              activeFilter === cat.name
                ? 'bg-amber-500 text-slate-950 border-amber-400 shadow-md font-extrabold'
                : 'bg-[#0b1324]/85 text-slate-300 border-slate-700/60 hover:bg-slate-800/80'
            }`}
          >
            {cat.name !== 'All' && (
              <span className="w-2 h-2 rounded-full" style={{ backgroundColor: cat.color }} />
            )}
            <span>{cat.name}</span>
          </button>
        ))}
      </div>

      {/* Floor Selector in 2D Mode */}
      {viewMode === '2D' && (
        <div className="absolute right-4 top-36 z-50 pointer-events-auto">
          <FloorSelector selectedFloor={selectedFloor} onSelectFloor={setSelectedFloor} />
        </div>
      )}

      {/* Bottom Center Floating Controls (Time badge, Walk, Recenter, 2D/3D, Lock angle like IITK) */}
      <div className="absolute bottom-5 left-1/2 -translate-x-1/2 z-40 flex items-center gap-2 pointer-events-auto">
        {/* Time of Day Button (Pops up slider) */}
        <button
          onClick={() => setIsTimePickerOpen(!isTimePickerOpen)}
          className="flex items-center gap-2 bg-[#0b1324]/90 backdrop-blur-xl border border-slate-700/60 hover:border-slate-500 rounded-2xl px-3.5 py-2.5 text-xs font-bold text-white shadow-2xl transition-all cursor-pointer"
          title="Adjust time of day"
        >
          <Clock className="w-4 h-4 text-amber-400" />
          <span>{formatTime(simulatedTime)}</span>
          <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
        </button>

        {/* Walk / First-person mode toggle */}
        <button
          onClick={() => setIsWalkMode(!isWalkMode)}
          className={`flex items-center gap-1.5 text-xs font-bold px-3.5 py-2.5 rounded-2xl border transition-all shadow-2xl cursor-pointer ${
            isWalkMode
              ? 'bg-teal-500 text-slate-950 border-teal-400'
              : 'bg-[#0b1324]/90 backdrop-blur-xl border-slate-700/60 text-slate-200 hover:bg-slate-800'
          }`}
          title="Toggle Walk mode"
        >
          <Footprints className="w-4 h-4" />
          <span>Walk</span>
        </button>

        {/* Recenter Campus Origin Button */}
        <button
          onClick={() => {
            window.location.reload();
          }}
          className="p-2.5 rounded-2xl bg-amber-500 hover:bg-amber-400 text-slate-950 transition-all shadow-2xl cursor-pointer"
          title="Recenter Camera to Campus Core"
        >
          <Navigation className="w-4 h-4" />
        </button>

        {/* Reset Camera Orbit Angle */}
        <button
          onClick={() => setIsAngleLocked(!isAngleLocked)}
          className="p-2.5 rounded-2xl bg-[#0b1324]/90 backdrop-blur-xl border border-slate-700/60 hover:bg-slate-800 text-slate-300 transition-all shadow-2xl cursor-pointer"
          title={isAngleLocked ? 'Unlock rotation' : 'Lock rotation'}
        >
          {isAngleLocked ? <Lock className="w-4 h-4 text-amber-400" /> : <Unlock className="w-4 h-4" />}
        </button>

        {/* 2D / 3D Mode Toggle Switch */}
        <button
          onClick={() => setViewMode(viewMode === '3D' ? '2D' : '3D')}
          className="flex items-center gap-1.5 bg-[#0b1324]/90 backdrop-blur-xl border border-slate-700/60 hover:border-slate-500 rounded-2xl px-3.5 py-2.5 text-xs font-bold text-white shadow-2xl transition-all cursor-pointer"
        >
          {viewMode === '3D' ? (
            <>
              <MapIcon className="w-4 h-4 text-sky-400" />
              <span>2D map</span>
            </>
          ) : (
            <>
              <Box className="w-4 h-4 text-amber-400" />
              <span>3D view</span>
            </>
          )}
        </button>
      </div>

      {/* Pop-up Time Slider if open */}
      {isTimePickerOpen && (
        <div className="absolute bottom-20 left-1/2 -translate-x-1/2 z-50 w-80 sm:w-96 pointer-events-auto">
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
      )}

      {/* Navigation Turn-by-Turn Card */}
      {activeRoute && (
        <div className="absolute bottom-20 right-4 z-50 pointer-events-auto">
          <NavigationSheet
            route={activeRoute}
            destinationTitle={navigationTargetName}
            onCancel={() => setActiveRoute(null)}
          />
        </div>
      )}

      {/* Building Inspection Sheet */}
      {selectedBuilding && (
        <div className="absolute bottom-20 right-4 z-50 pointer-events-auto">
          <BlockInspectionSheet
            buildingName={selectedBuilding.name}
            properties={selectedBuilding.feature?.properties || { levels: 4 }}
            rooms={
              roomsData?.features?.filter((f: any) =>
                (f.properties?.block || '').toLowerCase().includes(selectedBuilding.name.toLowerCase())
              ) || []
            }
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
