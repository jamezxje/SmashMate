import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { sessionApi } from '../api/sessions';
import type { SessionResponse, SessionAttendeeResponse, SessionTaskResponse, RsvpStatus } from '../types';
import { useAuthStore } from '../stores/useAuthStore';
import { ArrowLeft, CheckCircle, XCircle, Plus, Trash2, UserPlus, CheckSquare } from 'lucide-react';

export function SessionDetailPage() {
  const { id } = useParams();
  const sessionId = Number(id);
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [attendees, setAttendees] = useState<SessionAttendeeResponse[]>([]);
  const [tasks, setTasks] = useState<SessionTaskResponse[]>([]);
  const [newTaskTitle, setNewTaskTitle] = useState('');
  const [loading, setLoading] = useState(true);

  const { role } = useAuthStore();
  const navigate = useNavigate();
  const isAdmin = role === 'ADMIN';

  async function loadData() {
    setLoading(true);
    try {
      const [sRes, aRes, tRes] = await Promise.all([
        sessionApi.getById(sessionId),
        sessionApi.getAttendees(sessionId),
        sessionApi.getTasks(sessionId)
      ]);
      setSession(sRes.data.data);
      setAttendees(aRes.data.data);
      setTasks(tRes.data.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (sessionId) loadData();
  }, [sessionId]);

  async function handleRsvp(status: RsvpStatus) {
    await sessionApi.updateRsvp(sessionId, status);
    loadData();
  }

  async function handleCheckIn(memberId: number, current: boolean) {
    await sessionApi.checkInMember(sessionId, memberId, !current);
    loadData();
  }

  async function handleAddGuestAttendee() {
    const name = prompt('Nhập tên khách vãng lai tham gia buổi tập:');
    if (name?.trim()) {
      await sessionApi.addGuestAttendee(sessionId, name.trim());
      loadData();
    }
  }

  async function handleCreateTask(e: React.FormEvent) {
    e.preventDefault();
    if (!newTaskTitle.trim()) return;
    await sessionApi.createTask(sessionId, newTaskTitle.trim());
    setNewTaskTitle('');
    loadData();
  }

  async function handleToggleTask(taskId: number, current: boolean) {
    await sessionApi.updateTask(sessionId, taskId, !current);
    loadData();
  }

  async function handleDeleteTask(taskId: number) {
    await sessionApi.deleteTask(sessionId, taskId);
    loadData();
  }

  if (loading || !session) {
    return <div className="min-h-screen bg-slate-900 text-slate-400 p-8 text-center">Đang tải chi tiết buổi tập...</div>;
  }

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-6">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('/sessions')} className="p-2 bg-slate-800 rounded-xl hover:bg-slate-700 text-slate-300">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-white">Buổi sinh hoạt ngày {session.sessionDate}</h1>
            <p className="text-xs text-slate-400">{session.venueName || 'Chưa định địa điểm'} | {session.startTime} - {session.endTime || 'Chưa định'}</p>
          </div>
        </div>

        {/* RSVP Banner */}
        <div className="bg-slate-800 border border-slate-700/80 rounded-2xl p-5 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h3 className="text-sm font-bold text-white">Đăng ký tham gia (RSVP)</h3>
            <p className="text-xs text-slate-400">Xác nhận sự có mặt của bạn cho buổi tập này</p>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={() => handleRsvp('ATTENDING')} className="flex items-center gap-1.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 font-bold px-4 py-2 rounded-xl text-xs">
              <CheckCircle className="w-4 h-4" /> Tham gia
            </button>
            <button onClick={() => handleRsvp('ABSENT')} className="flex items-center gap-1.5 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 font-bold px-4 py-2 rounded-xl text-xs">
              <XCircle className="w-4 h-4" /> Vắng mặt
            </button>
          </div>
        </div>

        {/* Two Column Layout: Attendees & Tasks */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Attendees List */}
          <div className="bg-slate-800 border border-slate-700/80 rounded-2xl p-5 shadow-xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base font-bold text-white">Danh sách tham gia ({attendees.length})</h3>
              {isAdmin && (
                <button
                  onClick={handleAddGuestAttendee}
                  className="flex items-center gap-1 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 font-bold px-3 py-1.5 rounded-xl text-xs transition-colors"
                >
                  <UserPlus className="w-3.5 h-3.5" /> + Khách vãng lai
                </button>
              )}
            </div>
            <div className="space-y-2.5 max-h-96 overflow-y-auto pr-1">
              {attendees.map((a) => (
                <div key={a.id} className="flex items-center justify-between bg-slate-900/60 border border-slate-700/50 p-3 rounded-xl">
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="text-xs font-semibold text-white">{a.memberName}</p>
                      {a.memberRole === 'GUEST' && (
                        <span className="text-[10px] font-bold px-1.5 py-0.2 bg-amber-500/10 text-amber-400 border border-amber-500/20 rounded">GUEST</span>
                      )}
                    </div>
                    <span className={`text-[10px] font-bold ${a.rsvpStatus === 'ATTENDING' ? 'text-emerald-400' : 'text-rose-400'}`}>
                      RSVP: {a.rsvpStatus}
                    </span>
                  </div>
                  {isAdmin && (
                    <button
                      onClick={() => handleCheckIn(a.memberId, a.checkedIn)}
                      className={`text-xs px-3 py-1 rounded-lg font-bold border transition-colors ${
                        a.checkedIn ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30' : 'bg-slate-800 text-slate-400 border-slate-700'
                      }`}
                    >
                      {a.checkedIn ? '✓ Đã điểm danh' : 'Điểm danh'}
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Session Tasks */}
          <div className="bg-slate-800 border border-slate-700/80 rounded-2xl p-5 shadow-xl">
            <h3 className="text-base font-bold text-white mb-4">Checklist công việc</h3>

            {isAdmin && (
              <form onSubmit={handleCreateTask} className="flex gap-2 mb-4">
                <input
                  type="text"
                  value={newTaskTitle}
                  onChange={(e) => setNewTaskTitle(e.target.value)}
                  placeholder="Thêm việc (VD: Mua nước, Đặt sân)..."
                  className="flex-1 bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500"
                />
                <button type="submit" className="bg-emerald-500 text-slate-950 font-bold px-3 py-2 rounded-xl text-xs flex items-center gap-1">
                  <Plus className="w-4 h-4" /> Thêm
                </button>
              </form>
            )}

            <div className="space-y-2 max-h-80 overflow-y-auto pr-1">
              {tasks.map((t) => (
                <div key={t.id} className="flex items-center justify-between bg-slate-900/60 border border-slate-700/50 p-3 rounded-xl">
                  <div className="flex items-center gap-2.5">
                    <button onClick={() => isAdmin && handleToggleTask(t.id, t.isDone)} className="text-slate-400 hover:text-emerald-400">
                      <CheckSquare className={`w-5 h-5 ${t.isDone ? 'text-emerald-400' : 'text-slate-600'}`} />
                    </button>
                    <span className={`text-xs ${t.isDone ? 'line-through text-slate-500' : 'text-slate-200 font-medium'}`}>{t.title}</span>
                  </div>
                  {isAdmin && (
                    <button onClick={() => handleDeleteTask(t.id)} className="text-slate-500 hover:text-rose-400 p-1">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
