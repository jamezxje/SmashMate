import React, { useState } from 'react';
import { scheduleApi } from '../../api/schedules';
import type { ScheduleResponse } from '../../types';
import { X, Calendar, Clock, MapPin } from 'lucide-react';

interface Props {
  schedule: ScheduleResponse | null;
  onClose: () => void;
  onSaved: () => void;
}

const dayNames = [
  'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'
];

export function ScheduleFormModal({ schedule, onClose, onSaved }: Props) {
  const [dayOfWeek, setDayOfWeek] = useState(schedule?.dayOfWeek ?? 2);
  const [startTime, setStartTime] = useState(schedule?.startTime ?? '18:00');
  const [endTime, setEndTime] = useState(schedule?.endTime ?? '20:00');
  const [venueName, setVenueName] = useState(schedule?.venueName ?? '');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (schedule) {
        await scheduleApi.update(schedule.id, { dayOfWeek, startTime, endTime, venueName });
      } else {
        await scheduleApi.create({ dayOfWeek, startTime, endTime, venueName: venueName || undefined });
      }
      onSaved();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Không thể lưu lịch sinh hoạt');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700">
          <h2 className="text-lg font-bold text-white">
            {schedule ? 'Sửa lịch định kỳ' : 'Tạo lịch sinh hoạt định kỳ'}
          </h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white p-1 rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">
              Thứ trong tuần *
            </label>
            <div className="relative">
              <Calendar className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <select
                value={dayOfWeek}
                onChange={(e) => setDayOfWeek(Number(e.target.value))}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
              >
                {dayNames.map((name, idx) => (
                  <option key={idx + 1} value={idx + 1}>{name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">
                Giờ bắt đầu *
              </label>
              <div className="relative">
                <Clock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="time"
                  required
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">
                Giờ kết thúc *
              </label>
              <div className="relative">
                <Clock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="time"
                  required
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">
              Sân tập / Địa điểm
            </label>
            <div className="relative">
              <MapPin className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={venueName}
                onChange={(e) => setVenueName(e.target.value)}
                placeholder="Sân Cầu Lông ABC - Sân số 2"
                className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          {error && <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-xl text-rose-400 text-xs">{error}</div>}

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="flex-1 bg-slate-700 text-slate-200 py-2.5 rounded-xl text-sm">Hủy</button>
            <button type="submit" disabled={loading} className="flex-1 bg-emerald-500 text-slate-950 font-bold py-2.5 rounded-xl text-sm disabled:opacity-50">
              {loading ? 'Đang lưu...' : 'Lưu lịch tập'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
