import {
  Bell,
  ChevronLeft,
  ChevronRight,
  Heart,
  Home,
  Library,
  ListMusic,
  MoreHorizontal,
  Play,
  Plus,
  Search,
  Shuffle,
  SkipBack,
  SkipForward,
  Star,
  User,
  Volume2,
} from "lucide-react";

type Track = {
  title: string;
  artist: string;
  cover: string;
  rating: string;
};

type Playlist = {
  title: string;
  count: string;
  color: string;
};

const recentTracks: Track[] = [
  {
    title: "Faixa 1",
    artist: "Artista Exemplo",
    cover: "linear-gradient(135deg, #65499b, #8159c9)",
    rating: "4.8",
  },
  {
    title: "Faixa 2",
    artist: "Artista Exemplo",
    cover: "linear-gradient(135deg, #2f718f, #49a0bf)",
    rating: "4.6",
  },
  {
    title: "Faixa 3",
    artist: "Artista Exemplo",
    cover: "linear-gradient(135deg, #a15e39, #c47746)",
    rating: "4.9",
  },
  {
    title: "Faixa 4",
    artist: "Artista Exemplo",
    cover: "linear-gradient(135deg, #43875c, #56a872)",
    rating: "4.7",
  },
];

const playlists: Playlist[] = [
  { title: "Focus Beats", count: "24 musicas", color: "#4365a0" },
  { title: "Night Drive", count: "24 musicas", color: "#a1496e" },
  { title: "Chill Hits", count: "24 musicas", color: "#4b9676" },
];

const playlistLinks = ["Focus Beats", "Night Drive", "Chill Hits", "Workout Mix"];

function Sidebar() {
  return (
    <aside className="sidebar" aria-label="Navegacao principal">
      <div className="brand">
        <span className="brand-mark" aria-hidden="true" />
        <span>Splitfy</span>
      </div>

      <nav className="navigation">
        <button className="nav-item active" type="button">
          <Home size={18} />
          <span>Home</span>
        </button>
        <button className="nav-item" type="button">
          <Search size={18} />
          <span>Search</span>
        </button>
        <button className="nav-item" type="button">
          <Library size={18} />
          <span>Your Library</span>
        </button>
      </nav>

      <div className="side-rule" />

      <section className="playlist-menu" aria-label="Playlists">
        <div className="menu-title">
          <span>PLAYLISTS</span>
          <button className="icon-button small" type="button" aria-label="Adicionar playlist">
            <Plus size={15} />
          </button>
        </div>
        {playlistLinks.map((playlist) => (
          <button className="playlist-link" type="button" key={playlist}>
            {playlist}
          </button>
        ))}
      </section>
    </aside>
  );
}

function TopBar() {
  return (
    <header className="topbar">
      <div className="window-actions">
        <button className="icon-button" type="button" aria-label="Voltar">
          <ChevronLeft size={20} />
        </button>
        <button className="icon-button" type="button" aria-label="Avancar">
          <ChevronRight size={20} />
        </button>
      </div>

      <div className="topbar-actions">
        <button className="likes-pill" type="button">
          <span className="pulse-dot" aria-hidden="true" />
          5 New Likes
        </button>
        <button className="icon-button" type="button" aria-label="Notificacoes">
          <Bell size={18} />
        </button>
        <button className="profile-button" type="button" aria-label="Perfil">
          <User size={17} />
        </button>
      </div>
    </header>
  );
}

function TrackCard({ track }: { track: Track }) {
  return (
    <article className="track-card">
      <div className="cover" style={{ background: track.cover }}>
        <button className="play-button floating" type="button" aria-label={`Tocar ${track.title}`}>
          <Play size={19} fill="currentColor" />
        </button>
      </div>
      <div className="track-meta">
        <div>
          <h3>{track.title}</h3>
          <p>{track.artist}</p>
        </div>
        <button className="like-button active" type="button" aria-label={`Curtir ${track.title}`}>
          <Heart size={18} fill="currentColor" />
        </button>
      </div>
      <div className="rating-row">
        <Star size={14} fill="currentColor" />
        <span>{track.rating}</span>
      </div>
    </article>
  );
}

function PlaylistCard({ playlist }: { playlist: Playlist }) {
  return (
    <article className="playlist-card">
      <div className="playlist-art" style={{ backgroundColor: playlist.color }}>
        <ListMusic size={30} />
      </div>
      <div>
        <h3>{playlist.title}</h3>
        <p>{playlist.count}</p>
      </div>
      <button className="icon-button playlist-more" type="button" aria-label={`Mais opcoes de ${playlist.title}`}>
        <MoreHorizontal size={18} />
      </button>
    </article>
  );
}

function MainContent() {
  return (
    <main className="content">
      <TopBar />

      <section className="headline">
        <div>
          <p className="eyebrow">Dashboard Principal</p>
          <h1>Good afternoon</h1>
        </div>
      </section>

      <section className="content-section" aria-labelledby="recently-played-title">
        <div className="section-title-row">
          <h2 id="recently-played-title">Recently Played</h2>
          <span>HistoricoReproducao</span>
        </div>
        <div className="track-grid">
          {recentTracks.map((track) => (
            <TrackCard track={track} key={track.title} />
          ))}
        </div>
      </section>

      <section className="content-section" aria-labelledby="your-playlists-title">
        <div className="section-title-row">
          <h2 id="your-playlists-title">Your Playlists</h2>
          <span>Playlist</span>
        </div>
        <div className="playlist-grid">
          {playlists.map((playlist) => (
            <PlaylistCard playlist={playlist} key={playlist.title} />
          ))}
        </div>
      </section>
    </main>
  );
}

function MusicPlayer() {
  return (
    <footer className="player" aria-label="Player de musica">
      <div className="now-playing">
        <div className="mini-cover" />
        <div>
          <strong>Faixa Atual - Exemplo</strong>
          <span>Artista Exemplo</span>
        </div>
        <button className="like-button active" type="button" aria-label="Curtir faixa atual">
          <Heart size={17} fill="currentColor" />
        </button>
      </div>

      <div className="player-center">
        <div className="player-controls">
          <button className="icon-button" type="button" aria-label="Aleatorio">
            <Shuffle size={18} />
          </button>
          <button className="icon-button" type="button" aria-label="Faixa anterior">
            <SkipBack size={20} fill="currentColor" />
          </button>
          <button className="play-button" type="button" aria-label="Tocar ou pausar">
            <Play size={23} fill="currentColor" />
          </button>
          <button className="icon-button" type="button" aria-label="Proxima faixa">
            <SkipForward size={20} fill="currentColor" />
          </button>
        </div>
        <div className="progress-area">
          <span>1:12</span>
          <div className="progress-track">
            <span className="progress-fill" />
          </div>
          <span>3:45</span>
        </div>
      </div>

      <div className="volume-area">
        <Volume2 size={18} />
        <div className="volume-track">
          <span />
        </div>
      </div>
    </footer>
  );
}

export default function App() {
  return (
    <div className="app-shell">
      <Sidebar />
      <MainContent />
      <MusicPlayer />
    </div>
  );
}
