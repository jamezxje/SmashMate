import { api } from './axios';
import type { ApiResponse, SessionResponse, SessionStatus } from '../types';

export const sessionApi = {
  getAll: (status?: SessionStatus, month?: number, year?: number) =>
    api.get<ApiResponse<SessionResponse[]>>('/sessions', {
      params: { status, month, year }
    }),
  getById: (id: number) =>
    api.get<ApiResponse<SessionResponse>>(`/sessions/${id}`),
  create: (data: { scheduleId?: number; sessionDate: string; startTime: string; endTime?: string; venueName?: string; notes?: string }) =>
    api.post<ApiResponse<SessionResponse>>('/sessions', data),
  update: (id: number, data: { sessionDate?: string; startTime?: string; endTime?: string; venueName?: string; notes?: string }) =>
    api.put<ApiResponse<SessionResponse>>(`/sessions/${id}`, data),
  updateStatus: (id: number, status: SessionStatus) =>
    api.patch<ApiResponse<SessionResponse>>(`/sessions/${id}/status`, { status }),
  delete: (id: number) =>
    api.delete<ApiResponse<void>>(`/sessions/${id}`),
};
