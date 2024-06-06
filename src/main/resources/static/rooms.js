async function fetchrooms() {
    try {
        const response = await fetch('/rooms-data');
        if (response.ok) {
            const rooms = await response.json();
            const tableBody = document.getElementById('rooms-table-body');
            tableBody.innerHTML = '';
            rooms.forEach(room => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td><a href="/room/${room.id}">${room.roomName}</a></td>
                    <td>${room.password}</td>
                    <td>${room.creationDate}</td>
                    <td>${room.players.length} / ${room.playerCount}</td>
                `;
                tableBody.appendChild(row);
            });
        } else {
            console.error('Failed to fetch rooms data');
        }
    } catch (error) {
        console.error('Error fetching rooms data:', error);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    fetchrooms();
    fetchUserData();
});
