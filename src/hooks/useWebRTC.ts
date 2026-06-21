// useWebRTC.ts
// Hook de señalización WebRTC sobre STOMP para los directos.
//
// Dependencias (instalar en el proyecto frontend):
//   npm i @stomp/stompjs sockjs-client
//   npm i -D @types/sockjs-client
//
// Modelo: el HOST captura cámara/micro (getUserMedia) y abre una RTCPeerConnection
// por cada viewer que se une. Cada VIEWER abre una RTCPeerConnection hacia el host
// (solo recepción). La señalización (offer/answer/iceCandidate) viaja por STOMP:
//   publish -> /app/live/{eventId}/signal
//   subscribe -> /topic/live/{eventId}/signal   (se filtra por el campo `to`)

import { useCallback, useEffect, useRef, useState } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const STUN_SERVERS: RTCConfiguration = {
  iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
};

type SignalType = 'JOIN' | 'LEAVE' | 'OFFER' | 'ANSWER' | 'ICE';

interface SignalMessage {
  type: SignalType;
  from: string;
  to?: string;
  sdp?: RTCSessionDescriptionInit;
  candidate?: RTCIceCandidateInit;
  senderUser?: string;
}

export type Role = 'host' | 'viewer' | null;

export interface UseWebRTCOptions {
  /** URL base del backend (donde está el endpoint SockJS /ws). */
  baseUrl?: string;
  /** JWT para autenticar el CONNECT del WebSocket. */
  token: string;
  /** Id del directo (LiveEvent.id). */
  eventId: string;
}

export interface UseWebRTC {
  localVideoRef: React.RefObject<HTMLVideoElement>;
  remoteVideoRef: React.RefObject<HTMLVideoElement>;
  role: Role;
  connected: boolean;
  error: string | null;
  startBroadcast: () => Promise<void>;
  joinBroadcast: () => Promise<void>;
  leave: () => void;
}

