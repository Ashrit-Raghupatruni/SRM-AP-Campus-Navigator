import React from 'react';
import { Navigation, Clock, Footprints, X, MapPin } from 'lucide-react';
import { RouteResult } from '../../types/campus';

interface NavigationSheetProps {
  route: RouteResult;
  destinationTitle: string;
  originLabel?: string;
  onCancel: () => void;
  onRecenter?: () => void;
}

export const NavigationSheet: React.FC<NavigationSheetProps> = ({
  route,
  destinationTitle,
  originLabel = 'Current Location',
  onCancel,
  onRecenter,
}) => {
  return (
    <div className="bg-white/95 dark:bg-slate-900/95 backdrop-blur-md border border-slate-200/80 dark:border-slate-800 rounded-3xl shadow-2xl p-4 sm:p-5 flex flex-col gap-3 max-w-md w-full animate-in fade-in slide-in-from-bottom-3 duration-200">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="p-2.5 rounded-2xl bg-teal-500/10 text-teal-600 dark:text-teal-400">
            <Navigation className="w-5 h-5 fill-current" />
          </div>
          <div>
            <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1">
              <MapPin className="w-3 h-3 text-teal-500" />
              <span>From: {originLabel}</span>
            </div>
            <div className="font-extrabold text-slate-800 dark:text-slate-100 text-base">{destinationTitle}</div>
          </div>
        </div>
        <div className="flex items-center gap-1">
          {onRecenter && (
            <button
              onClick={onRecenter}
              className="p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
              title="Recenter Route"
            >
              <Navigation className="w-4 h-4" />
            </button>
          )}
          <button
            onClick={onCancel}
            className="p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 gap-2 bg-slate-50 dark:bg-slate-800/50 p-3 rounded-2xl border border-slate-100 dark:border-slate-800">
        <div className="flex items-center gap-2">
          <Clock className="w-4 h-4 text-teal-600" />
          <div>
            <div className="text-sm font-extrabold text-slate-800 dark:text-slate-100">
              {route.estimatedMinutes} min
            </div>
            <div className="text-[10px] text-slate-400">Estimated Walking</div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Footprints className="w-4 h-4 text-sky-600" />
          <div>
            <div className="text-sm font-extrabold text-slate-800 dark:text-slate-100">
              {route.totalDistanceMeters} m
            </div>
            <div className="text-[10px] text-slate-400">Campus Distance</div>
          </div>
        </div>
      </div>

      {/* Turn Cues */}
      <div className="flex flex-col gap-2 max-h-40 overflow-y-auto pr-1">
        {route.steps.map((st, i) => (
          <div key={i} className="flex items-start gap-2 text-xs text-slate-600 dark:text-slate-300">
            <div className="mt-1 w-2 h-2 rounded-full bg-teal-500 flex-shrink-0" />
            <div>
              <div className="font-medium">{st.instruction}</div>
              {st.distanceMeters > 0 && (
                <div className="text-[10px] text-slate-400">{st.distanceMeters} meters along path</div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
