import React from 'react';
import { X, Building2, Layers, Navigation } from 'lucide-react';

interface BlockInspectionSheetProps {
  buildingName: string;
  properties: any;
  rooms: any[];
  onClose: () => void;
  onNavigateToRoom: (room: any) => void;
}

export const BlockInspectionSheet: React.FC<BlockInspectionSheetProps> = ({
  buildingName,
  properties,
  rooms,
  onClose,
  onNavigateToRoom,
}) => {
  const levels = properties.levels || 4;

  return (
    <div className="bg-white/95 dark:bg-slate-900/95 backdrop-blur-md border border-slate-200/80 dark:border-slate-800 rounded-3xl shadow-2xl p-4 sm:p-5 flex flex-col gap-3 max-w-md w-full max-h-[70vh] transition-all">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="p-2.5 rounded-2xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400">
            <Building2 className="w-5 h-5" />
          </div>
          <div>
            <div className="font-bold text-slate-800 dark:text-slate-100 text-base">{buildingName}</div>
            <div className="text-xs text-slate-400 flex items-center gap-1.5">
              <span>{levels} Stories</span> • <span>{rooms.length} Verified Rooms & Facilities</span>
            </div>
          </div>
        </div>
        <button
          onClick={onClose}
          className="p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="text-xs font-semibold uppercase tracking-wider text-slate-400 mt-2">
        Rooms & Directory
      </div>

      <div className="overflow-y-auto flex-1 divide-y divide-slate-100 dark:divide-slate-800/60 pr-1">
        {rooms.length === 0 ? (
          <div className="py-6 text-center text-xs text-slate-400">
            Select a floor in the floor selector to view specific rooms in this block.
          </div>
        ) : (
          rooms.map((r, i) => {
            const p = r.properties || r;
            const floor = typeof p.floor === 'number' ? p.floor : parseInt(p.floor || '0', 10);
            return (
              <div key={i} className="py-2.5 flex items-center justify-between gap-2">
                <div>
                  <div className="font-bold text-xs text-slate-800 dark:text-slate-200">
                    {p.room_number || p.room_id}
                  </div>
                  <div className="text-[11px] text-slate-400">
                    {p.description || 'Academic Room'} • Floor {floor}
                  </div>
                </div>
                <button
                  onClick={() => onNavigateToRoom(p)}
                  className="p-1.5 rounded-lg bg-teal-50 dark:bg-teal-950/60 text-teal-600 hover:bg-teal-600 hover:text-white transition-colors"
                  title="Navigate here"
                >
                  <Navigation className="w-3.5 h-3.5" />
                </button>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
