export interface Video {
  videoId: string;
  title: string;
  channelName: string;
  isMyChannel: boolean;
  views: number;
  likes: number;
  comments: number;
  averageVph: number;
  recent10mVph: number;
  recent30mVph: number;
  recent1hVph: number;
  previous1hVph: number;
  accelerationRate: number;
  status: string;
  thumbnailUrl: string;
  shortsUrl: string;
  publishedAt: string;
  collectedAt: string | null;
}
export interface Channel {
  id: number;
  youtubeChannelId: string;
  channelName: string;
  isMyChannel: boolean;
}
export interface Snapshot {
  views: number;
  likes: number;
  comments: number;
  collectedAt: string;
  vph: number;
}
const base = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
export async function api<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(base + path, {
    method: body === undefined ? "GET" : "POST",
    headers: { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(
      error.detail || error.message || `Request failed (${response.status})`,
    );
  }
  return response.json();
}
