const loadBtn = document.getElementById("loadSessionsBtn");
const sessionsDiv = document.getElementById("sessions");

loadBtn.addEventListener("click", fetchSessions);

function fetchSessions() {
    fetch("http://localhost:8080/api/sessions")
        .then(res => res.json())
        .then(data => {
            sessionsDiv.innerHTML = "";
            data.forEach(s => {
                const div = document.createElement("div");
                div.innerHTML = `
                    <b>${s.subject}</b><br>
                    Duration: ${s.duration} mins<br>
                    Date: ${s.sessionDate}
                    <hr>
                `;
                sessionsDiv.appendChild(div);
            });
        })
        .catch(err => {
            sessionsDiv.innerHTML = "Error loading sessions";
            console.error(err);
        });
}
