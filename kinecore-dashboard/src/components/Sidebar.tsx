"use client";

import React from 'react';
import { 
  Activity, 
  BarChart3, 
  Database, 
  LayoutDashboard, 
  Settings, 
  Zap 
} from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function Sidebar() {
  const menuItems = [
    { icon: LayoutDashboard, label: 'Overview', active: true },
    { icon: Database, label: 'Models', active: false },
    { icon: Activity, label: 'Simulations', active: false },
    { icon: BarChart3, label: 'Analytics', active: false },
    { icon: Settings, label: 'Settings', active: false },
  ];

  return (
    <aside className="w-64 border-r border-white/5 bg-[#05050a] flex flex-col h-screen sticky top-0">
      <div className="p-6 flex items-center gap-3">
        <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
          <Zap className="text-white w-5 h-5 fill-white" />
        </div>
        <h1 className="text-xl font-bold tracking-tight text-white">KineCore</h1>
      </div>

      <nav className="flex-1 px-4 py-4 space-y-2">
        {menuItems.map((item) => (
          <button
            key={item.label}
            className={cn(
              "w-full flex items-center gap-3 px-3 py-2 rounded-md transition-all duration-200 group",
              item.active 
                ? "bg-white/5 text-white" 
                : "text-zinc-500 hover:text-white hover:bg-white/5"
            )}
          >
            <item.icon className={cn(
              "w-5 h-5",
              item.active ? "text-indigo-500" : "group-hover:text-indigo-400"
            )} />
            <span className="font-medium text-sm">{item.label}</span>
          </button>
        ))}
      </nav>

      <div className="p-6 border-t border-white/5">
        <div className="bg-indigo-600/10 border border-indigo-500/20 rounded-xl p-4">
          <p className="text-xs font-semibold text-indigo-400 uppercase tracking-wider mb-1">Status</p>
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            <span className="text-sm text-zinc-300">Engine Connected</span>
          </div>
        </div>
      </div>
    </aside>
  );
}
