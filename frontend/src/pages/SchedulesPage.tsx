import { useEffect, useState } from 'react';
import { scheduleApi } from '../api/schedules';
import type { ScheduleResponse } from '../types';
import { ScheduleFormModal } from '../components/schedules/ScheduleFormModal';
import { useAuthStore } from '../stores/useAuthStore';
import { Calendar, Plus, ToggleLeft, ToggleRight, Trash2, Edit, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const dayNames = ['', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'];

export function SchedulesPage() {
  const [schedules, setSchedules] = useState<ScheduleResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editTarget, setEditTarget] = useState<ScheduleResponse | null>(null);

  const { role } = useAuthStore();
  const navigate = useNavigate();
  const isAdmin = role === 'ADMIN';

  async function load() {
    setLoading(true);
    try {
      const { data } = await scheduleApi.getAll();
      setSchedules(data.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function handleToggle(id: number) {
    await scheduleApi.toggleActive(id);
    load();
  }

  async function handleDelete(id: number) {
    if (!confirm('Bạn có muốn xóa lịch định kỳ này?')) return;
    await scheduleApi.delete(id);
    load();
  }

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-6">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <button onClick={() => navigate('/sessions')} className="p-2 bg-slate-800 rounded-xl hover:bg-slate-700 text-slate-300">
              <ArrowLeft className="w-5 h-5" />
            </button>
            <h1 className="text-2xl font-bold text-white flex items-center gap-2">
              <Calendar className="w-6 h-6 text-emerald-400" />
              Lịch tập định kỳ hàng tuần
            </h1>
          </div>

          {isAdmin && (
            <button
              onClick={() => { setEditTarget(null); setShowModal(true); }}
              className="flex items-center gap-1.5 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold px-4 py-2 rounded-xl text-xs"
            >
              <Plus className="w-4 h-4" /> + Thêm lịch tập
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {loading ? (
            <p className="text-slate-400 col-span-2 text-center py-8">Đang tải lịch sinh hoạt...</p>
          ) : schedules.length === 0 ? (
            <p className="text-slate-400 col-span-2 text-center py-8">Chưa có lịch sinh hoạt định kỳ nào.</p>
          ) : (
            schedules.map((s) => (
              <div key={s.id} className="bg-slate-800 border border-slate-700/80 rounded-2xl p-5 shadow-lg flex justify-between items-center">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-extrabold text-emerald-400 text-lg">{dayNames[s.dayOfWeek]}</span>
                    <span className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded-full ${s.isActive ? 'bg-emerald-500/10 text-emerald-400' : 'bg-slate-700 text-slate-400'}`}>
                      {s.isActive ? 'Bật' : 'Tắt'}
                    </span>
                  </div>
                  <p className="text-sm font-semibold text-white mt-1">{s.startTime} - {s.endTime}</p>
                  <p className="text-xs text-slate-400 mt-0.5">{s.venueName || 'Chưa xếp địa điểm'}</p>
                </div>

                {isAdmin && (
                  <div className="flex items-center gap-2">
                    <button title="Bật/Tắt lịch" onClick={() => handleToggle(s.id)} className="p-2 text-slate-400 hover:text-emerald-400">
                      {s.isActive ? <ToggleRight className="w-6 h-6 text-emerald-400" /> : <ToggleLeft className="w-6 h-6 text-slate-500" />}
                    </button>
                    <button title="Sửa" onClick={() => { setEditTarget(s); setShowModal(true); }} className="p-2 text-blue-400 hover:bg-blue-400/10 rounded-lg">
                      <Edit className="w-4 h-4" />
                    </button>
                    <button title="Xóa" onClick={() => handleDelete(s.id)} className="p-2 text-rose-400 hover:bg-rose-400/10 rounded-lg">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>

      {showModal && (
        <ScheduleFormModal
          schedule={editTarget}
          onClose={() => setShowModal(false)}
          onSaved={() => { setShowModal(false); load(); }}
        />
      )}
    </div>
  );
}
