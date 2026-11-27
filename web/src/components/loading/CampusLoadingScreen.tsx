import React, { useState, useEffect } from 'react';
import { Map, CheckCircle2, Loader2, Sparkles, X, ArrowRight } from 'lucide-react';

export interface LoadingTaskStatus {
  mapOverview: boolean;
  academicBlocks: boolean;
  navigationGraph: boolean;
  facultyDirectory: boolean;
  roomDetails: boolean;
}

interface CampusLoadingScreenProps {
  status: LoadingTaskStatus;
  onDismiss: () => void;
}

export const CampusLoadingScreen: React.FC<CampusLoadingScreenProps> = ({
  status,
  onDismiss,
}) => {
  const [visible, setVisible] = useState(true);

  // Auto-dismiss after all essential tasks are loaded (or after max 7s)
  useEffect(() => {
    const isEssentialDone = status.mapOverview && status.academicBlocks;
    if (isEssentialDone) {
      const timer = setTimeout(() => {
        setVisible(false);
        onDismiss();
      }, 3500);
      return () => clearTimeout(timer);
    }
  }, [status.mapOverview, status.academicBlocks, onDismiss]);

  if (!visible) return null;

  const tasks = [
    { label: 'Campus Map & Roads', done: status.mapOverview },
    { label: 'Academic & Hostel Blocks', done: status.academicBlocks },
    { label: 'Pedestrian Navigation Graph', done: status.navigationGraph },
    { label: 'Faculty Directory & Cabins', done: status.facultyDirectory },
    { label: 'Classrooms & Research Labs', done: status.roomDetails },
  ];

  const completedCount = tasks.filter((t) => t.done).length;
  const progressPercent = Math.round((completedCount / tasks.length) * 100);

  return (
    <div className="fixed bottom-6 right-6 z-50 max-w-sm w-full animate-in fade-in slide-in-from-bottom-4 duration-300">
      <div className="bg-[#0b1324]/95 backdrop-blur-xl border border-slate-700/70 rounded-3xl p-5 shadow-2xl text-slate-100 flex flex-col gap-3.5">
        {/* Top Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-teal-500/10 border border-teal-500/20 flex items-center justify-center text-teal-400">
              <Sparkles className="w-4 h-4 animate-spin-slow" />
            </div>
            <div>
              <div className="text-xs font-black tracking-wider text-teal-400 uppercase">
                SRM AP CAMPUS
              </div>
              <div className="text-sm font-bold text-white">Preparing your experience</div>
            </div>
          </div>
          <button
            onClick={() => {
              setVisible(false);
              onDismiss();
            }}
            className="p-1.5 text-slate-400 hover:text-white rounded-xl hover:bg-slate-800/60 transition-colors"
            title="Dismiss loading introduction"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Real Task Progression List */}
        <div className="flex flex-col gap-2 py-1">
          {tasks.map((task, idx) => (
            <div key={idx} className="flex items-center justify-between text-xs font-medium">
              <span className={task.done ? 'text-slate-300' : 'text-slate-500'}>
                {task.label}
              </span>
              {task.done ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0" />
              ) : (
                <Loader2 className="w-3.5 h-3.5 text-teal-400 animate-spin flex-shrink-0" />
              )}
            </div>
          ))}
        </div>

        {/* Progress Bar & Interactive Skip Button */}
        <div className="pt-2 border-t border-slate-800 flex flex-col gap-2.5">
          <div className="flex items-center justify-between text-[11px] text-slate-400 font-semibold">
            <span>Progressive Loading</span>
            <span>{progressPercent}%</span>
          </div>
          <div className="w-full bg-slate-800/80 rounded-full h-1.5 overflow-hidden">
            <div
              className="bg-gradient-to-r from-teal-500 to-sky-400 h-full transition-all duration-300 rounded-full"
              style={{ width: `${progressPercent}%` }}
            />
          </div>

          <button
            onClick={() => {
              setVisible(false);
              onDismiss();
            }}
            className="mt-1 w-full py-2 px-3 rounded-xl bg-teal-500/10 hover:bg-teal-500/20 text-teal-300 border border-teal-500/30 text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer"
          >
            <span>Start Exploring Now</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
};
