// LivePage.tsx
// Página de directos: el host pulsa "Iniciar directo" (captura cámara y emite);
// los miembros pulsan "Unirse" (reciben el stream del host).
//
// Requiere el hook useWebRTC y un backend con el WebSocket STOMP en /ws.

import React, { useState } from 'react';
import { useWebRTC } from '../hooks/useWebRTC';

// Ajusta a la URL de tu backend (donde está el endpoint SockJS /ws).
const BASE_URL = 'http://localhost:8080';

export default function LivePage() {
  // En una app real, el token vendría del login y el eventId del listado /live/events.
  const [token, setToken] = useState('');
  const [eventId, setEventId] = useState('');

  const {
    localVideoRef,
    remoteVideoRef,
    role,
    connected,
    error,
    startBroadcast,
    joinBroadcast,
    leave,
  } = useWebRTC({ baseUrl: BASE_URL, token, eventId });

  const ready = token.trim().length > 0 && eventId.trim().length > 0;

  return (
    <div style={{ maxWidth: 960, margin: '0 auto', padding: 24, fontFamily: 'system-ui' }}>
      <h1>Directo</h1>

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 16 }}>
        <input
          placeholder="JWT (GET /live/dev-token?userId=...)"
          value={token}
          onChange={(e) => setToken(e.target.value)}
          style={{ flex: 2, minWidth: 280, padding: 8 }}
        />
        <input
          placeholder="ID del directo (LiveEvent.id)"
          value={eventId}
          onChange={(e) => setEventId(e.target.value)}
          style={{ flex: 1, minWidth: 220, padding: 8 }}
        />
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 8 }}>
        <button onClick={startBroadcast} disabled={!ready || role !== null}>
          Iniciar directo (host)
        </button>
        <button onClick={joinBroadcast} disabled={!ready || role !== null}>
          Unirse (miembro)
        </button>
        <button onClick={leave} disabled={role === null}>
          Salir
        </button>
      </div>

      <p style={{ color: '#666' }}>
        Estado: {connected ? 'conectado' : 'desconectado'}
        {role ? ` · rol: ${role}` : ''}
      </p>
      {error && <p style={{ color: 'crimson' }}>Error: {error}</p>}

      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginTop: 16 }}>
        <figure style={{ margin: 0 }}>
          <figcaption>Emisor (tu cámara)</figcaption>
          <video
            ref={localVideoRef}
            autoPlay
            playsInline
            muted
            style={{ width: 440, background: '#000', borderRadius: 8 }}
          />
        </figure>

        <figure style={{ margin: 0 }}>
          <figcaption>Receptor (directo)</figcaption>
          <video
            ref={remoteVideoRef}
            autoPlay
            playsInline
            style={{ width: 440, background: '#000', borderRadius: 8 }}
          />
        </figure>
      </div>
    </div>
  );
}
