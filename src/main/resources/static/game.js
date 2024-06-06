async function fetchGameData(gameId) {
    try {
        const response = await fetch(`/game-data/${gameId}`);
        if (response.ok) {
            const gameData = await response.json();
            document.getElementById('game-name').textContent = `Game Name: ${gameData.gameName}`;
            document.getElementById('game-info').innerHTML = `
                Password: ${gameData.password}<br>
                Creation Date: ${gameData.creationDate}<br>
                Player Count: ${gameData.players.length} / ${gameData.playerCount}<br>
                Players: ${gameData.players.join(", ")}<br>
                Host: ${gameData.hostId}
            `;
        } else {
            console.error('Failed to fetch game data');
        }
    } catch (error) {
        console.error('Error fetching game data:', error);
    }
}

async function joinGame(gameId) {
    try {
        const response = await fetch(`/join-game/${gameId}`, {
            method: 'POST'
        });
        if (response.ok) {
            console.log('Joined game successfully');
        } else {
            console.error('Failed to join game');
        }
    } catch (error) {
        console.error('Error joining game:', error);
    }
}

async function leaveGame(gameId) {
    try {
        const response = await fetch(`/leave-game/${gameId}`, {
            method: 'POST'
        });
        if (response.ok) {
            console.log('Left game successfully');
        } else {
            console.error('Failed to leave game');
        }
    } catch (error) {
        console.error('Error leaving game:', error);
    }
}

async function deleteGame() {
    const urlParts = window.location.pathname.split('/');
    const gameId = urlParts[urlParts.length - 1];
    if (confirm("Are you sure you want to delete this game?")) {
        try {
            const response = await fetch(`/delete-game/${gameId}`, {
                method: 'DELETE'
            });
            if (response.ok) {
                alert("Game deleted successfully.");
                window.location.href = '/games';
            } else {
                console.error('Failed to delete game');
            }
        } catch (error) {
            console.error('Error deleting game:', error);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const urlParts = window.location.pathname.split('/');
    const gameId = urlParts[urlParts.length - 1]; // Assumes URL is like /game/{id}
    if (gameId) {
        fetchGameData(gameId);
        joinGame(gameId);
        window.addEventListener('beforeunload', () => leaveGame(gameId));
        setInterval(() => fetchGameData(gameId), 5000); // Update every 5 seconds
    }
    fetchUserData();
});
