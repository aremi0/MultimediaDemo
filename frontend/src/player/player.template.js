// FILE di TEMPLATE => serve uno script *.sh per sostituire le variabili d'ambiente

import Keycloak from 'https://cdn.jsdelivr.net/npm/keycloak-js@26.1.4/+esm'

const keycloak = new Keycloak({
    url: 'https://${DOMAIN_NAME}/',
    realm: 'multimedia-realm',
    clientId: 'frontend-client'
});

keycloak.init({
    onLoad: 'check-sso',
    checkLoginIframe: false,
    pkceMethod: 'S256',
    flow: 'standard'
}).then(authenticated => {
    if (!authenticated) {
        keycloak.login();
    } else {
        loadPreloadedSong();
    }
});

function loadPreloadedSong() {
    fetch("https://${DOMAIN_NAME}/api/music-streaming-service/v1/private/preload/song", {
        headers: {
            Authorization: `Bearer ${keycloak.token}`
        }
    })
        .then(res => res.json())
        .then(data => {
            const audio = document.getElementById('audioPlayer');
            audio.src = data.audioUrl; // es: https://cdn.example.com/songs/123.mp3
            document.getElementById('songInfo').innerText = `Titolo: ${data.title}\nArtista: ${data.artist}`;
        })
        .catch(err => {
            console.error("Errore nel caricamento del brano", err);
        });
}
