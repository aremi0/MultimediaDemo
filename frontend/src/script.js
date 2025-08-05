const keycloak = new Keycloak({
    url: 'http://keycloak:8080/',
    realm: 'multimedia',
    clientId: 'frontend-client'
});

keycloak.init({ onLoad: 'login-required' }).then(authenticated => {
    if (authenticated) {
        document.getElementById('loginBtn').style.display = 'none';
        document.getElementById('logoutBtn').style.display = 'inline-block';
        document.getElementById('content').style.display = 'block';
        document.getElementById('token').innerText = keycloak.token;
    }
});

document.getElementById('logoutBtn').addEventListener('click', () => {
    keycloak.logout();
});

document.getElementById('callMe').addEventListener('click', () => {
    fetch("http://gateway:8080/demo/me", {
        headers: {
            Authorization: `Bearer ${keycloak.token}`
        }
    })
        .then(res => res.json())
        .then(data => {
            document.getElementById('response').innerText = JSON.stringify(data, null, 2);
        });
});