export function useWebRTC({ baseUrl = '', token, eventId }: UseWebRTCOptions): UseWebRTC {
  const localVideoRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRef = useRef<HTMLVideoElement>(null);

  const clientRef = useRef<Client | null>(null);
  const localStreamRef = useRef<MediaStream | null>(null);
  // En el host hay una conexión por viewer; en el viewer, una sola (clave "host").
  const peersRef = useRef<Map<string, RTCPeerConnection>>(new Map());
  const clientIdRef = useRef<string>(crypto.randomUUID());
  const roleRef = useRef<Role>(null);

  const [role, setRole] = useState<Role>(null);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // ---- envío de señalización ----
  const send = useCallback(
    (msg: Omit<SignalMessage, 'from'>) => {
      const client = clientRef.current;
      if (!client || !client.connected) return;
      client.publish({
        destination: `/app/live/${eventId}/signal`,
        body: JSON.stringify({ ...msg, from: clientIdRef.current }),
      });
    },
    [eventId],
  );

  // ---- crea una RTCPeerConnection y cablea ICE + tracks ----
  const createPeer = useCallback(
    (remoteId: string): RTCPeerConnection => {
      const pc = new RTCPeerConnection(STUN_SERVERS);

      pc.onicecandidate = (e) => {
        if (e.candidate) {
          send({ type: 'ICE', to: remoteId, candidate: e.candidate.toJSON() });
        }
      };

      // El viewer recibe el stream del host por aquí.
      pc.ontrack = (e) => {
        if (remoteVideoRef.current && e.streams[0]) {
          remoteVideoRef.current.srcObject = e.streams[0];
        }
      };

      // El host adjunta su cámara/micro a cada peer.
      if (roleRef.current === 'host' && localStreamRef.current) {
        localStreamRef.current.getTracks().forEach((track) => {
          pc.addTrack(track, localStreamRef.current as MediaStream);
        });
      }

      peersRef.current.set(remoteId, pc);
      return pc;
    },
    [send],
  );

  // ---- manejo de mensajes entrantes ----
  const handleSignal = useCallback(
    async (msg: SignalMessage) => {
      // Ignorar mensajes propios o dirigidos a otro cliente.
      if (msg.from === clientIdRef.current) return;
      if (msg.to && msg.to !== clientIdRef.current) return;

      switch (msg.type) {
        case 'JOIN': {
          // Solo el host responde a las uniones creando oferta para ese viewer.
          if (roleRef.current !== 'host') return;
          const pc = createPeer(msg.from);
          const offer = await pc.createOffer();
          await pc.setLocalDescription(offer);
          send({ type: 'OFFER', to: msg.from, sdp: offer });
          break;
        }
        case 'OFFER': {
          // El viewer recibe la oferta del host y responde.
          const pc = createPeer(msg.from);
          await pc.setRemoteDescription(new RTCSessionDescription(msg.sdp!));
          const answer = await pc.createAnswer();
          await pc.setLocalDescription(answer);
          send({ type: 'ANSWER', to: msg.from, sdp: answer });
          break;
        }
        case 'ANSWER': {
          const pc = peersRef.current.get(msg.from);
          if (pc) await pc.setRemoteDescription(new RTCSessionDescription(msg.sdp!));
          break;
        }
        case 'ICE': {
          const pc = peersRef.current.get(msg.from);
          if (pc && msg.candidate) await pc.addIceCandidate(new RTCIceCandidate(msg.candidate));
          break;
        }
        case 'LEAVE': {
          const pc = peersRef.current.get(msg.from);
          if (pc) {
            pc.close();
            peersRef.current.delete(msg.from);
          }
          break;
        }
      }
    },
    [createPeer, send],
  );

  // ---- conexión STOMP (con JWT en el CONNECT) ----
  const connect = useCallback((): Promise<void> => {
    return new Promise((resolve, reject) => {
      if (clientRef.current?.connected) {
        resolve();
        return;
      }
      const client = new Client({
        webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 3000,
        onConnect: () => {
          setConnected(true);
          client.subscribe(`/topic/live/${eventId}/signal`, (frame: IMessage) => {
            try {
              handleSignal(JSON.parse(frame.body) as SignalMessage);
            } catch (e) {
              // mensaje malformado: ignorar
            }
          });
          resolve();
        },
        onStompError: (frame) => {
          const reason = frame.headers['message'] || 'STOMP error';
          setError(reason);
          reject(new Error(reason));
        },
        onWebSocketError: () => {
          setError('No se pudo abrir el WebSocket');
        },
      });
      clientRef.current = client;
      client.activate();
    });
  }, [baseUrl, token, eventId, handleSignal]);

  // ---- HOST: iniciar directo ----
  const startBroadcast = useCallback(async () => {
    setError(null);
    roleRef.current = 'host';
    setRole('host');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
      localStreamRef.current = stream;
      if (localVideoRef.current) {
        localVideoRef.current.srcObject = stream;
      }
      await connect();
      // El host queda a la espera de mensajes JOIN de los viewers.
    } catch (e) {
      setError((e as Error).message);
    }
  }, [connect]);

  // ---- VIEWER: unirse al directo ----
  const joinBroadcast = useCallback(async () => {
    setError(null);
    roleRef.current = 'viewer';
    setRole('viewer');
    try {
      await connect();
      // Anuncia su llegada; el host responderá con una OFFER.
      send({ type: 'JOIN', to: '' });
    } catch (e) {
      setError((e as Error).message);
    }
  }, [connect, send]);

  // ---- salir / limpiar ----
  const leave = useCallback(() => {
    send({ type: 'LEAVE', to: '' });
    peersRef.current.forEach((pc) => pc.close());
    peersRef.current.clear();
    localStreamRef.current?.getTracks().forEach((t) => t.stop());
    localStreamRef.current = null;
    clientRef.current?.deactivate();
    clientRef.current = null;
    roleRef.current = null;
    setRole(null);
    setConnected(false);
  }, [send]);

  // Limpieza al desmontar.
  useEffect(() => {
    return () => {
      peersRef.current.forEach((pc) => pc.close());
      peersRef.current.clear();
      localStreamRef.current?.getTracks().forEach((t) => t.stop());
      clientRef.current?.deactivate();
    };
  }, []);

  return {
    localVideoRef,
    remoteVideoRef,
    role,
    connected,
    error,
    startBroadcast,
    joinBroadcast,
    leave,
  };
}
