import { useState, useEffect } from "react";
import { getSummary, getSessions } from "@/api/endpoints";
import Layout from "@/components/Layout";
import { Card } from "@/components/ui/card";
import {
  BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Tooltip, Cell,
} from "recharts";

const DAY_ORDER = ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"];
const DAY_SHORT = { MONDAY:"Mon", TUESDAY:"Tue", WEDNESDAY:"Wed", THURSDAY:"Thu",
                    FRIDAY:"Fri", SATURDAY:"Sat", SUNDAY:"Sun" };

function dayOfWeek(iso) {
  // iso is "YYYY-MM-DD"
  const [y, m, d] = iso.split("-").map(Number);
  const idx = new Date(y, m - 1, d).getDay();   // 0 = Sunday
  return DAY_ORDER[(idx + 6) % 7];
}

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([getSummary(), getSessions()])
      .then(([s, list]) => {
        setSummary(s.data);
        setSessions(list.data);
      })
      .catch(() => setError("Could not load your data"))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <Layout>
        <div className="h-screen grid place-items-center text-muted-foreground text-sm">
          Loading...
        </div>
      </Layout>
    );
  }

  if (error) {
    return (
      <Layout>
        <div className="p-8 text-destructive text-sm">{error}</div>
      </Layout>
    );
  }

  const hrs = Math.floor(summary.totalMinutes / 60);
  const mins = summary.totalMinutes % 60;

  // ---- minutes per weekday, computed client-side from sessions ----
  const byDay = DAY_ORDER.map((day) => ({
    day: DAY_SHORT[day],
    full: day,
    minutes: sessions
      .filter((s) => s.sessionDate && dayOfWeek(s.sessionDate) === day)
      .reduce((sum, s) => sum + s.duration, 0),
  }));

  // ---- minutes per subject ----
  const subjectMap = {};
  sessions.forEach((s) => {
    subjectMap[s.subject] = (subjectMap[s.subject] || 0) + s.duration;
  });
  const bySubject = Object.entries(subjectMap)
    .map(([subject, minutes]) => ({ subject, minutes }))
    .sort((a, b) => b.minutes - a.minutes)
    .slice(0, 6);
  const maxSubject = bySubject[0]?.minutes || 1;

  const recent = [...sessions]
    .sort((a, b) => (b.sessionDate || "").localeCompare(a.sessionDate || ""))
    .slice(0, 5);

  const stats = [
    { label: "Sessions", value: summary.totalSessions },
    { label: "Total time", value: hrs > 0 ? `${hrs}h ${mins}m` : `${mins}m` },
    { label: "Avg focus score", value: summary.averageFocusScore?.toFixed(1) ?? "0.0" },
    { label: "Best day", value: summary.bestDay === "No Data" ? "—"
        : summary.bestDay.charAt(0) + summary.bestDay.slice(1).toLowerCase(), small: true },
  ];

  return (
    <Layout>
      <div className="p-8 max-w-6xl">
        <h1 className="text-xl font-bold">Dashboard</h1>
        <p className="text-sm text-muted-foreground mt-1">
          {sessions.length === 0 ? "No sessions logged yet" : `Across ${sessions.length} sessions`}
        </p>

        {sessions.length === 0 ? (
          <Card className="p-10 mt-8 text-center">
            <p className="text-sm text-muted-foreground">
              Finish a Pomodoro session and your stats will show up here.
            </p>
          </Card>
        ) : (
          <>
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mt-7">
              {stats.map((s) => (
                <Card key={s.label} className="p-4">
                  <div className="text-[10px] uppercase tracking-wider text-muted-foreground font-semibold">
                    {s.label}
                  </div>
                  <div className={`${s.small ? "text-lg" : "text-2xl"} font-semibold tabular-nums mt-2`}>
                    {s.value}
                  </div>
                </Card>
              ))}
            </div>

            <div className="grid lg:grid-cols-2 gap-4 mt-4">
              <Card className="p-5">
                <div className="text-[10px] uppercase tracking-wider text-muted-foreground font-semibold mb-4">
                  Minutes by day of week
                </div>
                <ResponsiveContainer width="100%" height={170}>
                  <BarChart data={byDay}>
                    <XAxis dataKey="day" tickLine={false} axisLine={false}
                           tick={{ fontSize: 11, fill: "currentColor", opacity: 0.5 }} />
                    <YAxis hide />
                    <Tooltip
                      cursor={{ fill: "currentColor", opacity: 0.05 }}
                      contentStyle={{
                        background: "var(--background)",
                        border: "1px solid var(--border)",
                        borderRadius: 8, fontSize: 12,
                      }}
                      formatter={(v) => [`${v} min`, "Focused"]}
                    />
                    <Bar dataKey="minutes" radius={[4, 4, 0, 0]}>
                      {byDay.map((d, i) => (
                        <Cell key={i}
                              className={d.full === summary.bestDay ? "fill-primary" : "fill-muted-foreground"}
                              opacity={d.full === summary.bestDay ? 1 : 0.35} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </Card>

              <Card className="p-5">
                <div className="text-[10px] uppercase tracking-wider text-muted-foreground font-semibold mb-4">
                  Time by subject
                </div>
                <div className="space-y-3">
                  {bySubject.map((s) => (
                    <div key={s.subject} className="grid grid-cols-[80px_1fr_56px] items-center gap-3 text-sm">
                      <span className="truncate">{s.subject}</span>
                      <div className="h-2 bg-muted rounded-full overflow-hidden">
                        <div className="h-full bg-primary rounded-full"
                             style={{ width: `${(s.minutes / maxSubject) * 100}%` }} />
                      </div>
                      <span className="text-xs text-muted-foreground tabular-nums text-right">
                        {s.minutes}m
                      </span>
                    </div>
                  ))}
                </div>
              </Card>
            </div>

            <Card className="mt-4 overflow-hidden">
              <div className="px-5 pt-5 pb-3 text-[10px] uppercase tracking-wider text-muted-foreground font-semibold">
                Recent sessions
              </div>
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    {["Subject","Date","Start","Focused","Breaks","Rating","Score"].map((h) => (
                      <th key={h} className="text-left text-[10px] uppercase tracking-wider
                                             text-muted-foreground font-semibold px-5 py-2">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {recent.map((s) => (
                    <tr key={s.id} className="border-b last:border-0">
                      <td className="px-5 py-3 text-sm font-medium">{s.subject}</td>
                      <td className="px-5 py-3 text-sm text-muted-foreground tabular-nums">{s.sessionDate}</td>
                      <td className="px-5 py-3 text-sm text-muted-foreground tabular-nums">
                        {s.startTime ? s.startTime.slice(0, 5) : "—"}
                      </td>
                      <td className="px-5 py-3 text-sm tabular-nums">{s.duration}m</td>
                      <td className="px-5 py-3 text-sm tabular-nums text-muted-foreground">
                        {s.breaksTaken ?? 0}
                      </td>
                      <td className="px-5 py-3 text-sm tabular-nums text-muted-foreground">
                        {s.focusRating}/5
                      </td>
                      <td className="px-5 py-3 text-sm font-semibold tabular-nums">
                        {s.focusScore?.toFixed(1)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Card>
          </>
        )}
      </div>
    </Layout>
  );
}