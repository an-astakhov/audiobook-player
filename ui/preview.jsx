import React, { useMemo, useState } from "react";

const booksSeed = [
  {
    id: "wind",
    title: "The Name of the Wind",
    author: "Patrick Rothfuss",
    durationMin: 1662,
    progressMin: 684,
    lastPlayedRank: 1,
    speed: 1.0,
    cover: "linear-gradient(145deg, #6f3d2e, #d49a5f)",
    chapters: [
      { title: "Prologue: A Silence of Three Parts", startMin: 0 },
      { title: "Chapter 1: A Place for Demons", startMin: 28 },
      { title: "Chapter 2: A Beautiful Day", startMin: 71 },
      { title: "Chapter 3: Wood and Word", startMin: 118 },
      { title: "Chapter 4: Halfway to Newarre", startMin: 172 },
    ],
  },
  {
    id: "strange",
    title: "Jonathan Strange & Mr Norrell",
    author: "Susanna Clarke",
    durationMin: 1952,
    progressMin: 144,
    lastPlayedRank: 2,
    speed: 1.15,
    cover: "linear-gradient(145deg, #2f3a34, #b9aa87)",
    chapters: [
      { title: "The Library at Hurtfew", startMin: 0 },
      { title: "The Friends of English Magic", startMin: 42 },
      { title: "The Stones of York", startMin: 96 },
      { title: "The Horse Sands", startMin: 151 },
    ],
  },
  {
    id: "blind",
    title: "Blindsight",
    author: "Peter Watts",
    durationMin: 612,
    progressMin: 592,
    lastPlayedRank: 3,
    speed: 1.25,
    cover: "linear-gradient(145deg, #12161c, #5c718a)",
    chapters: [
      { title: "Theseus", startMin: 0 },
      { title: "Rorschach", startMin: 80 },
      { title: "The Gang of Four", startMin: 171 },
      { title: "Scramblers", startMin: 289 },
    ],
  },
  {
    id: "children",
    title: "Children of Time",
    author: "Adrian Tchaikovsky",
    durationMin: 986,
    progressMin: 301,
    lastPlayedRank: 4,
    speed: 1.0,
    cover: "linear-gradient(145deg, #3e4e36, #c6a85d)",
    chapters: [
      { title: "Genesis", startMin: 0 },
      { title: "The Last Humans", startMin: 57 },
      { title: "Portia", startMin: 129 },
      { title: "The Gilgamesh", startMin: 244 },
    ],
  },
];

function formatTime(mins) {
  const h = Math.floor(mins / 60);
  const m = Math.floor(mins % 60);
  return `${h}h ${String(m).padStart(2, "0")}m`;
}

function percent(progress, total) {
  return Math.min(100, Math.round((progress / total) * 100));
}

export default function App() {
  const [books, setBooks] = useState(booksSeed);
  const [selectedBookId, setSelectedBookId] = useState(null);
  const [showEmptyState, setShowEmptyState] = useState(false);

  const sortedBooks = useMemo(() => {
    return [...books].sort((a, b) => a.lastPlayedRank - b.lastPlayedRank);
  }, [books]);

  const selectedBook = books.find((b) => b.id === selectedBookId);

  function updateBook(id, patch) {
    setBooks((prev) => prev.map((b) => (b.id === id ? { ...b, ...patch } : b)));
  }

  return (
    <main className="appShell">
      <style>{styles}</style>

      <section className="phoneFrame">
        {!selectedBook ? (
          <LibraryScreen
            books={sortedBooks}
            showEmptyState={showEmptyState}
            onToggleEmpty={() => setShowEmptyState((v) => !v)}
            onOpenBook={setSelectedBookId}
          />
        ) : (
          <BookScreen
            book={selectedBook}
            onBack={() => setSelectedBookId(null)}
            onUpdate={(patch) => updateBook(selectedBook.id, patch)}
          />
        )}
      </section>
    </main>
  );
}

