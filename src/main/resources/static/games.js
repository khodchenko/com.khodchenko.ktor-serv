async function fetchGames() {
    try {
        const response = await fetch('/games-data');
        if (response.ok) {
            const games = await response.json();
            const tableBody = document.getElementById('games-table-body');
            tableBody.innerHTML = '';
            games.forEach(game => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td><a href="/game/${game.id}">${game.gameName}</a></td>
                    <td>${game.password}</td>
                    <td>${game.creationDate}</td>
                    <td>${game.players.length} / ${game.playerCount}</td>
                `;
                tableBody.appendChild(row);
            });
        } else {
            console.error('Failed to fetch games data');
        }
    } catch (error) {
        console.error('Error fetching games data:', error);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    fetchGames();
    fetchUserData();
});
