"use client";

import React from 'react';
import { 
  Plus, 
  Play, 
  Clock, 
  CheckCircle2, 
  AlertCircle,
  TrendingUp,
  Users,
  Target,
  Activity
} from 'lucide-react';
import { 
  AreaChart, 
  Area, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer 
} from 'recharts';

const dummyData = [
  { time: 0, p5: 100, p50: 100, p95: 100 },
  { time: 20, p5: 90, p50: 110, p95: 130 },
  { time: 40, p5: 85, p50: 125, p95: 170 },
  { time: 60, p5: 70, p50: 150, p95: 240 },
  { time: 80, p5: 60, p50: 180, p95: 350 },
  { time: 100, p5: 50, p50: 220, p95: 500 },
];

export default function DashboardOverview() {
  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex justify-between items-end">
        <div>
          <h2 className="text-3xl font-bold text-white tracking-tight">System Overview</h2>
          <p className="text-zinc-500 mt-1">Monitor real-time simulation convergence and model performance.</p>
        </div>
        <button className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg font-semibold flex items-center gap-2 transition-all shadow-lg shadow-indigo-600/20 active:scale-95">
          <Plus className="w-5 h-5" />
          Create New Model
        </button>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {[
          { label: 'Active Jobs', value: '3', icon: Activity, color: 'text-indigo-400' },
          { label: 'Total Models', value: '12', icon: Target, color: 'text-purple-400' },
          { label: 'Success Rate', value: '98.2%', icon: TrendingUp, color: 'text-emerald-400' },
        ].map((stat) => (
          <div key={stat.label} className="glass-card p-6 rounded-2xl">
            <div className="flex justify-between items-start">
              <stat.icon className={`w-6 h-6 ${stat.color}`} />
              <span className="text-2xl font-bold text-white">{stat.value}</span>
            </div>
            <p className="text-sm font-medium text-zinc-500 mt-2">{stat.label}</p>
          </div>
        ))}
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Simulation Chart */}
        <div className="lg:col-span-2 glass-card rounded-2xl p-6">
          <div className="flex justify-between items-center mb-8">
            <h3 className="text-lg font-semibold text-white">Live Projection Convergence</h3>
            <div className="flex gap-4 text-xs font-medium">
              <div className="flex items-center gap-1.5 text-zinc-400">
                <div className="w-2.5 h-2.5 rounded-full bg-indigo-500/20 border border-indigo-500" />
                95% Confidence
              </div>
              <div className="flex items-center gap-1.5 text-zinc-400">
                <div className="w-2.5 h-2.5 rounded-full bg-indigo-500" />
                Median (p50)
              </div>
            </div>
          </div>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={dummyData}>
                <defs>
                  <linearGradient id="colorP50" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#ffffff05" />
                <XAxis 
                  dataKey="time" 
                  stroke="#71717a" 
                  fontSize={12} 
                  tickLine={false} 
                  axisLine={false}
                />
                <YAxis 
                  stroke="#71717a" 
                  fontSize={12} 
                  tickLine={false} 
                  axisLine={false}
                  tickFormatter={(value) => `${value}`}
                />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#09090b', borderColor: '#27272a', color: '#fff' }}
                  itemStyle={{ color: '#fff' }}
                />
                <Area 
                  type="monotone" 
                  dataKey="p95" 
                  stroke="none" 
                  fill="#6366f1" 
                  fillOpacity={0.05} 
                />
                <Area 
                  type="monotone" 
                  dataKey="p5" 
                  stroke="none" 
                  fill="#6366f1" 
                  fillOpacity={0.1} 
                />
                <Area 
                  type="monotone" 
                  dataKey="p50" 
                  stroke="#6366f1" 
                  strokeWidth={3}
                  fillOpacity={1} 
                  fill="url(#colorP50)" 
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Recent Jobs */}
        <div className="glass-card rounded-2xl p-6">
          <h3 className="text-lg font-semibold text-white mb-6">Recent Deployments</h3>
          <div className="space-y-4">
            {[
              { id: 'df0d1e1c', name: 'Kerala-2026-v2', status: 'completed', time: '2 mins ago' },
              { id: 'ab339c5c', name: 'SIR-Standard-Model', status: 'running', time: '5 mins ago' },
              { id: 'xe967-75', name: 'Climate-Feedback-Loop', status: 'failed', time: '12 mins ago' },
              { id: 'db339c5c', name: 'Socio-Economic-Stress', status: 'completed', time: '1 hr ago' },
            ].map((job) => (
              <div key={job.id} className="flex items-center justify-between p-3 rounded-xl hover:bg-white/5 transition-colors cursor-pointer group">
                <div className="flex items-center gap-3">
                  <div className={`p-2 rounded-lg ${
                    job.status === 'completed' ? 'bg-emerald-500/10' :
                    job.status === 'running' ? 'bg-indigo-500/10' : 'bg-red-500/10'
                  }`}>
                    {job.status === 'completed' && <CheckCircle2 className="w-4 h-4 text-emerald-500" />}
                    {job.status === 'running' && <Play className="w-4 h-4 text-indigo-500 animate-pulse fill-indigo-500" />}
                    {job.status === 'failed' && <AlertCircle className="w-4 h-4 text-red-500" />}
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-zinc-200">{job.name}</p>
                    <p className="text-xs text-zinc-500">{job.time}</p>
                  </div>
                </div>
                <div className="text-[10px] font-mono text-zinc-600 group-hover:text-zinc-400">
                  #{job.id}
                </div>
              </div>
            ))}
          </div>
          <button className="w-full mt-6 py-2.5 rounded-xl border border-white/5 text-sm font-medium text-zinc-400 hover:text-white hover:bg-white/5 transition-all">
            View All Jobs
          </button>
        </div>
      </div>
    </div>
  );
}
