import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { MembersPage } from './pages/MembersPage';
import { SchedulesPage } from './pages/SchedulesPage';
import { useAuthStore } from './stores/useAuthStore';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.accessToken);
  return token ? <>{children}</> : <Navigate to="/login" replace />;
}

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/members"
          element={
            <ProtectedRoute>
              <MembersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/schedules"
          element={
            <ProtectedRoute>
              <SchedulesPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/members" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
