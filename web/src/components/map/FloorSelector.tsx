import React from 'react';
import { Layers } from 'lucide-react';

interface FloorSelectorProps {
  selectedFloor: number | null;
  onSelectFloor: (floor: number | null) => void;
  floors?: number[];
}

export const FloorSelector: React.FC<FloorSelectorProps> = ({
  selectedFloor,
  onSelectFloor,
  floors = [5, 4, 3, 2, 1, 0],
}) => {
  return (
    <div className="flex flex-col bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-lg p-1.5 gap-1">
      <div className="flex items-center justify-center p-2 text-slate-500 dark:text-slate-400" title="Floor Selector">
        <Layers className="w-4 h-4" />
      </div>

      <button
        onClick={() => onSelectFloor(null)}
        className={`px-2.5 py-1.5 text-xs font-bold rounded-xl transition-all cursor-pointer ${
          selectedFloor === null
            ? 'bg-srm-navy text-white shadow-sm'
            : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
        }`}
      >
        ALL
      </button>

      {floors.map((fl) => (
        <button
          key={fl}
          onClick={() => onSelectFloor(fl)}
          className={`px-2.5 py-1.5 text-xs font-bold rounded-xl transition-all cursor-pointer ${
            selectedFloor === fl
              ? 'bg-teal-600 text-white shadow-sm'
              : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
          }`}
        >
          {fl === 0 ? 'GF' : `L${fl}`}
        </button>
      ))}
    </div>
  );
};
