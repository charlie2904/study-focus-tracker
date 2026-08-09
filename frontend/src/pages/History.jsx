import { useState, useEffect } from "react";
import { getSessions, deleteSession } from "@/api/endpoints";
import Layout from "@/components/Layout";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export default function History() {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [sortBy, setSortBy] = useState("date");
  const [deleting, setDeleting] = useState(null);

  async function load() {
    try {
      const res = await getSessions();
      setSessions(res.data);
    } catch {
      setError("Could not load sessions");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function handleDelete(id) {
    setDeleting(id);
    try {
      await deleteSession(id);
      setSessions((prev) => prev.filter((s) => s.id !== id));
    } catch (err) {
      setError(err.response?.data?.errors?.[0] || "Could not delete");
    } finally {
      setDeleting(null);
    }
  }

  const filtered = sessions
    .filter((s) => s.subject.toLowerCase().includes(query.toLowerCase()))
    .sort((a, b) => {
      if (sortBy === "score") return b.focusScore - a.focusScore;
      if (sortBy === "duration") return b.duration - a.duration;
      return (b.sessionDate || "").localeCompare(a.sessionDate || "");
    });

  const scoreColor = (n) =>
    n >= 75 ? "text-green-500" : n >= 50 ? "text-blue-500" : "text-muted-foreground";

  if (loading) {
    return (
      <Layout>
        <div className="h-screen grid place-items-center text-muted-foreground text-sm">
          Loading...
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="p-8 max-w-5xl">
        <h1 className="text-xl font-bold">History</h1>
        <p className="text-sm text-muted-foreground mt-1">
          {sessions.length} session{sessions.length === 1 ? "" : "s"} logged
        </p>

        {error && <p className="text-sm text-destructive mt-4">{error}</p>}

        {sessions.length === 0 ? (
          <Card className="p-10 mt-8 text-center">
            <p className="text-sm text-muted-foreground">
              Nothing here yet. Finish a session on the Timer page.
            </p>
          </Card>
        ) : (
          <>
            <div className="flex gap-2 mt-6">
              <Input placeholder="Filter by subject..." value={query}
                     onChange={(e) => setQuery(e.target.value)}
                     className="max-w-xs" />
              <div className="flex gap-1">
                {[["date","Date"],["score","Score"],["duration","Duration"]].map(([k, label]) => (
                  <Button key={k} size="sm"
                          variant={sortBy === k ? "default" : "outline"}
                          onClick={() => setSortBy(k)}>
                    {label}
                  </Button>
                ))}
              </div>
            </div>

            <div className="space-y-2 mt-5">
              {filtered.map((s) => (
                <Card key={s.id} className="p-4 flex items-center gap-5">
                  <div className="min-w-0 flex-1">
                    <div className="font-medium truncate">{s.subject}</div>
                    <div className="text-xs text-muted-foreground mt-0.5">
                      {s.sessionDate}
                      {s.startTime && ` · ${s.startTime.slice(0, 5)}`}
                      {" · "}{s.duration}m of {s.plannedDuration}m planned
                    </div>
                    {s.notes && (
                      <div className="text-xs text-muted-foreground mt-1.5 italic truncate">
                        "{s.notes}"
                      </div>
                    )}
                  </div>

                  <div className="hidden sm:flex gap-6 text-center shrink-0">
                    <div>
                      <div className="text-sm tabular-nums">{s.breaksTaken ?? 0}</div>
                      <div className="text-[10px] uppercase tracking-wider text-muted-foreground">Breaks</div>
                    </div>
                    <div>
                      <div className="text-sm tabular-nums">{s.interruptions ?? 0}</div>
                      <div className="text-[10px] uppercase tracking-wider text-muted-foreground">Interrupts</div>
                    </div>
                    <div>
                      <div className="text-sm tabular-nums">{s.focusRating}/5</div>
                      <div className="text-[10px] uppercase tracking-wider text-muted-foreground">Rating</div>
                    </div>
                  </div>

                  <div className="text-right shrink-0 w-14">
                    <div className={`text-lg font-semibold tabular-nums ${scoreColor(s.focusScore)}`}>
                      {s.focusScore?.toFixed(1)}
                    </div>
                    <div className="text-[10px] uppercase tracking-wider text-muted-foreground">Score</div>
                  </div>

                  <Button variant="ghost" size="sm"
                          className="text-muted-foreground hover:text-destructive shrink-0"
                          disabled={deleting === s.id}
                          onClick={() => handleDelete(s.id)}>
                    {deleting === s.id ? "..." : "Delete"}
                  </Button>
                </Card>
              ))}

              {filtered.length === 0 && (
                <p className="text-sm text-muted-foreground py-8 text-center">
                  No sessions match "{query}"
                </p>
              )}
            </div>
          </>
        )}
      </div>
    </Layout>
  );
}