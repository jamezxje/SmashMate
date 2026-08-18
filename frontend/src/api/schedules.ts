import { api } from './axios';
import type { ApiResponse, ScheduleResponse } from '../types';

export const scheduleApi = {
  getAll: () =>
    api.get<ApiResponse<ScheduleResponse[]>>('/schedules'),
  create: (data: { dayOfWeek: number; startTime: string; endTime: string; venueName?: string }) =>
    api.post<ApiResponse<ScheduleResponse>>('/schedules', data),
  update: (id: number, data: { dayOfWeek?: number; startTime?: string; endTime?: string; venueName?: string }) =>
    api.put<ApiResponse<ScheduleResponse>>(`/schedules/${id}`, data),
  toggleActive: (id: number) =>
    api.patch<ApiResponse<ScheduleResponse>>(`/schedules/${id}/toggle`),
  delete: (id: number) =>
    api.delete<ApiResponse<void>>(`/schedules/${id}`),
};
