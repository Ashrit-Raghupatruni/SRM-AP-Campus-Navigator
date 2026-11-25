import React from 'react';
import { Sun, Moon, Sunrise, Sunset, Clock, Sparkles } from 'lucide-react';
import { formatTime } from '../../core/solar';
import { SolarInfo } from '../../types/campus';

interface TimeControlBarProps {
  solarInfo: SolarInfo;
  simulatedTime: Date;
  isLiveTime: boolean;
  onTimeChange: (newDate: Date) => void;
  onToggleLive: () => void;
}

export const TimeControlBar: React.FC<TimeControlBarProps> = ({
  solarInfo,
  simulatedTime,
  isLiveTime,
  onTimeChange,
  onToggleLive,
}) => {
  const currentMinutes = simulatedTime.getHours() * 60 + simulatedTime.getMinutes();

  const handleSliderChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const mins = parseInt(e.target.value, 10);
    const d = new Date(simulatedTime);
    d.setHours(Math.floor(mins / 60));
    d.setMinutes(mins % 60);
    d.setSeconds(0);
    onTimeChange(d);
  };

  const getPhaseName = () => {
    if (solarInfo.isGoldenHour) return 'Golden Hour';
    if (solarInfo.isNight) return 'Night View';
    return 'Daylight';
  };

  return (
    <div className="bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-lg p-3 sm:p-4 text-slate-800 dark:text-slate-100 flex flex-col gap-2 transition-all">
      {/* Top Header */}
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          {solarInfo.isNight ? (
            <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400 ring-1 ring-indigo-500/20">
              <Moon className="w-4 h-4" />
            </div>
          ) : solarInfo.isGoldenHour ? (
            <div className="p-2 rounded-xl bg-amber-500/10 text-amber-500 ring-1 ring-amber-500/20">
              <Sparkles className="w-4 h-4" />
            </div>
          ) : (
            <div className="p-2 rounded-xl bg-teal-500/10 text-teal-600 dark:text-teal-400 ring-1 ring-teal-500/20">
              <Sun className="w-4 h-4" />
            </div>
          )}
          <div>
            <div className="flex items-center gap-1.5 font-bold text-sm tracking-tight">
              <span>{formatTime(simulatedTime)}</span>
              <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                {getPhaseName()}
              </span>
            </div>
            <div className="text-[11px] text-slate-500 dark:text-slate-400">SRM AP Local Time</div>
          </div>
        </div>

        {/* Live sync toggle */}
        <button
          onClick={onToggleLive}
          className={`flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-xl transition-all cursor-pointer ${
            isLiveTime
              ? 'bg-teal-600 text-white shadow-sm hover:bg-teal-700'
              : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
          }`}
          title="Toggle live local clock synchronization"
        >
          <Clock className="w-3.5 h-3.5" />
          <span>{isLiveTime ? 'Live Sync' : 'Custom'}</span>
        </button>
      </div>

      {/* 24h Slider */}
      <div className="flex flex-col gap-1 mt-1">
        <input
          type="range"
          min="0"
          max="1439"
          value={currentMinutes}
          onChange={handleSliderChange}
          className="w-full h-1.5 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none cursor-pointer accent-teal-600 transition-colors"
        />
        <div className="flex justify-between items-center text-[10px] text-slate-400 font-mono">
          <span>00:00</span>
          <span className="flex items-center gap-1 text-amber-500 font-medium">
            <Sunrise className="w-3 h-3" /> {formatTime(solarInfo.sunrise)}
          </span>
          <span>12:00</span>
          <span className="flex items-center gap-1 text-orange-400 font-medium">
            <Sunset className="w-3 h-3" /> {formatTime(solarInfo.sunset)}
          </span>
          <span>23:59</span>
        </div>
      </div>
    </div>
  );
};
