async function fetchGameData(gameId) {
    try {
        const response = await fetch(`/game-data/${gameId}`);
        if (response.ok) {
            const gameData = await response.json();
            document.getElementById('game-name').textContent = gameData.gameName;
            document.getElementById('game-details').innerHTML = `
                Game Name: ${gameData.gameName}<br>
                Password: ${gameData.password}<br>
                Creation Date: ${gameData.creationDate}<br>
                Player Count: ${gameData.playerCount}
            `;
        } else {
            console.error('Failed to fetch game data');
        }
    } catch (error) {
        console.error('Error fetching game data:', error);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const urlParts = window.location.pathname.split('/');
    const gameId = urlParts[urlParts.length - 1]; // Assumes URL is like /game/{id}
    if (gameId) {
        fetchGameData(gameId);
    }
    fetchUserData();
});
