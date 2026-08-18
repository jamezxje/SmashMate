import { api } from './axios';
import type { ApiResponse, TokenResponse } from '../types';

export const authApi = {
  login: (email: string, password: string) =>
    api.post<ApiResponse<TokenResponse>>('/auth/login', { email, password }),

  refresh: () =>
    api.post<ApiResponse<TokenResponse>>('/auth/refresh'),

  changePassword: (currentPassword: string, newPassword: string) =>
    api.patch<ApiResponse<void>>('/auth/change-password', { currentPassword, newPassword }),
};
