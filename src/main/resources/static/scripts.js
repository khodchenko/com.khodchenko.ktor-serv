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

function goToRooms() {
    window.location.href = '/rooms';
}

function goToMainPage() {
    window.location.href = '/';
}

function goToRoomCreation(){
    window.location.href = '/create-room';
}

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

fetchUserData();
