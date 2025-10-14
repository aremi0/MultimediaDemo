// FILE di TEMPLATE => serve uno script *.sh per sostituire le variabili d'ambiente

import Keycloak from 'https://cdn.jsdelivr.net/npm/keycloak-js@26.1.4/+esm'

const keycloak = new Keycloak({
    url: 'https://${DOMAIN_NAME}/',
    realm: 'multimedia-realm',
    clientId: 'frontend-client'
});

const info = JSON.parse(localStorage.getItem("activeSongInfo"));

keycloak.init({
    onLoad: 'check-sso',
    checkLoginIframe: false,
    pkceMethod: 'S256',
    flow: 'standard'
}).then(authenticated => {
    if (!authenticated) {
        keycloak.login();
    } else {
        loadActiveSong();
    }
});

if (info) {
    document.getElementById('songInfo').innerText = `Titolo: ${info.title}\nArtista: ${info.artist}`;
}

function loadActiveSong() {
    const audio = document.getElementById('audioPlayer');
    const mediaSource = new MediaSource();
    audio.src = URL.createObjectURL(mediaSource);

    mediaSource.addEventListener('sourceopen', () => {
        const sourceBuffer = mediaSource.addSourceBuffer('audio/mpeg'); // o 'audio/webm; codecs="vorbis"' se usi WebM

        fetch("https://${DOMAIN_NAME}/api/music-streaming-service/v1/private/stream", {
            headers: {
                Authorization: `Bearer ${keycloak.token}`
            }
        })
            .then(response => {
                const reader = response.body.getReader();

                function pump() {
                    return reader.read().then(({ done, value }) => {
                        if (done || !value) {
                            const waitForBuffer = () => {
                                if (!sourceBuffer.updating) {
                                    mediaSource.endOfStream();
                                } else {
                                    setTimeout(waitForBuffer, 50); // aspetta e riprova
                                }
                            };
                            waitForBuffer();
                            return;
                        }

                        sourceBuffer.appendBuffer(value);
                        return pump();
                    });
                }


                return pump();
            })
            .catch(err => {
                console.error("Errore nello streaming audio", err);
            });
    });
}

