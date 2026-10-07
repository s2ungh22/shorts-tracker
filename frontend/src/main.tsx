import Search from "./Search";
import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  BrowserRouter,
  Routes,
  Route,
  NavLink,
  useNavigate,
  useParams,
} from "react-router-dom";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import { api, Video, Channel, Snapshot } from "./api";
import "./style.css";
const fmt = (n: number) =>
  new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 1 }).format(n);
const symbols: Record<string, string> = {
  SURGING: "🚀",
  SECOND_WAVE: "🔥",
  REEXPANDING: "🔥",
  RISING: "↑",
  SLOWING: "↓",
  PLATEAU: "💤",
};
function Status({ value }: { value: string }) {
  return (
    <span className={"status " + value}>
      {symbols[value]} {value.replaceAll("_", " ")}
    </span>
  );
}
function VideoTable({ videos }: { videos: Video[] }) {
  const navigate = useNavigate();
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {[
              "영상",
              "제목 / 채널",
              "조회수",
              "10분 VPH",
              "1시간 VPH",
              "평균 VPH",
              "가속도",
              "상태",
            ].map((x) => (
              <th key={x}>{x}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {videos.map((v) => (
            <tr
              key={v.videoId}
              tabIndex={0}
              onClick={() => navigate("/videos/" + v.videoId)}
              onKeyDown={(e) => {
                if (e.key === "Enter") navigate("/videos/" + v.videoId);
              }}
            >
              <td>
                <img className="thumb" src={v.thumbnailUrl} alt="" />
              </td>
              <td>
                <strong>{v.title}</strong>
                <small>{v.channelName}</small>
              </td>
              <td>{fmt(v.views)}</td>
              <td>{fmt(v.recent10mVph)}</td>
              <td className="accent">{fmt(v.recent1hVph)}</td>
              <td>{fmt(v.averageVph)}</td>
              <td>{fmt(v.accelerationRate)}%</td>
              <td>
                <Status value={v.status} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {videos.length === 0 && (
        <p className="empty">
          추적 중인 영상이 없습니다. 영상을 등록하거나 채널을 Sync하세요.
        </p>
      )}
    </div>
  );
}
function Dashboard({ all = false }: { all?: boolean }) {
  const [videos, setVideos] = useState<Video[]>([]),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true),
    [id, setId] = useState(""),
    [busy, setBusy] = useState(false);
  const load = () =>
    api<Video[]>("/api/dashboard/trending")
      .then((v) => {
        setVideos(v.sort((a, b) => b.recent1hVph - a.recent1hVph));
        setError("");
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  useEffect(() => {
    void load();
    const timer = setInterval(load, 60000);
    return () => clearInterval(timer);
  }, []);
  async function track(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      await api("/api/videos/track", { videoId: id });
      setId("");
      await load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <header>
        <div>
          <p className="eyebrow">LIVE MONITORING · 10 MIN COLLECTION</p>
          <h1>{all ? "Videos" : "Dashboard"}</h1>
          <p>조회수의 속도와 다음 확산 신호를 확인하세요.</p>
        </div>
        <button onClick={load}>새로고침</button>
      </header>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      {!all && (
        <div className="cards">
          {[
            ["Tracking Videos", videos.length],
            [
              "Rising Videos",
              videos.filter((v) => ["RISING", "SURGING"].includes(v.status))
                .length,
            ],
            [
              "Second Wave",
              videos.filter((v) =>
                ["SECOND_WAVE", "REEXPANDING"].includes(v.status),
              ).length,
            ],
            ["Plateau", videos.filter((v) => v.status === "PLATEAU").length],
          ].map(([label, value]) => (
            <article key={label}>
              <p>{label}</p>
              <b>{value}</b>
            </article>
          ))}
        </div>
      )}
      <section>
        <div className="section-heading">
          <h2>{all ? "추적 영상" : "지금 터지는 영상"}</h2>
          <span>최근 1시간 VPH 순</span>
        </div>
        <form className="inline" onSubmit={track}>
          <input
            required
            pattern="[A-Za-z0-9_-]{11}"
            value={id}
            onChange={(e) => setId(e.target.value)}
            placeholder="YouTube Video ID (11자리)"
            aria-label="Video ID"
          />
          <button disabled={busy}>{busy ? "수집 중…" : "영상 추적"}</button>
        </form>
        {loading ? <p>불러오는 중…</p> : <VideoTable videos={videos} />}
      </section>
    </>
  );
}
function Channels({ mine }: { mine: boolean }) {
  const [channels, setChannels] = useState<Channel[]>([]),
    [error, setError] = useState(""),
    [name, setName] = useState(""),
    [id, setId] = useState(""),
    [isMine, setIsMine] = useState(mine),
    [busy, setBusy] = useState(false);
  const load = () =>
    api<Channel[]>("/api/channels")
      .then(setChannels)
      .catch((e) => setError(e.message));
  useEffect(() => {
    setIsMine(mine);
    void load();
  }, [mine]);
  async function add(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      await api("/api/channels", {
        youtubeChannelId: id,
        channelName: name,
        isMyChannel: isMine,
      });
      setId("");
      setName("");
      setError("");
      await load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function sync(id: number) {
    setBusy(true);
    try {
      const result = await api<{ collected: number }>(
        `/api/channels/${id}/sync`,
        {},
      );
      setError("");
      alert(`${result.collected}개 영상 통계를 수집했습니다.`);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <header>
        <div>
          <p className="eyebrow">CHANNEL MANAGEMENT</p>
          <h1>{mine ? "My Channel" : "Competitors"}</h1>
          <p>채널을 등록하고 최신 Shorts 후보를 동기화하세요.</p>
        </div>
      </header>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      <section>
        <h2>채널 등록</h2>
        <form className="channel-form" onSubmit={add}>
          <label>
            Channel ID
            <input
              required
              pattern="UC[A-Za-z0-9_-]{22}"
              value={id}
              onChange={(e) => setId(e.target.value)}
              placeholder="UC…"
            />
          </label>
          <label>
            Channel Name
            <input
              required
              maxLength={200}
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </label>
          <label className="check">
            <input
              type="checkbox"
              checked={isMine}
              onChange={(e) => setIsMine(e.target.checked)}
            />{" "}
            My Channel
          </label>
          <button disabled={busy}>등록</button>
        </form>
      </section>
      <section>
        <h2>등록 채널</h2>
        {channels
          .filter((c) => c.isMyChannel === mine)
          .map((c) => (
            <div className="channel" key={c.id}>
              <div>
                <strong>{c.channelName}</strong>
                <small>{c.youtubeChannelId}</small>
              </div>
              <button disabled={busy} onClick={() => sync(c.id)}>
                Sync
              </button>
            </div>
          ))}
      </section>
    </>
  );
}
function Detail() {
  const { id } = useParams();
  const [video, setVideo] = useState<Video>(),
    [history, setHistory] = useState<Snapshot[]>([]),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  async function load() {
    try {
      const [v, h] = await Promise.all([
        api<Video>(`/api/videos/${id}`),
        api<Snapshot[]>(`/api/videos/${id}/history`),
      ]);
      setVideo(v);
      setHistory(h);
      setError("");
    } catch (e) {
      setError((e as Error).message);
    }
  }
  useEffect(() => {
    void load();
  }, [id]);
  async function collect() {
    setBusy(true);
    try {
      await api(`/api/videos/${id}/collect`, {});
      await load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  const points = history.map((h) => ({
    ...h,
    time: new Date(h.collectedAt).toLocaleString("ko-KR"),
  }));
  return (
    <>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {video ? (
        <>
          <header>
            <div>
              <p className="eyebrow">{video.channelName}</p>
              <h1>{video.title}</h1>
              <Status value={video.status} />
            </div>
            <a
              className="button"
              href={video.shortsUrl}
              target="_blank"
              rel="noreferrer"
            >
              Shorts 바로가기 ↗
            </a>
          </header>
          <div className="detail-overview">
            <img src={video.thumbnailUrl} alt={video.title} />
            <div className="cards">
              {[
                ["조회수", video.views],
                ["Likes", video.likes],
                ["Comments", video.comments],
                ["평균 VPH", video.averageVph],
                ["1시간 VPH", video.recent1hVph],
              ].map(([label, value]) => (
                <article key={label}>
                  <p>{label}</p>
                  <b>{fmt(Number(value))}</b>
                </article>
              ))}
            </div>
          </div>
          <p>
            마지막 수집:{" "}
            {video.collectedAt
              ? new Date(video.collectedAt).toLocaleString("ko-KR")
              : "아직 수집되지 않음"}{" "}
            <button disabled={busy} onClick={collect}>
              지금 수집
            </button>
          </p>
          <div className="charts">
            {(["views", "vph"] as const).map((key) => (
              <section key={key}>
                <h2>{key === "views" ? "시간별 조회수" : "시간별 VPH"}</h2>
                {points.length < 2 ? (
                  <p className="empty">
                    그래프를 위해 최소 2개의 스냅샷이 필요합니다.
                  </p>
                ) : (
                  <ResponsiveContainer width="100%" height={280}>
                    <LineChart data={points}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="time" tick={{ fontSize: 10 }} />
                      <YAxis width={65} />
                      <Tooltip />
                      <Line
                        type="monotone"
                        dataKey={key}
                        stroke="#4f46e5"
                        strokeWidth={2}
                        dot={false}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </section>
            ))}
          </div>
          <section>
            <h2>Snapshot History</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>수집 시간</th>
                    <th>조회수</th>
                    <th>Likes</th>
                    <th>Comments</th>
                    <th>VPH</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((h) => (
                    <tr key={h.collectedAt}>
                      <td>{new Date(h.collectedAt).toLocaleString("ko-KR")}</td>
                      <td>{fmt(h.views)}</td>
                      <td>{fmt(h.likes)}</td>
                      <td>{fmt(h.comments)}</td>
                      <td>{fmt(h.vph)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      ) : (
        !error && <p>불러오는 중…</p>
      )}
    </>
  );
}
function App() {
  return (
    <BrowserRouter>
      <aside>
        <div className="brand">◉ Shorts Tracker</div>
        <p className="eyebrow">WORKSPACE</p>
        <nav>
          {[
            ["/", "Dashboard"],
            ["/search", "Search"],
            ["/my-channel", "My Channel"],
            ["/competitors", "Competitors"],
            ["/videos", "Videos"],
          ].map(([path, label]) => (
            <NavLink key={path} to={path} end>
              {label}
            </NavLink>
          ))}
        </nav>
        <p className="sidebar-note">
          YouTube Shorts
          <br />
          Growth Intelligence
        </p>
      </aside>
      <main>
        <Routes>
          <Route path="/search" element={<Search />} />
          <Route path="/" element={<Dashboard />} />
          <Route path="/videos" element={<Dashboard all />} />
          <Route path="/videos/:id" element={<Detail />} />
          <Route path="/my-channel" element={<Channels mine />} />
          <Route path="/competitors" element={<Channels mine={false} />} />
          <Route path="*" element={<p>페이지를 찾을 수 없습니다.</p>} />
        </Routes>
      </main>
    </BrowserRouter>
  );
}
createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
