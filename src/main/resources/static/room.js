async function fetchRoomData(roomId) {
    try {
        const response = await fetch(`/room-data/${roomId}`);
        if (response.ok) {
            const roomData = await response.json();
            document.getElementById('room-name').textContent = `room Name: ${roomData.roomName}`;
            document.getElementById('room-info').innerHTML = `
                Password: ${roomData.password}<br>
                Creation Date: ${roomData.creationDate}<br>
                Player Count: ${roomData.players.length} / ${roomData.playerCount}<br>
                Players: ${roomData.players.join(", ")}<br>
                Host: ${roomData.hostId}
            `;
        } else {
            console.error('Failed to fetch room data');
        }
    } catch (error) {
        console.error('Error fetching room data:', error);
    }
}

async function joinRoom(roomId) {
    try {
        const response = await fetch(`/join-room/${roomId}`, {
            method: 'POST'
        });
        if (response.ok) {
            console.log('Joined room successfully');
        } else {
            console.error('Failed to join room');
        }
    } catch (error) {
        console.error('Error joining room:', error);
    }
}

async function leaveRoom(roomId) {
    try {
        const response = await fetch(`/leave-room/${roomId}`, {
            method: 'POST'
        });
        if (response.ok) {
            console.log('Left room successfully');
        } else {
            console.error('Failed to leave room');
        }
    } catch (error) {
        console.error('Error leaving room:', error);
    }
}

async function deleteRoom() {
    const urlParts = window.location.pathname.split('/');
    const roomId = urlParts[urlParts.length - 1];
    if (confirm("Are you sure you want to delete this room?")) {
        try {
            const response = await fetch(`/delete-room/${roomId}`, {
                method: 'DELETE'
            });
            if (response.ok) {
                alert("room deleted successfully.");
                window.location.href = '/rooms';
            } else {
                console.error('Failed to delete room');
            }
        } catch (error) {
            console.error('Error deleting room:', error);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const urlParts = window.location.pathname.split('/');
    const roomId = urlParts[urlParts.length - 1]; // Assumes URL is like /room/{id}
    if (roomId) {
        fetchRoomData(roomId);
        joinRoom(roomId);
        window.addEventListener('beforeunload', () => leaveRoom(roomId));
        setInterval(() => fetchRoomData(roomId), 5000); // Update every 5 seconds
    }
    fetchUserData();
});
