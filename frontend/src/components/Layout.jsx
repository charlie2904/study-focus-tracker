import { NavLink, useNavigate } from "react-router-dom";
import ThemeToggle from "@/components/ThemeToggle";

const links = [
  { to: "/timer",     label: "Timer",     icon: "◐" },
  { to: "/dashboard", label: "Dashboard", icon: "▤" },
  { to: "/history",   label: "History",   icon: "☰" },
];

function usernameFromToken() {
  try {
    const token = localStorage.getItem("token");
    if (!token) return "you";
    const payload = JSON.parse(atob(token.split(".")[1]));
    return payload.sub || "you";
  } catch {
    return "you";
  }
}

export default function Layout({ children }) {
  const username = usernameFromToken();
  const navigate = useNavigate();

  function logout() {
    localStorage.removeItem("token");
    navigate("/login");
  }

  return (
    <div className="min-h-screen flex bg-background text-foreground">
      {/* Sidebar */}
      <aside className="w-56 border-r flex flex-col p-3 shrink-0">
        <div className="flex items-center gap-2 px-2 py-3 font-semibold">
          <span className="h-6 w-6 rounded-md bg-primary grid place-items-center text-primary-foreground text-xs">
            ◐
          </span>
          FocusTrack
        </div>

        <nav className="flex flex-col gap-0.5 mt-4">
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  isActive
                    ? "bg-muted text-foreground"
                    : "text-muted-foreground hover:text-foreground hover:bg-muted/50"
                }`
              }
            >
              <span className="w-4 text-center opacity-80">{l.icon}</span>
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto">
                  <ThemeToggle />
                </div>

                <div className="pt-4 border-t">


          <div className="flex items-center gap-3 px-2 py-2">
            <span className="h-7 w-7 rounded-full bg-primary/15 text-primary grid place-items-center text-xs font-bold">
              {username.charAt(0).toUpperCase()}
            </span>
            <div className="min-w-0 flex-1">
              <div className="text-sm font-medium truncate">{username}</div>
              <button
                onClick={logout}
                className="text-xs text-muted-foreground hover:text-foreground"
              >
                Log out
              </button>
            </div>
          </div>
        </div>
      </aside>

      <main className="flex-1 min-w-0">{children}</main>
    </div>
  );
}