import React, { useState } from 'react';
import { sessionApi } from '../../api/sessions';
import type { SessionResponse } from '../../types';
import { X, Calendar, Clock, MapPin, FileText } from 'lucide-react';

interface Props {
  session: SessionResponse | null;
  onClose: () => void;
  onSaved: () => void;
}

export function SessionFormModal({ session, onClose, onSaved }: Props) {
  const [sessionDate, setSessionDate] = useState(session?.sessionDate ?? new Date().toISOString().split('T')[0]);
  const [startTime, setStartTime] = useState(session?.startTime ?? '18:00');
  const [endTime, setEndTime] = useState(session?.endTime ?? '20:00');
  const [venueName, setVenueName] = useState(session?.venueName ?? '');
  const [notes, setNotes] = useState(session?.notes ?? '');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (session) {
        await sessionApi.update(session.id, { sessionDate, startTime, endTime, venueName, notes });
      } else {
        await sessionApi.create({ sessionDate, startTime, endTime, venueName: venueName || undefined, notes: notes || undefined });
      }
      onSaved();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Không thể lưu buổi sinh hoạt');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700">
          <h2 className="text-lg font-bold text-white">{session ? 'Sửa buổi sinh hoạt' : 'Tạo buổi tập mới'}</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white p-1 rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">Ngày sinh hoạt *</label>
            <div className="relative">
              <Calendar className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input type="date" required value={sessionDate} onChange={(e) => setSessionDate(e.target.value)} className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500" />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">Giờ bắt đầu *</label>
              <div className="relative">
                <Clock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input type="time" required value={startTime} onChange={(e) => setStartTime(e.target.value)} className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500" />
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">Giờ kết thúc</label>
              <div className="relative">
                <Clock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500" />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">Địa điểm / Sân tập</label>
            <div className="relative">
              <MapPin className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input type="text" value={venueName} onChange={(e) => setVenueName(e.target.value)} placeholder="Sân số 3 - Cầu lông Thể Thao" className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500" />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase mb-1">Ghi chú</label>
            <div className="relative">
              <FileText className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={3} placeholder="Ghi chú buổi tập..." className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500" />
            </div>
          </div>

          {error && <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-xl text-rose-400 text-xs">{error}</div>}

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="flex-1 bg-slate-700 text-slate-200 py-2.5 rounded-xl text-sm">Hủy</button>
            <button type="submit" disabled={loading} className="flex-1 bg-emerald-500 text-slate-950 font-bold py-2.5 rounded-xl text-sm disabled:opacity-50">
              {loading ? 'Đang lưu...' : 'Lưu buổi tập'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
