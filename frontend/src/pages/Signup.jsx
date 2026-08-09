import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { signup } from "@/api/endpoints";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function Signup() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setErrors([]);
    setLoading(true);
    try {
      const res = await signup(username, password);
      localStorage.setItem("token", res.data.token);
      navigate("/timer");
    } catch (err) {
      setErrors(err.response?.data?.errors || ["Something went wrong"]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-2 bg-background">
      <div className="hidden lg:flex flex-col justify-between p-10 border-r bg-muted/30">
        <div className="flex items-center gap-2 font-semibold">
          <span className="h-6 w-6 rounded-md bg-primary" />
          FocusTrack
        </div>
        <div className="flex justify-center">
          <svg width="180" height="180" className="-rotate-90">
            <circle cx="90" cy="90" r="76" fill="none" strokeWidth="8" className="stroke-muted" />
            <circle cx="90" cy="90" r="76" fill="none" strokeWidth="8"
                    strokeLinecap="round" strokeDasharray="477" strokeDashoffset="330"
                    className="stroke-primary" />
          </svg>
        </div>
        <div>
          <h3 className="text-2xl font-bold leading-tight">Start with<br />one session.</h3>
          <p className="text-sm text-muted-foreground mt-3 max-w-xs">
            No setup, no configuration. Pick a subject and press start.
          </p>
        </div>
      </div>

      <div className="flex items-center justify-center p-8">
        <form onSubmit={handleSubmit} className="w-full max-w-sm space-y-4">
          <div className="space-y-1">
            <h2 className="text-2xl font-bold">Create account</h2>
            <p className="text-sm text-muted-foreground">Start tracking in under a minute.</p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="username">Username</Label>
            <Input id="username" autoComplete="off" value={username} required
                   onChange={(e) => setUsername(e.target.value)} />
            <p className="text-xs text-muted-foreground">3 to 30 characters</p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input id="password" type="password" autoComplete="new-password"
                   value={password} required
                   onChange={(e) => setPassword(e.target.value)} />
            <p className="text-xs text-muted-foreground">At least 6 characters</p>
          </div>

          {errors.length > 0 && (
            <div className="space-y-1">
              {errors.map((msg, i) => (
                <p key={i} className="text-sm text-destructive">{msg}</p>
              ))}
            </div>
          )}

          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? "Creating..." : "Create account"}
          </Button>

          <p className="text-sm text-muted-foreground text-center">
            Already registered?{" "}
            <Link to="/login" className="text-primary hover:underline">Log in</Link>
          </p>
        </form>
      </div>
    </div>
  );
}