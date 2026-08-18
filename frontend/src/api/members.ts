import { api } from './axios';
import type { ApiResponse, MemberResponse, Role } from '../types';

export const memberApi = {
  getAll: (role?: Role) =>
    api.get<ApiResponse<MemberResponse[]>>('/members', { params: role ? { role } : {} }),
  create: (data: { fullName: string; phone?: string; email?: string; password?: string }) =>
    api.post<ApiResponse<MemberResponse>>('/members', data),
  createGuest: (fullName: string) =>
    api.post<ApiResponse<MemberResponse>>('/members/guests', { fullName }),
  update: (id: number, data: { fullName?: string; phone?: string }) =>
    api.put<ApiResponse<MemberResponse>>(`/members/${id}`, data),
  assignAccount: (id: number, email: string, password: string) =>
    api.patch<ApiResponse<MemberResponse>>(`/members/${id}/account`, { email, password }),
  updateStatus: (id: number, status: string) =>
    api.patch<ApiResponse<MemberResponse>>(`/members/${id}/status`, { status }),
  delete: (id: number) => api.delete<ApiResponse<void>>(`/members/${id}`),
};
