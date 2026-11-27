import React, { useState, useEffect } from 'react';
import { Sparkles } from 'lucide-react';

export interface LoadingTaskStatus {
  mapOverview: boolean;
  academicBlocks: boolean;
  navigationGraph: boolean;
  facultyDirectory: boolean;
  roomDetails: boolean;
}

interface CampusEngineLoadingProps {
  status?: LoadingTaskStatus;
}

export const CampusEngineLoading: React.FC<CampusEngineLoadingProps> = ({ status }) => {
  const steps = [
    { label: 'Campus Map & Roads', key: 'mapOverview' as const },
    { label: 'Academic & Hostel Blocks', key: 'academicBlocks' as const },
    { label: 'Pedestrian Navigation Graph', key: 'navigationGraph' as const },
    { label: 'Faculty Directory & Cabins', key: 'facultyDirectory' as const },
    { label: 'Classrooms & Research Labs', key: 'roomDetails' as const },
  ];

  // Dynamically cycle through each step smoothly from step 0 to step 4
  const [activeStepIndex, setActiveStepIndex] = useState(0);

  useEffect(() => {
    // Progress through each stage step by step every 450ms
    const interval = setInterval(() => {
      setActiveStepIndex((prev) => {
        if (prev < steps.length - 1) {
          return prev + 1;
        }
        return prev;
      });
    }, 500);

    return () => clearInterval(interval);
  }, [steps.length]);

  const currentTask = steps[activeStepIndex];

  return (
    <div className="w-full h-full flex flex-col items-center justify-center bg-[#040810] text-slate-200 select-none">
      {/* Golden Glowing Spinner */}
      <div className="relative mb-5 flex items-center justify-center">
        <div className="w-12 h-12 rounded-full border-2 border-amber-500/15" />
        <div className="absolute inset-0 w-12 h-12 rounded-full border-2 border-transparent border-t-amber-500 border-r-amber-400 animate-spin" />
        <Sparkles className="w-4 h-4 text-amber-400/80 absolute animate-pulse" />
      </div>

      {/* Main Dynamic Status Line (Replacing "Loading 3D Campus Engine...") */}
      <div className="flex items-center gap-2 mb-2 transition-all duration-300">
        <span className="text-sm sm:text-base font-semibold tracking-wide text-slate-100">
          Loading {currentTask.label}...
        </span>
      </div>

      {/* Subtle Step Trail */}
      <div className="flex items-center gap-1.5 mt-2">
        {steps.map((st, idx) => {
          const isDone = idx < activeStepIndex;
          const isCurrent = idx === activeStepIndex;
          return (
            <div
              key={st.key}
              className={`h-1.5 rounded-full transition-all duration-500 ${
                isDone
                  ? 'w-6 bg-teal-400'
                  : isCurrent
                  ? 'w-8 bg-amber-400 animate-pulse'
                  : 'w-2 bg-slate-700/60'
              }`}
              title={st.label}
            />
          );
        })}
      </div>
    </div>
  );
};
