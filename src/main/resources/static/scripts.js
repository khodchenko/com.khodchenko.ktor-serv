function togglePassword() {
    var passwordField = document.getElementById('password');
    var toggleButton = document.querySelector('.toggle-password');
    if (passwordField.type === 'password') {
        passwordField.type = 'text';
        toggleButton.textContent = 'Hide';
    } else {
        passwordField.type = 'password';
        toggleButton.textContent = 'Show';
    }
}

function toggleConfirmPassword() {
    var confirmPasswordField = document.getElementById('confirm-password');
    var toggleButton = confirmPasswordField.nextElementSibling;
    if (confirmPasswordField.type === 'password') {
        confirmPasswordField.type = 'text';
        toggleButton.textContent = 'Hide';
    } else {
        confirmPasswordField.type = 'password';
        toggleButton.textContent = 'Show';
    }
}

function validatePasswords() {
    var password = document.getElementById('password').value;
    var confirmPassword = document.getElementById('confirm-password').value;
    if (password !== confirmPassword) {
        alert("Passwords do not match!");
        return false;
    }
    return true;
}

function validateAndSubmitForm(event) {
    event.preventDefault();
    if (validatePasswords()) {
        var form = document.getElementById('register-form');
        var formData = new FormData(form);
        var xhr = new XMLHttpRequest();
        xhr.open("POST", "/register", true);
        xhr.onload = function () {
            if (xhr.status === 200) {
                window.location.href = "/";
            } else if (xhr.status === 409) {
                document.getElementById('error-message').textContent = "That email is already registered";
            } else {
                document.getElementById('error-message').textContent = "An error occurred. Please try again.";
            }
        };
        xhr.send(formData);
    }
}

document.getElementById('register-form')?.addEventListener('input', function() {
    var username = document.getElementById('username').value;
    var nickname = document.getElementById('nickname').value;
    var avatar = document.getElementById('avatar').value;
    var password = document.getElementById('password').value;
    var confirmPassword = document.getElementById('confirm-password').value;

    var registerButton = document.getElementById('register-button');
    registerButton.disabled = !(username && nickname && avatar && password && confirmPassword);
    registerButton.style.opacity = registerButton.disabled ? 0.5 : 1;
});

const urlParams = new URLSearchParams(window.location.search);
const errorMessage = urlParams.get('error');
if (errorMessage) {
    document.getElementById('error-message').textContent = decodeURIComponent(errorMessage.replace(/\+/g, ' '));
}

async function fetchUserData() {
    try {
        const response = await fetch('/user-data');
        if (response.ok) {
            const userData = await response.json();
            document.querySelector('.username').textContent = `Hello, ${userData.nickname}`;
            document.querySelector('.email').textContent = userData.email;
            document.querySelector('.registration-date').textContent = `Registered on: ${userData.registrationDate}`;
            document.querySelector('.user-card img').src = userData.avatar || 'default-avatar.png';
        } else {
            console.error('Failed to fetch user data');
        }
    } catch (error) {
        console.error('Error fetching user data:', error);
    }
}

function logout() {
    window.location.href = '/logout';
}

function goToGames() {
    window.location.href = '/games';
}

function goToMainPage() {
    window.location.href = '/';
}

function goToRoomCreation(){
    window.location.href = '/create-game';
}

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

fetchUserData();
