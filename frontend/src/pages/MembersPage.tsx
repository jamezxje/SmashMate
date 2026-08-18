import { useEffect, useState } from 'react';
import { memberApi } from '../api/members';
import type { MemberResponse } from '../types';
import { MemberFormModal } from '../components/members/MemberFormModal';
import { formatVND } from '../utils/formatCurrency';

export function MembersPage() {
  const [members, setMembers] = useState<MemberResponse[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [editTarget, setEditTarget] = useState<MemberResponse | null>(null);

  async function load() {
    const { data } = await memberApi.getAll();
    setMembers(data.data);
  }

  useEffect(() => {
    load();
  }, []);

  async function handleDelete(id: number) {
    if (!confirm('Bạn có chắc chắn muốn xóa thành viên này?')) return;
    await memberApi.delete(id);
    load();
  }

  async function handleAddGuest() {
    const name = prompt('Tên khách vãng lai:');
    if (name?.trim()) {
      await memberApi.createGuest(name.trim());
      load();
    }
  }

  const roleBadge: Record<string, string> = {
    ADMIN: 'bg-purple-100 text-purple-700 border-purple-200',
    MEMBER: 'bg-blue-100 text-blue-700 border-blue-200',
    GUEST: 'bg-amber-100 text-amber-700 border-amber-200',
  };

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Danh Sách Thành Viên</h1>
          <p className="text-sm text-gray-500">Quản lý thành viên, khách vãng lai và phân quyền</p>
        </div>
        <div className="flex gap-2">
          <button
            id="add-guest-btn"
            onClick={handleAddGuest}
            className="px-4 py-2 bg-amber-50 text-amber-700 border border-amber-200 rounded-lg hover:bg-amber-100 text-sm font-medium transition"
          >
            + Thêm Khách
          </button>
          <button
            id="add-member-btn"
            onClick={() => {
              setEditTarget(null);
              setShowModal(true);
            }}
            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 text-sm font-medium transition shadow-sm"
          >
            + Thêm Thành Viên
          </button>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        <table className="w-full text-sm border-collapse text-left">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200 text-gray-600 font-semibold">
              <th className="p-4">Tên</th>
              <th className="p-4">SĐT</th>
              <th className="p-4">Vai Trò</th>
              <th className="p-4">Trạng Thái</th>
              <th className="p-4">Số Dư (Balance)</th>
              <th className="p-4">Tài Khoản</th>
              <th className="p-4 text-right">Thao Tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {members.map((m) => (
              <tr key={m.id} className="hover:bg-gray-50/80 transition">
                <td className="p-4 font-medium text-gray-900">{m.fullName}</td>
                <td className="p-4 text-gray-600">{m.phone ?? '—'}</td>
                <td className="p-4">
                  <span className={`inline-block px-2.5 py-1 rounded-full text-xs font-semibold border ${roleBadge[m.role]}`}>
                    {m.role}
                  </span>
                </td>
                <td className="p-4">
                  <span className={`inline-block px-2.5 py-1 rounded-full text-xs font-semibold ${
                    m.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                  }`}>
                    {m.status}
                  </span>
                </td>
                <td className="p-4 font-medium text-gray-700">
                  {formatVND(m.balance)}
                </td>
                <td className="p-4">
                  {m.hasAccount ? (
                    <span className="text-green-600 text-xs font-semibold">✅ Có</span>
                  ) : (
                    <span className="text-gray-400 text-xs">Offline</span>
                  )}
                </td>
                <td className="p-4 text-right space-x-3">
                  <button
                    onClick={() => {
                      setEditTarget(m);
                      setShowModal(true);
                    }}
                    className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                  >
                    Sửa
                  </button>
                  <button
                    onClick={() => handleDelete(m.id)}
                    className="text-red-600 hover:text-red-800 text-xs font-medium"
                  >
                    Xóa
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showModal && (
        <MemberFormModal
          member={editTarget}
          onClose={() => setShowModal(false)}
          onSaved={() => {
            setShowModal(false);
            load();
          }}
        />
      )}
    </div>
  );
}
