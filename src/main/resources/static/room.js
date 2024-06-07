const urlParts = window.location.pathname.split('/');
const roomId = urlParts[urlParts.length - 1];
const socket = new WebSocket(`ws://${window.location.host}/ws/${roomId}`);

socket.onmessage = function(event) {
    const message = JSON.parse(event.data);
    const messageElement = document.createElement('div');
    messageElement.textContent = `[${message.timestamp}] ${message.sender}: ${message.content}`;
    document.getElementById('messages').appendChild(messageElement);
};

function sendMessage() {
    const input = document.getElementById('message-input');
    const message = input.value;
    socket.send(message);
    input.value = '';
}

function fetchRoomData(roomId) {
    fetch(`/room-data/${roomId}`)
        .then(response => response.json())
        .then(roomData => {
            document.getElementById('room-name').textContent = `Room Name: ${roomData.roomName}`;
            document.getElementById('room-info').innerHTML = `
                Password: ${roomData.password}<br>
                Creation Date: ${roomData.creationDate}<br>
                Player Count: ${roomData.players.length} / ${roomData.playerCount}<br>
                Players: ${roomData.players.join(", ")}<br>
                Host: ${roomData.hostId}
            `;
        })
        .catch(error => console.error('Error fetching room data:', error));
}

document.addEventListener('DOMContentLoaded', () => {
    if (roomId) {
        fetchRoomData(roomId);
        joinRoom(roomId);
        window.addEventListener('beforeunload', () => leaveRoom(roomId));
        setInterval(() => fetchRoomData(roomId), 5000); // Update every 5 seconds
    }
    fetchUserData();
});

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
    if (confirm("Are you sure you want to delete this room?")) {
        try {
            const response = await fetch(`/delete-room/${roomId}`, {
                method: 'DELETE'
            });
            if (response.ok) {
                alert("Room deleted successfully.");
                window.location.href = '/rooms';
            } else {
                console.error('Failed to delete room');
            }
        } catch (error) {
            console.error('Error deleting room:', error);
        }
    }
}
