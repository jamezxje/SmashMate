export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  error?: string;
  timestamp: string;
}

export type Role = 'ADMIN' | 'MEMBER' | 'GUEST';
export type MemberStatus = 'ACTIVE' | 'INACTIVE' | 'LEFT';

export interface MemberResponse {
  id: number;
  fullName: string;
  phone?: string;
  email?: string;
  role: Role;
  status: MemberStatus;
  balance: number;
  joinedDate: string;
  createdAt: string;
  hasAccount: boolean;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface ScheduleResponse {
  id: number;
  dayOfWeek: number; // 1=Monday ... 7=Sunday
  startTime: string;
  endTime: string;
  venueName?: string;
  isActive: boolean;
  createdAt: string;
}