function LibraryScreen({ books, showEmptyState, onToggleEmpty, onOpenBook }) {
  const visibleBooks = showEmptyState ? [] : books;

  return (
    <div className="screen">
      <header className="libraryHeader">
        <div>
          <p className="eyebrow">Audiobooks</p>
          <h1>Library</h1>
        </div>

        <button className="importButton" onClick={onToggleEmpty}>
          {showEmptyState ? "Restore" : "Import"}
        </button>
      </header>

      {visibleBooks.length === 0 ? (
        <section className="emptyState">
          <div className="emptyIcon">□</div>
          <h2>Your library is empty</h2>
          <p>
            Import audiobook folders or files to begin. Your books, progress,
            and playback position will appear here.
          </p>
          <button className="primaryButton" onClick={onToggleEmpty}>
            Import audiobook
          </button>
        </section>
      ) : (
        <>
          <div className="sectionLabel">Recently played</div>

          <div className="bookList">
            {visibleBooks.map((book) => (
              <button
                key={book.id}
                className="bookRow"
                onClick={() => onOpenBook(book.id)}
              >
                <BookCover cover={book.cover} small />

                <div className="bookRowBody">
                  <div className="bookRowTop">
                    <div>
                      <h2>{book.title}</h2>
                      <p>{book.author}</p>
                    </div>
                    <span>{formatTime(book.durationMin)}</span>
                  </div>

                  <div className="progressTrack">
                    <div
                      className="progressFill"
                      style={{
                        width: `${percent(book.progressMin, book.durationMin)}%`,
                      }}
                    />
                  </div>

                  <div className="bookMeta">
                    <span>{percent(book.progressMin, book.durationMin)}%</span>
                    <span>
                      {formatTime(book.progressMin)} /{" "}
                      {formatTime(book.durationMin)}
                    </span>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function BookScreen({ book, onBack, onUpdate }) {
  const [isPlaying, setIsPlaying] = useState(false);

  const progressPct = percent(book.progressMin, book.durationMin);

  function jumpToChapter(startMin) {
    onUpdate({ progressMin: startMin, lastPlayedRank: 0 });
  }

  function nudge(minutes) {
    const next = Math.max(
      0,
      Math.min(book.durationMin, book.progressMin + minutes)
    );

    onUpdate({ progressMin: next, lastPlayedRank: 0 });
  }

  return (
    <div className="screen bookScreen">
      <header className="detailTopBar">
        <button className="ghostButton" onClick={onBack}>
          ← Library
        </button>
        <button className="ghostButton">•••</button>
      </header>

      <section className="hero">
        <BookCover cover={book.cover} large />

        <div className="titleBlock">
          <h1>{book.title}</h1>
          <p>{book.author}</p>
        </div>
      </section>

      <section className="playerPanel">
        <div className="timeRow">
          <span>{formatTime(book.progressMin)}</span>
          <span>{formatTime(book.durationMin)}</span>
        </div>

        <input
          className="seekBar"
          type="range"
          min="0"
          max={book.durationMin}
          value={book.progressMin}
          onChange={(e) =>
            onUpdate({
              progressMin: Number(e.target.value),
              lastPlayedRank: 0,
            })
          }
        />

        <div className="transport">
          <button className="roundButton" onClick={() => nudge(-30)}>
            −30
          </button>

          <button
            className="playButton"
            onClick={() => setIsPlaying((v) => !v)}
          >
            {isPlaying ? "Pause" : "Play"}
          </button>

          <button className="roundButton" onClick={() => nudge(30)}>
            +30
          </button>
        </div>

        <div className="speedControl">
          {[0.85, 1.0, 1.15, 1.25, 1.5].map((speed) => (
            <button
              key={speed}
              className={book.speed === speed ? "speed active" : "speed"}
              onClick={() => onUpdate({ speed })}
            >
              {speed}×
            </button>
          ))}
        </div>
      </section>

      <section className="chapters">
        <div className="chapterHeader">
          <h2>Chapters</h2>
          <span>{book.chapters.length}</span>
        </div>

        <div className="chapterList">
          {book.chapters.map((chapter, index) => {
            const isCurrent =
              book.progressMin >= chapter.startMin &&
              (index === book.chapters.length - 1 ||
                book.progressMin < book.chapters[index + 1].startMin);

            return (
              <button
                key={chapter.title}
                className={isCurrent ? "chapter current" : "chapter"}
                onClick={() => jumpToChapter(chapter.startMin)}
              >
                <div>
                  <p>{chapter.title}</p>
                  <span>{formatTime(chapter.startMin)}</span>
                </div>

                {isCurrent && <strong>Now</strong>}
              </button>
            );
          })}
        </div>
      </section>
    </div>
  );
}

function BookCover({ cover, small, large }) {
  return (
    <div
      className={[
        "cover",
        small ? "coverSmall" : "",
        large ? "coverLarge" : "",
      ].join(" ")}
      style={{ background: cover }}
    >
      <div className="coverInner">
        <span>AUDIO</span>
        <strong>BOOK</strong>
      </div>
    </div>
  );
}

const styles = `
:root {
  --bg: #efe8dd;
  --surface: #fbf7f0;
  --surface-2: #f5eee4;
  --ink: #2e241c;
  --muted: #776a5e;
  --muted-2: #a09384;
  --line: rgba(46, 36, 28, 0.12);
  --accent: #7b4d34;
  --accent-dark: #4d2f22;
  --shadow: 0 22px 70px rgba(45, 34, 25, 0.18);
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: #d8d0c4;
  color: var(--ink);
  font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

button {
  font: inherit;
}

.appShell {
  min-height: 100vh;
  display: grid;
  place-items: start center;
  padding: 24px 0;
}

.phoneFrame {
  width: 390px;
  height: 820px;
  overflow: hidden;
  border-radius: 34px;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.68), transparent 38%),
    var(--bg);
  box-shadow: var(--shadow);
  border: 1px solid rgba(255,255,255,0.55);
}

.screen {
  height: 100%;
  overflow-y: auto;
  padding: 28px 22px 36px;
}

.libraryHeader {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 30px;
}

.eyebrow {
  margin: 0 0 5px;
  color: var(--muted);
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 42px;
  line-height: 0.95;
  letter-spacing: -0.045em;
}

.importButton,
.primaryButton {
  border: 0;
  background: var(--accent-dark);
  color: #fffaf3;
  border-radius: 999px;
  padding: 11px 16px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.sectionLabel {
  color: var(--muted);
  font-size: 13px;
  margin-bottom: 12px;
  letter-spacing: 0.03em;
}

.bookList {
  display: grid;
  gap: 14px;
  padding-bottom: 16px;
}

.bookRow {
  width: 100%;
  display: flex;
  gap: 14px;
  text-align: left;
  border: 1px solid var(--line);
  background: rgba(251, 247, 240, 0.74);
  padding: 12px;
  border-radius: 22px;
  cursor: pointer;
  color: inherit;
}

.bookRow:active {
  transform: scale(0.99);
}

.cover {
  position: relative;
  flex: 0 0 auto;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 26px rgba(48, 36, 24, 0.2);
}

.cover::after {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(0,0,0,0.22), transparent 18%),
    linear-gradient(180deg, rgba(255,255,255,0.24), transparent 48%);
  pointer-events: none;
}

.coverSmall {
  width: 72px;
  height: 104px;
}

.coverLarge {
  width: 208px;
  height: 296px;
  border-radius: 24px;
}

.coverInner {
  position: absolute;
  inset: 15px 12px;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  color: rgba(255,255,255,0.92);
}

.coverInner span {
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.coverInner strong {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 22px;
  letter-spacing: -0.04em;
}

.coverLarge .coverInner {
  inset: 26px 22px;
}

.coverLarge .coverInner span {
  font-size: 12px;
}

.coverLarge .coverInner strong {
  font-size: 44px;
}

.bookRowBody {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.bookRowTop {
  display: flex;
  gap: 8px;
  justify-content: space-between;
  align-items: flex-start;
}

.bookRow h2 {
  margin: 0 0 4px;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 20px;
  line-height: 1.05;
  letter-spacing: -0.035em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bookRow p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.bookRowTop > span {
  flex: 0 0 auto;
  color: var(--muted-2);
  font-size: 12px;
  margin-top: 2px;
}

.progressTrack {
  height: 5px;
  border-radius: 999px;
  background: rgba(46, 36, 28, 0.11);
  overflow: hidden;
  margin: 14px 0 8px;
}

.progressFill {
  height: 100%;
  border-radius: inherit;
  background: var(--accent);
}

.bookMeta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: var(--muted);
  font-size: 12px;
}

.emptyState {
  min-height: 570px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 20px;
}

.emptyIcon {
  width: 86px;
  height: 112px;
  display: grid;
  place-items: center;
  border-radius: 20px;
  border: 2px dashed rgba(46,36,28,0.2);
  color: var(--muted-2);
  font-size: 36px;
  margin-bottom: 24px;
}

.emptyState h2 {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 30px;
  line-height: 1;
  letter-spacing: -0.04em;
  margin: 0 0 12px;
}

.emptyState p {
  margin: 0 0 24px;
  color: var(--muted);
  line-height: 1.5;
  font-size: 15px;
}

.bookScreen {
  padding-top: 22px;
}

.detailTopBar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 22px;
}

.ghostButton {
  border: 0;
  background: rgba(255,255,255,0.36);
  color: var(--accent-dark);
  border-radius: 999px;
  padding: 9px 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.titleBlock {
  text-align: center;
  margin: 22px 0 24px;
}

.titleBlock h1 {
  font-size: 34px;
  line-height: 0.98;
  max-width: 330px;
}

.titleBlock p {
  margin: 10px 0 0;
  color: var(--muted);
  font-size: 16px;
}

.playerPanel {
  background: rgba(251, 247, 240, 0.78);
  border: 1px solid var(--line);
  border-radius: 28px;
  padding: 18px;
  margin-bottom: 26px;
}

.timeRow {
  display: flex;
  justify-content: space-between;
  color: var(--muted);
  font-size: 13px;
  margin-bottom: 8px;
}

.seekBar {
  width: 100%;
  accent-color: var(--accent);
  cursor: pointer;
}

.transport {
  display: grid;
  grid-template-columns: 72px 1fr 72px;
  align-items: center;
  gap: 12px;
  margin: 18px 0 16px;
}

.roundButton,
.playButton {
  border: 0;
  cursor: pointer;
  color: #fffaf3;
  background: var(--accent-dark);
}

.roundButton {
  height: 58px;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 800;
}

.playButton {
  height: 68px;
  border-radius: 999px;
  font-size: 20px;
  font-weight: 850;
  letter-spacing: -0.02em;
}

.speedControl {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-top: 4px;
}

.speed {
  border: 1px solid var(--line);
  background: var(--surface-2);
  color: var(--muted);
  border-radius: 999px;
  padding: 8px 11px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.speed.active {
  background: var(--ink);
  color: #fffaf3;
}

.chapters {
  padding-bottom: 20px;
}

.chapterHeader {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 10px;
}

.chapterHeader h2 {
  margin: 0;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.chapterHeader span {
  color: var(--muted);
  font-size: 13px;
}

.chapterList {
  display: grid;
  gap: 10px;
}

.chapter {
  width: 100%;
  border: 1px solid var(--line);
  background: rgba(251,247,240,0.58);
  border-radius: 18px;
  padding: 13px 14px;
  text-align: left;
  color: inherit;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}

.chapter.current {
  background: rgba(123, 77, 52, 0.12);
  border-color: rgba(123, 77, 52, 0.28);
}

.chapter p {
  margin: 0 0 4px;
  font-weight: 750;
  font-size: 14px;
}

.chapter span {
  color: var(--muted);
  font-size: 12px;
}

.chapter strong {
  color: var(--accent-dark);
  font-size: 12px;
}
`;