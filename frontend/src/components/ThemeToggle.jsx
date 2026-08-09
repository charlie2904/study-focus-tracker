import { useState, useEffect } from "react";

export default function ThemeToggle() {
  const [dark, setDark] = useState(
    () => localStorage.getItem("theme") !== "light"
  );

  useEffect(() => {
    document.documentElement.classList.toggle("dark", dark);
    localStorage.setItem("theme", dark ? "dark" : "light");
  }, [dark]);

  return (
    <button
      onClick={() => setDark(!dark)}
      className="flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium
                 text-muted-foreground hover:text-foreground hover:bg-muted/50 w-full transition-colors"
    >
      <span className="w-4 text-center">{dark ? "☾" : "☀"}</span>
      {dark ? "Dark" : "Light"}
    </button>
  );
}