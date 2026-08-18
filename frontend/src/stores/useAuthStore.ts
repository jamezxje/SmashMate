import { create } from 'zustand';
import type { Role } from '../types';

interface AuthState {
  accessToken: string | null;
  role: Role | null;
  email: string | null;
  setAccessToken: (token: string) => void;
  logout: () => void;
}

function parseJwtClaim(token: string, claim: string): string | null {
  try {
    return JSON.parse(atob(token.split('.')[1]))[claim] ?? null;
  } catch {
    return null;
  }
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  role: null,
  email: null,
  setAccessToken: (token) =>
    set({
      accessToken: token,
      role: parseJwtClaim(token, 'role') as Role | null,
      email: parseJwtClaim(token, 'sub'),
    }),
  logout: () => set({ accessToken: null, role: null, email: null }),
}));
