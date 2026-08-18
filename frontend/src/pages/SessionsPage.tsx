import { useEffect, useState } from 'react';
import { sessionApi } from '../api/sessions';
import type { SessionResponse, SessionStatus } from '../types';
import { SessionFormModal } from '../components/sessions/SessionFormModal';
import { useAuthStore } from '../stores/useAuthStore';
import { Calendar, Plus, MapPin, Clock, ArrowRight, Shield, Users, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const statusBadges: Record<SessionStatus, string> = {
  UPCOMING: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  IN_PROGRESS: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 animate-pulse',
  CLOSED: 'bg-slate-700 text-slate-400 border-slate-600',
  CANCELLED: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
};

export function SessionsPage() {
  const [sessions, setSessions] = useState<SessionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editTarget, setEditTarget] = useState<SessionResponse | null>(null);

  const { role, logout } = useAuthStore();
  const navigate = useNavigate();
  const isAdmin = role === 'ADMIN';

  async function load() {
    setLoading(true);
    try {
      const { data } = await sessionApi.getAll();
      setSessions(data.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col">
      <header className="bg-slate-800/80 border-b border-slate-700/80 sticky top-0 z-40 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-emerald-500/10 border border-emerald-500/20 rounded-xl flex items-center justify-center">
              <Shield className="w-5 h-5 text-emerald-400" />
            </div>
            <h1 className="font-extrabold text-lg text-white">SmashMate</h1>
          </div>

          <div className="flex items-center gap-3">
            <button onClick={() => navigate('/members')} className="flex items-center gap-1 text-xs text-slate-300 hover:text-white px-3 py-1.5 rounded-lg bg-slate-700">
              <Users className="w-4 h-4" /> Thành viên
            </button>
            <button onClick={() => navigate('/schedules')} className="flex items-center gap-1 text-xs text-slate-300 hover:text-white px-3 py-1.5 rounded-lg bg-slate-700">
              <Calendar className="w-4 h-4" /> Lịch tập
            </button>
            <button onClick={() => { logout(); navigate('/login'); }} className="text-xs text-slate-400 hover:text-rose-400 p-1.5">
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Calendar className="w-6 h-6 text-emerald-400" /> Danh sách buổi sinh hoạt
          </h1>
          {isAdmin && (
            <button onClick={() => { setEditTarget(null); setShowModal(true); }} className="flex items-center gap-1.5 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold px-4 py-2 rounded-xl text-xs">
              <Plus className="w-4 h-4" /> + Tạo buổi tập
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {loading ? (
            <p className="text-slate-400 col-span-3 text-center py-12">Đang tải danh sách buổi tập...</p>
          ) : sessions.length === 0 ? (
            <p className="text-slate-400 col-span-3 text-center py-12">Chưa có buổi sinh hoạt nào được tạo.</p>
          ) : (
            sessions.map((s) => (
              <div key={s.id} onClick={() => navigate(`/sessions/${s.id}`)} className="bg-slate-800 border border-slate-700/80 hover:border-emerald-500/50 rounded-2xl p-5 shadow-xl transition-all cursor-pointer flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-extrabold text-white">{s.sessionDate}</span>
                    <span className={`px-2.5 py-0.5 rounded-full border text-[10px] font-bold ${statusBadges[s.status]}`}>
                      {s.status}
                    </span>
                  </div>

                  <div className="space-y-1.5 text-xs text-slate-300">
                    <div className="flex items-center gap-2">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      <span>{s.startTime} - {s.endTime || 'Chưa định'}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <MapPin className="w-3.5 h-3.5 text-slate-400" />
                      <span>{s.venueName || 'Chưa chọn sân'}</span>
                    </div>
                  </div>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-700/60 flex items-center justify-between text-xs text-emerald-400 font-semibold">
                  <span>Chi tiết & RSVP</span>
                  <ArrowRight className="w-4 h-4" />
                </div>
              </div>
            ))
          )}
        </div>
      </main>

      {showModal && (
        <SessionFormModal
          session={editTarget}
          onClose={() => setShowModal(false)}
          onSaved={() => { setShowModal(false); load(); }}
        />
      )}
    </div>
  );
}
