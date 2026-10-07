import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "./api";
interface SearchItem {
  videoId: string;
  title: string;
  channelName: string;
  publishedAt: string;
  thumbnailUrl: string | null;
  videoUrl: string;
}
interface SearchResponse {
  query: string;
  publishedAfter: string;
  days: number;
  maxResults: number;
  items: SearchItem[];
}
export default function Search() {
  const [query, setQuery] = useState("");
  const [days, setDays] = useState(1);
  const [maxResults, setMaxResults] = useState(50);
  const [result, setResult] = useState<SearchResponse>();
  const [busy, setBusy] = useState(false);
  const [tracking, setTracking] = useState<string>();
  const [error, setError] = useState("");
  const navigate = useNavigate();
  async function search(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setResult(undefined);
    try {
      setResult(
        await api<SearchResponse>("/api/search", {
          query: query.trim(),
          days,
          maxResults,
        }),
      );
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function track(videoId: string) {
    setTracking(videoId);
    setError("");
    try {
      await api("/api/videos/track", { videoId });
      navigate("/videos/" + videoId);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setTracking(undefined);
    }
  }
  return (
    <>
      <header>
        <div>
          <p className="eyebrow">CONTENT DISCOVERY</p>
          <h1>콘텐츠 검색</h1>
          <p>한국 기준 최근 게시 영상을 검색하고 추적하세요.</p>
        </div>
      </header>
      <section>
        <form onSubmit={search} className="search-form">
          <label>
            검색어
            <input
              required
              maxLength={200}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="예: 연예인 추천"
            />
          </label>
          <fieldset>
            <legend>검색 기간 · 오늘 포함</legend>
            <div className="day-buttons">
              {[1, 2, 3, 4, 5, 6, 7].map((day) => (
                <button
                  key={day}
                  type="button"
                  aria-pressed={days === day}
                  onClick={() => setDays(day)}
                  disabled={busy}
                >
                  {day === 1 ? "오늘 (1일)" : `최근 ${day}일`}
                </button>
              ))}
            </div>
          </fieldset>
          <label>
            최대 결과 개수 (1~50)
            <input
              type="number"
              required
              min={1}
              max={50}
              step={1}
              value={maxResults}
              onChange={(e) => setMaxResults(e.target.valueAsNumber)}
            />
          </label>
          <button disabled={busy || !query.trim()}>
            {busy ? "검색 중…" : "검색"}
          </button>
          <p className="search-note">
            기간은 한국 시간 자정 기준입니다. 최신 게시순으로 일반 영상과
            Shorts를 함께 검색합니다. 인기·VPH 순위는 추적 후 확인할 수
            있습니다.
          </p>
        </form>
      </section>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {busy && <p role="status">검색 결과를 불러오는 중입니다.</p>}
      {result && (
        <section>
          <h2>
            “{result.query}” 검색 결과 · {result.items.length}개
          </h2>
          <p className="search-note">
            게시일 기준:{" "}
            {new Date(result.publishedAfter).toLocaleString("ko-KR", {
              timeZone: "Asia/Seoul",
            })}{" "}
            이후 (한국 시간)
          </p>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>영상</th>
                  <th>제목 / 채널</th>
                  <th>게시일 (한국 시간)</th>
                  <th>추적</th>
                </tr>
              </thead>
              <tbody>
                {result.items.map((item) => (
                  <tr key={item.videoId}>
                    <td>
                      {item.thumbnailUrl && (
                        <img className="thumb" src={item.thumbnailUrl} alt="" />
                      )}
                    </td>
                    <td>
                      <a href={item.videoUrl} target="_blank" rel="noreferrer">
                        <strong>{item.title}</strong>
                      </a>
                      <small>{item.channelName}</small>
                    </td>
                    <td>
                      {new Date(item.publishedAt).toLocaleString("ko-KR", {
                        timeZone: "Asia/Seoul",
                      })}
                    </td>
                    <td>
                      <button
                        disabled={!!tracking}
                        onClick={() => track(item.videoId)}
                      >
                        {tracking === item.videoId ? "등록 중…" : "영상 추적"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!result.items.length && (
            <p className="empty">
              검색 결과가 없습니다. 검색어 또는 기간을 변경해 보세요.
            </p>
          )}
        </section>
      )}
    </>
  );
}
