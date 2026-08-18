import { api } from './axios';
import type { ApiResponse, SessionResponse, SessionStatus, SessionAttendeeResponse, SessionTaskResponse, RsvpStatus } from '../types';

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

  getAttendees: (sessionId: number) =>
    api.get<ApiResponse<SessionAttendeeResponse[]>>(`/sessions/${sessionId}/attendees`),
  updateRsvp: (sessionId: number, rsvpStatus: RsvpStatus) =>
    api.patch<ApiResponse<SessionAttendeeResponse>>(`/sessions/${sessionId}/rsvp`, { rsvpStatus }),
  checkInMember: (sessionId: number, memberId: number, checkedIn: boolean) =>
    api.patch<ApiResponse<SessionAttendeeResponse>>(`/sessions/${sessionId}/attendees/${memberId}/checkin`, { checkedIn }),
  addGuestAttendee: (sessionId: number, fullName: string) =>
    api.post<ApiResponse<SessionAttendeeResponse>>(`/sessions/${sessionId}/attendees/guest`, { fullName }),

  getTasks: (sessionId: number) =>
    api.get<ApiResponse<SessionTaskResponse[]>>(`/sessions/${sessionId}/tasks`),
  createTask: (sessionId: number, title: string, assignedToId?: number) =>
    api.post<ApiResponse<SessionTaskResponse>>(`/sessions/${sessionId}/tasks`, { title, assignedToId }),
  updateTask: (sessionId: number, taskId: number, isDone: boolean) =>
    api.patch<ApiResponse<SessionTaskResponse>>(`/sessions/${sessionId}/tasks/${taskId}`, { isDone }),
  deleteTask: (sessionId: number, taskId: number) =>
    api.delete<ApiResponse<void>>(`/sessions/${sessionId}/tasks/${taskId}`),
};
