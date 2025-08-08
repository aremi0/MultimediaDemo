import Keycloak  from 'https://cdn.jsdelivr.net/npm/keycloak-js@26.1.4/+esm'

const keycloak = new Keycloak({
    url: 'https://localhost/', // Inserisco l'indirizzo del nuovo https-reverse-proxy al path che mi indirizza verso keycloak '/auth/'
    realm: 'multimedia-realm',
    clientId: 'frontend-client'
});

// Inizializza Keycloak e gestisce il ritorno dal login
keycloak.init({
    onLoad: 'check-sso',
    checkLoginIframe: false,
    pkceMethod: 'S256',
    flow: 'standard'
}).then(authenticated => {
    if (authenticated) {
        onLoginSuccess();
    } else {
        console.log("Non autenticato");
    }
}).catch(err => {
    console.error("Errore durante init", err);
    console.log("Keycloak object:", keycloak);
});

document.getElementById('loginBtn').addEventListener('click', () => {
    keycloak.login();
});

document.getElementById('logoutBtn').addEventListener('click', () => {
    keycloak.logout();
});

document.getElementById('callMe').addEventListener('click', () => {
    fetch("https://localhost/api/demo-service/v2/public/demo", { // Instrado verso l'https-reverse-proxy al path che mi indirizza verso il gateway-service '/api/'
        headers: {
            Authorization: `Bearer ${keycloak.token}`
        }
    })
        .then(res => res.json())
        .then(data => {
            document.getElementById('response').innerText = JSON.stringify(data, null, 2);
        });
});

function onLoginSuccess() {
    document.getElementById('loginBtn').style.display = 'none';
    document.getElementById('logoutBtn').style.display = 'inline-block';
    document.getElementById('content').style.display = 'block';
    document.getElementById('token').innerText = keycloak.token;
}
