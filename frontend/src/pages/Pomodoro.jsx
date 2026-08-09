import { useState, useEffect, useCallback, useRef } from "react";
import {
  getActivePomodoro, startPomodoro, pausePomodoro,
  resumePomodoro, startBreak, completePomodoro, abandonPomodoro,
  getSessions,
} from "@/api/endpoints";
import Layout from "@/components/Layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card } from "@/components/ui/card";

function fmt(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds));
  const m = Math.floor(s / 60);
  return `${String(m).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
}

function todayISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

const CYCLE_LENGTH = 4;

export default function Pomodoro() {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [subject, setSubject] = useState("");
  const [focusMinutes, setFocusMinutes] = useState(25);
  const [error, setError] = useState("");
  const [rating, setRating] = useState(4);
  const [notes, setNotes] = useState("");
  const [finishing, setFinishing] = useState(false);
  const [today, setToday] = useState({ count: 0, minutes: 0 });

  const notifiedRef = useRef(false);

  // ---- today's totals, from completed sessions ----
  const loadToday = useCallback(async () => {
    try {
      const res = await getSessions();
      const iso = todayISO();
      const mine = res.data.filter((s) => s.sessionDate === iso);
      setToday({
        count: mine.length,
        minutes: mine.reduce((sum, s) => sum + s.duration, 0),
      });
    } catch {
      /* non-critical */
    }
  }, []);

  // ---- server state ----
  const refresh = useCallback(async () => {
    try {
      const res = await getActivePomodoro();
      setSession(res.data);
    } catch (err) {
      if (err.response?.status === 404) setSession(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
    loadToday();
    if ("Notification" in window && Notification.permission === "default") {
      Notification.requestPermission();
    }
  }, [refresh, loadToday]);

  // ---- local tick + periodic re-sync ----
  useEffect(() => {
    if (!session || session.status === "PAUSED") return;

    const tick = setInterval(() => {
      setSession((prev) => {
        if (!prev || prev.status === "PAUSED") return prev;
        if (prev.status === "ON_BREAK") {
          return { ...prev, currentBreakSeconds: prev.currentBreakSeconds + 1 };
        }
        return {
          ...prev,
          elapsedFocusSeconds: prev.elapsedFocusSeconds + 1,
          remainingFocusSeconds: Math.max(0, prev.remainingFocusSeconds - 1),
        };
      });
    }, 1000);

    const sync = setInterval(refresh, 15000);
    return () => { clearInterval(tick); clearInterval(sync); };
  }, [session?.status, refresh]);

  // ---- tab title ----
  useEffect(() => {
    if (!session) {
      document.title = "FocusTrack";
      return;
    }
    const t = session.status === "ON_BREAK"
      ? fmt(session.currentBreakSeconds)
      : fmt(session.remainingFocusSeconds);
    const tag = session.status === "ON_BREAK" ? "break"
              : session.status === "PAUSED"   ? "paused"
              : session.subject;
    document.title = `${t} · ${tag}`;
  }, [session?.remainingFocusSeconds, session?.currentBreakSeconds, session?.status, session?.subject, session]);

  // ---- notify once when the target is reached ----
  useEffect(() => {
    if (!session || session.status !== "RUNNING") return;
    if (session.remainingFocusSeconds > 0) { notifiedRef.current = false; return; }
    if (notifiedRef.current) return;

    notifiedRef.current = true;
    if ("Notification" in window && Notification.permission === "granted") {
      new Notification("Session complete", {
        body: `${session.focusMinutes} minutes on ${session.subject}. Time for a break.`,
      });
    }
  }, [session?.remainingFocusSeconds, session?.status, session]);

  async function act(fn) {
    setError("");
    try {
      const res = await fn();
      setSession(res.data);
    } catch (err) {
      setError(err.response?.data?.errors?.[0] || "Something went wrong");
    }
  }

  async function handleStart(e) {
    e.preventDefault();
    await act(() => startPomodoro(subject, Number(focusMinutes)));
  }

  async function handleComplete() {
    try {
      await completePomodoro(Number(rating), notes);
      setSession(null);
      setFinishing(false);
      setNotes("");
      loadToday();
    } catch (err) {
      setError(err.response?.data?.errors?.[0] || "Could not save session");
    }
  }

  async function handleAbandon() {
    await abandonPomodoro();
    setSession(null);
    setFinishing(false);
  }

  const hrs = Math.floor(today.minutes / 60);
  const mins = today.minutes % 60;

  // ================= LOADING =================
  if (loading) {
    return (
      <Layout>
        <div className="h-screen grid place-items-center text-muted-foreground text-sm">
          Loading…
        </div>
      </Layout>
    );
  }

  // ================= START FORM =================
  if (!session) {
    return (
      <Layout>
        <div className="p-8 max-w-5xl">
          <h1 className="text-xl font-bold">Start a session</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Pick a subject and a target. The server keeps the clock.
          </p>

          <div className="grid lg:grid-cols-[1fr_260px] gap-6 mt-8">
            <form onSubmit={handleStart} className="space-y-4 max-w-sm">
              <div className="space-y-2">
                <Label htmlFor="subject">Subject</Label>
                <Input id="subject" value={subject} required autoComplete="off"
                       onChange={(e) => setSubject(e.target.value)}
                       placeholder="Data Structures" />
              </div>

              <div className="space-y-2">
                <Label htmlFor="minutes">Focus minutes</Label>
                <div className="flex gap-2">
                  {[15, 25, 45, 60].map((n) => (
                    <Button key={n} type="button"
                            variant={Number(focusMinutes) === n ? "default" : "outline"}
                            className="flex-1"
                            onClick={() => setFocusMinutes(n)}>
                      {n}
                    </Button>
                  ))}
                </div>
                <Input id="minutes" type="number" min="1" max="180"
                       value={focusMinutes}
                       onChange={(e) => setFocusMinutes(e.target.value)} />
              </div>

              {error && <p className="text-sm text-destructive">{error}</p>}

              <Button type="submit" className="w-full">Start session</Button>
            </form>

            <div className="space-y-3">
              <Card className="p-4">
                <div className="text-[10px] uppercase tracking-wider text-muted-foreground font-semibold">
                  Today
                </div>
                <div className="text-2xl font-semibold tabular-nums mt-2">
                  {hrs > 0 ? `${hrs}h ` : ""}{mins}m
                </div>
                <div className="text-xs text-muted-foreground mt-1">
                  across {today.count} session{today.count === 1 ? "" : "s"}
                </div>
              </Card>

              <Card className="p-4">
                <div className="text-[10px] uppercase tracking-wider text-muted-foreground font-semibold">
                  Cycle
                </div>
                <div className="flex gap-1.5 mt-3">
                  {Array.from({ length: CYCLE_LENGTH }).map((_, i) => (
                    <span key={i}
                          className={`h-2.5 w-2.5 rounded-full ${
                            i < today.count % CYCLE_LENGTH ? "bg-primary" : "bg-muted"
                          }`} />
                  ))}
                </div>
                <div className="text-xs text-muted-foreground mt-3">
                  {CYCLE_LENGTH - (today.count % CYCLE_LENGTH)} until a long break
                </div>
              </Card>
            </div>
          </div>
        </div>
      </Layout>
    );
  }

  // ================= FINISH DIALOG =================
  if (finishing) {
    return (
      <Layout>
        <div className="p-8 max-w-md">
          <h1 className="text-xl font-bold">How did that go?</h1>
          <p className="text-sm text-muted-foreground mt-1">
            {session.subject} · {Math.round(session.elapsedFocusSeconds / 60)} min focused
            {session.breaksTaken > 0 && ` · ${session.breaksTaken} break${session.breaksTaken > 1 ? "s" : ""}`}
          </p>

          <div className="space-y-5 mt-7">
            <div className="space-y-2">
              <Label>Focus rating</Label>
              <div className="flex gap-2">
                {[1, 2, 3, 4, 5].map((n) => (
                  <Button key={n} type="button"
                          variant={rating === n ? "default" : "outline"}
                          className="flex-1" onClick={() => setRating(n)}>
                    {n}
                  </Button>
                ))}
              </div>
              <p className="text-xs text-muted-foreground">
                {["Constantly distracted", "Struggled to focus", "Average",
                  "Focused, minor distractions", "Deep focus throughout"][rating - 1]}
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="notes">Notes (optional)</Label>
              <Input id="notes" value={notes} autoComplete="off"
                     onChange={(e) => setNotes(e.target.value)}
                     placeholder="What helped or got in the way?" />
            </div>

            <Card className="p-4 flex items-center justify-between">
              <span className="text-sm text-muted-foreground">Projected focus score</span>
              <span className="text-xl font-semibold tabular-nums">
                {(
                  Math.round(
                    (Math.max(1, Math.round(session.elapsedFocusSeconds / 60)) /
                      session.focusMinutes) * rating * 20 * 100
                  ) / 100
                ).toFixed(1)}
              </span>
            </Card>

            {error && <p className="text-sm text-destructive">{error}</p>}

            <div className="flex gap-2">
              <Button className="flex-1" onClick={handleComplete}>Save session</Button>
              <Button variant="ghost" onClick={() => setFinishing(false)}>Back</Button>
            </div>
          </div>
        </div>
      </Layout>
    );
  }

  // ================= ACTIVE TIMER =================
  const onBreak = session.status === "ON_BREAK";
  const paused  = session.status === "PAUSED";
  const done    = session.remainingFocusSeconds === 0 && !onBreak;

  const total = session.focusMinutes * 60;
  const progress = total > 0 ? (total - session.remainingFocusSeconds) / total : 0;
  const R = 110;
  const CIRC = 2 * Math.PI * R;

  return (
    <Layout>
      <div className="p-8">
        <div className="flex items-start justify-between max-w-5xl">
          <div>
            <h1 className="text-xl font-bold">Focus session</h1>
            <p className="text-sm text-muted-foreground mt-1">
              Session {(today.count % CYCLE_LENGTH) + 1} of {CYCLE_LENGTH} in this cycle
            </p>
          </div>
        </div>

        <div className="grid lg:grid-cols-[1fr_260px] gap-8 mt-8 max-w-5xl">
          {/* timer */}
          <div className="flex flex-col items-center">
            <div className="text-sm text-muted-foreground border rounded-full px-4 py-1.5">
              {onBreak
                ? "Break time"
                : <>Studying <span className="font-medium text-foreground">{session.subject}</span></>}
            </div>

            <div className="relative mt-6">
              <svg width="250" height="250" className="-rotate-90">
                <circle cx="125" cy="125" r={R} fill="none" strokeWidth="9"
                        className="stroke-muted" />
                <circle cx="125" cy="125" r={R} fill="none" strokeWidth="9"
                        strokeLinecap="round"
                        strokeDasharray={CIRC}
                        strokeDashoffset={CIRC * (1 - progress)}
                        className={`transition-all duration-1000 ease-linear ${
                          onBreak ? "stroke-green-500" : "stroke-primary"
                        }`} />
              </svg>
              <div className="absolute inset-0 grid place-content-center text-center">
                <div className={`text-5xl font-semibold tabular-nums tracking-tight ${
                  paused ? "opacity-50" : ""
                }`}>
                  {onBreak ? fmt(session.currentBreakSeconds) : fmt(session.remainingFocusSeconds)}
                </div>
                <div className={`text-[10px] uppercase tracking-[0.16em] mt-1.5 font-semibold ${
                  onBreak ? "text-green-500" : paused ? "text-muted-foreground" : "text-primary"
                }`}>
                  {onBreak ? "On break" : paused ? "Paused" : done ? "Target reached" : "Focusing"}
                </div>
              </div>
            </div>

            {error && <p className="text-sm text-destructive mt-4">{error}</p>}

            <div className="flex gap-2 mt-7">
              {onBreak || paused ? (
                <Button onClick={() => act(resumePomodoro)}>
                  {onBreak ? "Back to work" : "Resume"}
                </Button>
              ) : (
                <>
                  <Button onClick={() => act(pausePomodoro)}>Pause</Button>
                  <Button variant="outline" onClick={() => act(startBreak)}>Take a break</Button>
                </>
              )}
              <Button variant="ghost" onClick={() => setFinishing(true)}>Finish</Button>
              <Button variant="ghost"
                      className="text-muted-foreground"
                      onClick={handleAbandon}>Cancel</Button>
            </div>

            <div className="flex gap-1.5 mt-6">
              {Array.from({ length: CYCLE_LENGTH }).map((_, i) => {
                const idx = today.count % CYCLE_LENGTH;
                return (
                  <span key={i}
                        className={`h-2.5 w-2.5 rounded-full ${
                          i < idx ? "bg-primary"
                          : i === idx ? "bg-primary ring-4 ring-primary/20"
                          : "bg-muted"
                        }`} />
                );
              })}
            </div>
          </div>

          {/* side panel */}
           <div className="space-y-2.5 self-start">
            {[
              ["Focused", fmt(session.elapsedFocusSeconds)],
              ["Breaks", session.breaksTaken],
              ["Interruptions", session.interruptions],
              ["Paused", fmt(session.totalPausedSeconds)],
            ].map(([label, value]) => (
              <Card key={label} className="px-4 py-3 flex items-center justify-between">
                <span className="text-sm text-muted-foreground">{label}</span>
                <span className="font-semibold tabular-nums">{value}</span>
              </Card>
            ))}

            <Card className="p-4">
              <div className="text-[10px] uppercase tracking-wider text-muted-foreground font-semibold">
                Today
              </div>
              <div className="text-2xl font-semibold tabular-nums mt-2">
                {hrs > 0 ? `${hrs}h ` : ""}{mins}m
              </div>
              <div className="text-xs text-muted-foreground mt-1">
                across {today.count} session{today.count === 1 ? "" : "s"}
              </div>
            </Card>
          </div>
        </div>
      </div>
    </Layout>
  );
}