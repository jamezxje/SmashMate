import { useEffect, useState } from 'react';
import { memberApi } from '../api/members';
import type { MemberResponse, Role } from '../types';
import { MemberFormModal } from '../components/members/MemberFormModal';
import { AssignAccountModal } from '../components/members/AssignAccountModal';
import { useAuthStore } from '../stores/useAuthStore';
import { formatVND } from '../utils/formatCurrency';
import {
  UserPlus,
  UserCheck,
  Shield,
  Trash2,
  Edit,
  Key,
  LogOut,
  Search,
  Filter,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export function MembersPage() {
  const [members, setMembers] = useState<MemberResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showFormModal, setShowFormModal] = useState(false);
  const [editTarget, setEditTarget] = useState<MemberResponse | null>(null);
  const [assignTarget, setAssignTarget] = useState<MemberResponse | null>(null);
  const [roleFilter, setRoleFilter] = useState<Role | 'ALL'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const { role: userRole, logout } = useAuthStore();
  const navigate = useNavigate();
  const isAdmin = userRole === 'ADMIN';

  async function load() {
    setLoading(true);
    try {
      const filter = roleFilter === 'ALL' ? undefined : roleFilter;
      const { data } = await memberApi.getAll(filter);
      setMembers(data.data);
    } catch {
      // Error handling handled by axios interceptors
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [roleFilter]);

  async function handleDelete(id: number, name: string) {
    if (!confirm(`Bạn có chắc chắn muốn xóa thành viên "${name}"?`)) return;
    try {
      await memberApi.delete(id);
      load();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể xóa thành viên');
    }
  }

  async function handleAddGuest() {
    const name = prompt('Nhập tên khách vãng lai (GUEST):');
    if (name?.trim()) {
      try {
        await memberApi.createGuest(name.trim());
        load();
      } catch (err: any) {
        alert(err.response?.data?.message || 'Không thể tạo khách vãng lai');
      }
    }
  }

  function handleLogout() {
    logout();
    navigate('/login');
  }

  const filteredMembers = members.filter((m) =>
    m.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (m.email && m.email.toLowerCase().includes(searchTerm.toLowerCase())) ||
    (m.phone && m.phone.includes(searchTerm))
  );

  const roleBadges: Record<Role, string> = {
    ADMIN: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
    MEMBER: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
    GUEST: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col">
      {/* Header */}
      <header className="bg-slate-800/80 border-b border-slate-700/80 backdrop-blur-md sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-emerald-500/10 border border-emerald-500/20 rounded-xl flex items-center justify-center">
              <Shield className="w-5 h-5 text-emerald-400" />
            </div>
            <div>
              <h1 className="font-extrabold text-lg text-white leading-tight">SmashMate</h1>
              <p className="text-[11px] text-slate-400 font-medium">Quản lý thành viên câu lạc bộ</p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-700 text-slate-300 border border-slate-600">
              Role: <strong className="text-emerald-400">{userRole}</strong>
            </span>
            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 text-xs font-medium text-slate-400 hover:text-rose-400 transition-colors py-1.5 px-3 rounded-lg hover:bg-slate-700/50"
            >
              <LogOut className="w-4 h-4" />
              Đăng xuất
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Actions Bar */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div className="flex flex-wrap items-center gap-3">
            <div className="relative flex-1 sm:w-64">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm theo tên, email, SĐT..."
                className="w-full bg-slate-800 border border-slate-700 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500"
              />
            </div>

            <div className="flex items-center gap-1.5 bg-slate-800 border border-slate-700 rounded-xl p-1">
              <Filter className="w-3.5 h-3.5 text-slate-400 ml-2" />
              {(['ALL', 'ADMIN', 'MEMBER', 'GUEST'] as const).map((r) => (
                <button
                  key={r}
                  onClick={() => setRoleFilter(r)}
                  className={`text-xs px-3 py-1 rounded-lg font-medium transition-all ${
                    roleFilter === r
                      ? 'bg-emerald-500 text-slate-950 font-bold'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {r === 'ALL' ? 'Tất cả' : r}
                </button>
              ))}
            </div>
          </div>

          {isAdmin && (
            <div className="flex items-center gap-2">
              <button
                id="add-guest-btn"
                onClick={handleAddGuest}
                className="flex items-center gap-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-amber-400 font-semibold px-4 py-2 rounded-xl text-xs transition-all shadow-sm"
              >
                <UserCheck className="w-4 h-4" />
                + Thêm khách vãng lai
              </button>
              <button
                id="add-member-btn"
                onClick={() => {
                  setEditTarget(null);
                  setShowFormModal(true);
                }}
                className="flex items-center gap-1.5 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold px-4 py-2 rounded-xl text-xs transition-all shadow-lg shadow-emerald-500/10"
              >
                <UserPlus className="w-4 h-4" />
                + Thêm thành viên
              </button>
            </div>
          )}
        </div>

        {/* Table List */}
        <div className="bg-slate-800 border border-slate-700/80 rounded-2xl shadow-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/60 text-slate-400 uppercase font-semibold border-b border-slate-700/80">
                <tr>
                  <th className="p-4">Họ & Tên</th>
                  <th className="p-4">Số điện thoại</th>
                  <th className="p-4">Vai trò</th>
                  <th className="p-4">Trạng thái</th>
                  <th className="p-4">Số dư ví</th>
                  <th className="p-4">Tài khoản</th>
                  {isAdmin && <th className="p-4 text-right">Thao tác</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700/50">
                {loading ? (
                  <tr>
                    <td colSpan={isAdmin ? 7 : 6} className="p-8 text-center text-slate-400">
                      Đang tải danh sách thành viên...
                    </td>
                  </tr>
                ) : filteredMembers.length === 0 ? (
                  <tr>
                    <td colSpan={isAdmin ? 7 : 6} className="p-8 text-center text-slate-400">
                      Chưa có thành viên nào trong danh sách.
                    </td>
                  </tr>
                ) : (
                  filteredMembers.map((m) => (
                    <tr key={m.id} className="hover:bg-slate-700/30 transition-colors">
                      <td className="p-4 font-semibold text-white">
                        <div className="flex items-center gap-2.5">
                          <div className="w-8 h-8 rounded-full bg-slate-700 flex items-center justify-center text-slate-300 font-bold text-xs">
                            {m.fullName.charAt(0).toUpperCase()}
                          </div>
                          <div>
                            <div>{m.fullName}</div>
                            {m.email && <div className="text-[11px] text-slate-400 font-normal">{m.email}</div>}
                          </div>
                        </div>
                      </td>
                      <td className="p-4 text-slate-300">{m.phone || '—'}</td>
                      <td className="p-4">
                        <span className={`px-2.5 py-1 rounded-lg border text-[11px] font-bold ${roleBadges[m.role]}`}>
                          {m.role}
                        </span>
                      </td>
                      <td className="p-4">
                        <span
                          className={`px-2.5 py-1 rounded-lg border text-[11px] font-bold ${
                            m.status === 'ACTIVE'
                              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                              : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                          }`}
                        >
                          {m.status}
                        </span>
                      </td>
                      <td className="p-4 font-medium text-emerald-400">
                        {formatVND(m.balance)}
                      </td>
                      <td className="p-4">
                        {m.hasAccount ? (
                          <span className="text-emerald-400 font-semibold flex items-center gap-1">
                            ✓ Có tài khoản
                          </span>
                        ) : m.role === 'GUEST' ? (
                          <span className="text-slate-500">Khách ẩn</span>
                        ) : (
                          <span className="text-amber-400 font-medium">Chưa có (Member Offline)</span>
                        )}
                      </td>

                      {isAdmin && (
                        <td className="p-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            {!m.hasAccount && m.role !== 'GUEST' && (
                              <button
                                title="Cấp tài khoản đăng nhập"
                                onClick={() => setAssignTarget(m)}
                                className="p-1.5 text-amber-400 hover:bg-amber-400/10 rounded-lg transition-colors"
                              >
                                <Key className="w-4 h-4" />
                              </button>
                            )}

                            <button
                              title="Sửa thông tin"
                              onClick={() => {
                                setEditTarget(m);
                                setShowFormModal(true);
                              }}
                              className="p-1.5 text-blue-400 hover:bg-blue-400/10 rounded-lg transition-colors"
                            >
                              <Edit className="w-4 h-4" />
                            </button>

                            <button
                              title="Xóa thành viên"
                              onClick={() => handleDelete(m.id, m.fullName)}
                              className="p-1.5 text-rose-400 hover:bg-rose-400/10 rounded-lg transition-colors"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>

      {/* Modals */}
      {showFormModal && (
        <MemberFormModal
          member={editTarget}
          onClose={() => setShowFormModal(false)}
          onSaved={() => {
            setShowFormModal(false);
            load();
          }}
        />
      )}

      {assignTarget && (
        <AssignAccountModal
          member={assignTarget}
          onClose={() => setAssignTarget(null)}
          onSaved={() => {
            setAssignTarget(null);
            load();
          }}
        />
      )}
    </div>
  );
}
