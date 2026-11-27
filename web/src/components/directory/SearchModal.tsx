import React, { useState } from 'react';
import { Search, X, MapPin, User, Beaker, DoorOpen, Navigation } from 'lucide-react';
import { SearchResultItem } from '../../core/search';

interface SearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSearch: (query: string, category: 'all' | 'room' | 'faculty' | 'lab') => SearchResultItem[];
  onSelectItem: (item: SearchResultItem) => void;
  onNavigateTo: (item: SearchResultItem) => void;
}

export const SearchModal: React.FC<SearchModalProps> = ({
  isOpen,
  onClose,
  onSearch,
  onSelectItem,
  onNavigateTo,
}) => {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState<'all' | 'room' | 'faculty' | 'lab'>('all');

  if (!isOpen) return null;

  const results = onSearch(query, category);

  const getIcon = (cat: SearchResultItem['category']) => {
    switch (cat) {
      case 'faculty':
        return <User className="w-4 h-4 text-emerald-500" />;
      case 'lab':
        return <Beaker className="w-4 h-4 text-purple-500" />;
      default:
        return <DoorOpen className="w-4 h-4 text-teal-600" />;
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-start justify-center p-4 sm:pt-20">
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl shadow-2xl w-full max-w-2xl overflow-hidden flex flex-col max-h-[85vh] transition-all">
        {/* Search Input Bar */}
        <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center gap-3">
          <Search className="w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="Search 580+ rooms, labs, faculty cabins, or facilities..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            autoFocus
            className="w-full bg-transparent text-slate-800 dark:text-slate-100 text-base outline-none placeholder:text-slate-400 font-medium"
          />
          {query && (
            <button onClick={() => setQuery('')} className="p-1 text-slate-400 hover:text-slate-600">
              <X className="w-4 h-4" />
            </button>
          )}
          <button
            onClick={onClose}
            className="text-xs font-semibold px-2.5 py-1.5 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-500 hover:text-slate-700 dark:hover:text-slate-200"
          >
            ESC
          </button>
        </div>

        {/* Category Filter Pills */}
        <div className="px-4 py-2.5 bg-slate-50/60 dark:bg-slate-950/40 border-b border-slate-100 dark:border-slate-800 flex gap-2 overflow-x-auto">
          {(['all', 'room', 'faculty', 'lab'] as const).map((cat) => (
            <button
              key={cat}
              onClick={() => setCategory(cat)}
              className={`text-xs font-bold px-3 py-1.5 rounded-xl capitalize transition-all cursor-pointer whitespace-nowrap ${
                category === cat
                  ? 'bg-srm-navy text-white shadow-sm'
                  : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700'
              }`}
            >
              {cat === 'all' ? 'All Directory' : cat === 'room' ? 'Rooms & Classes' : cat === 'faculty' ? 'Faculty Cabins' : 'Research Labs'}
            </button>
          ))}
        </div>

        {/* Results List */}
        <div className="overflow-y-auto flex-1 divide-y divide-slate-100 dark:divide-slate-800/60 p-2">
          {results.length === 0 ? (
            <div className="text-center py-12 text-slate-400 text-sm">
              No matching locations or faculty found for "{query}"
            </div>
          ) : (
            results.map((item) => (
              <div
                key={item.id}
                className="p-3 rounded-2xl hover:bg-slate-50 dark:hover:bg-slate-800/50 flex items-center justify-between gap-3 group transition-colors cursor-pointer"
                onClick={() => {
                  onSelectItem(item);
                  onClose();
                }}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className="p-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 group-hover:scale-105 transition-transform">
                    {getIcon(item.category)}
                  </div>
                  <div className="min-w-0">
                    <div className="font-bold text-sm text-slate-800 dark:text-slate-100 truncate">
                      {item.title}
                    </div>
                    <div className="text-xs text-slate-500 dark:text-slate-400 truncate mt-0.5">
                      {item.subtitle}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2 flex-shrink-0">
                  {item.floor !== undefined && (
                    <span className="text-[11px] font-semibold px-2 py-0.5 rounded-lg bg-teal-50 dark:bg-teal-950/60 text-teal-600 dark:text-teal-400 border border-teal-200 dark:border-teal-800">
                      L{item.floor}
                    </span>
                  )}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onNavigateTo(item);
                      onClose();
                    }}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-bold text-xs transition-colors shadow-sm"
                    title="Direct Navigation"
                  >
                    <Navigation className="w-3.5 h-3.5 fill-current" />
                    <span>Navigate</span>
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
