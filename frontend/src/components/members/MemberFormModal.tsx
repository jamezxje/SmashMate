import { useState } from 'react';
import { memberApi } from '../../api/members';
import type { MemberResponse } from '../../types';

interface MemberFormModalProps {
  member: MemberResponse | null;
  onClose: () => void;
  onSaved: () => void;
}

export function MemberFormModal({ member, onClose, onSaved }: MemberFormModalProps) {
  const [fullName, setFullName] = useState(member?.fullName ?? '');
  const [phone, setPhone] = useState(member?.phone ?? '');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (member) {
        await memberApi.update(member.id, { fullName, phone });
      } else {
        await memberApi.create({
          fullName,
          phone: phone || undefined,
          email: email || undefined,
          password: password || undefined,
        });
      }
      onSaved();
    } catch (err: unknown) {
      setError('Lỗi khi lưu thông tin thành viên');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl p-6 w-full max-w-md shadow-xl">
        <h2 className="text-lg font-bold mb-4">{member ? 'Sửa thông tin' : 'Thêm thành viên mới'}</h2>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Họ tên *</label>
            <input
              id="member-fullname"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Nguyễn Văn A"
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Số điện thoại</label>
            <input
              id="member-phone"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="0900000000"
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
          {!member && (
            <>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Email (Tùy chọn - Tạo tài khoản)</label>
                <input
                  id="member-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="member@test.com"
                  className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Mật khẩu (Tùy chọn)</label>
                <input
                  id="member-password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Mật khẩu ban đầu"
                  className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                />
              </div>
            </>
          )}
          {error && <p className="text-red-500 text-xs">{error}</p>}
          <div className="flex gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 border rounded-lg py-2 text-sm hover:bg-gray-50"
            >
              Hủy
            </button>
            <button
              id="member-save-btn"
              type="submit"
              disabled={loading}
              className="flex-1 bg-green-600 text-white rounded-lg py-2 text-sm hover:bg-green-700 disabled:opacity-50 font-medium"
            >
              {loading ? 'Đang lưu...' : 'Lưu'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
