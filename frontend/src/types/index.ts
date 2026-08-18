export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  error?: string;
  timestamp: string;
}

export type Role = 'ADMIN' | 'MEMBER' | 'GUEST';
export type MemberStatus = 'ACTIVE' | 'INACTIVE' | 'LEFT';
export type SessionStatus = 'UPCOMING' | 'IN_PROGRESS' | 'CLOSED' | 'CANCELLED';
export type RsvpStatus = 'ATTENDING' | 'ABSENT' | 'PENDING';

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

export interface SessionResponse {
  id: number;
  scheduleId?: number;
  sessionDate: string;
  startTime: string;
  endTime?: string;
  venueName?: string;
  status: SessionStatus;
  notes?: string;
  createdById: number;
  createdByName: string;
  createdAt: string;
}

export interface SessionAttendeeResponse {
  id: number;
  sessionId: number;
  memberId: number;
  memberName: string;
  memberPhone?: string;
  memberRole: Role;
  memberStatus: MemberStatus;
  rsvpStatus: RsvpStatus;
  checkedIn: boolean;
}

export interface SessionTaskResponse {
  id: number;
  sessionId: number;
  title: string;
  assignedToId?: number;
  assignedToName?: string;
  isDone: boolean;
  createdAt: string;
}
