import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { login } from "@/api/endpoints";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await login(username, password);
      localStorage.setItem("token", res.data.token);
      navigate("/timer");
    } catch (err) {
      const msg = err.response?.data?.errors?.[0] || "Something went wrong";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-2 bg-background">
      {/* Left - brand panel */}
      <div className="hidden lg:flex flex-col justify-between p-10 border-r bg-muted/30">
        <div className="flex items-center gap-2 font-semibold">
          <span className="h-6 w-6 rounded-md bg-primary" />
          FocusTrack
        </div>

        <div className="flex justify-center">
          <svg width="180" height="180" className="-rotate-90">
            <circle cx="90" cy="90" r="76" fill="none" strokeWidth="8"
                    className="stroke-muted" />
            <circle cx="90" cy="90" r="76" fill="none" strokeWidth="8"
                    strokeLinecap="round" strokeDasharray="477" strokeDashoffset="140"
                    className="stroke-primary" />
          </svg>
        </div>

        <div>
          <h3 className="text-2xl font-bold leading-tight">
            Focus is a<br />measurable habit.
          </h3>
          <p className="text-sm text-muted-foreground mt-3 max-w-xs">
            Track every session, see when you actually concentrate, and let the
            data pick your study hours.
          </p>
        </div>
      </div>

      {/* Right - form */}
      <div className="flex items-center justify-center p-8">
        <form onSubmit={handleSubmit} className="w-full max-w-sm space-y-4">
          <div className="space-y-1">
            <h2 className="text-2xl font-bold">Welcome back</h2>
            <p className="text-sm text-muted-foreground">
              Log in to pick up where you left off.
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="username">Username</Label>
            <Input
              id="username"
              autoComplete="off"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="rishabh"
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          {error && (
            <p className="text-sm text-destructive">{error}</p>
          )}

          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? "Logging in..." : "Log in"}
          </Button>

          <p className="text-sm text-muted-foreground text-center">
            No account yet?{" "}
            <Link to="/signup" className="text-primary hover:underline">
              Create one
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}