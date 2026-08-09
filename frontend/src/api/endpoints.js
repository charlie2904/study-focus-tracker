import api from "./client";

// ===== AUTH =====
export const login = (username, password) =>
  api.post("/auth/login", { username, password });

export const signup = (username, password) =>
  api.post("/auth/signup", { username, password });

// ===== SESSIONS =====
export const getSessions = () => api.get("/sessions");

export const deleteSession = (id) => api.delete(`/sessions/${id}`);

// ===== ANALYTICS =====
export const getSummary = () => api.get("/analytics/summary");

// ===== POMODORO =====
export const startPomodoro = (subject, focusMinutes) =>
  api.post("/pomodoro/start", { subject, focusMinutes });

export const getActivePomodoro = () => api.get("/pomodoro/active");

export const pausePomodoro = () => api.post("/pomodoro/pause");

export const resumePomodoro = () => api.post("/pomodoro/resume");

export const startBreak = () => api.post("/pomodoro/break");

export const completePomodoro = (focusRating, notes) =>
  api.post("/pomodoro/complete", { focusRating, notes });

export const abandonPomodoro = () => api.post("/pomodoro/abandon");