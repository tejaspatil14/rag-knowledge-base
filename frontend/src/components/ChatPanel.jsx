import { useEffect, useRef, useState } from 'react';
import { ask } from '../api';

export default function ChatPanel({ hasDocuments }) {
  const [question, setQuestion] = useState('');
  const [loading, setLoading] = useState(false);
  const [items, setItems] = useState([]); // [{ id, question, response?, error? }]
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [items, loading]);

  async function handleSubmit(e) {
    e.preventDefault();
    const q = question.trim();
    if (!q || loading) return;

    setQuestion('');
    setLoading(true);
    try {
      const response = await ask(q);
      setItems((prev) => [...prev, { id: Date.now(), question: q, response }]);
    } catch (err) {
      setItems((prev) => [...prev, { id: Date.now(), question: q, error: err.message }]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="card chat">
      <h2>Ask a question</h2>

      <div className="messages">
        {items.length === 0 && !loading && (
          <p className="muted">
            {hasDocuments
              ? 'Ask anything about your documents, for example: "Which rivers flow through the state?"'
              : 'Upload a document on the left first, then ask your question here.'}
          </p>
        )}

        {items.map((item) => (
          <div key={item.id} className="exchange">
            <div className="bubble bubble-user">{item.question}</div>
            {item.error ? (
              <div className="bubble bubble-error">{item.error}</div>
            ) : (
              <Answer response={item.response} />
            )}
          </div>
        ))}

        {loading && <div className="bubble bubble-bot muted">Thinking... local models can take up to a minute.</div>}
        <div ref={endRef} />
      </div>

      <form className="ask-form" onSubmit={handleSubmit}>
        <input
          type="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Type your question..."
          maxLength={1000}
          disabled={loading}
        />
        <button type="submit" disabled={loading || !question.trim()}>
          Ask
        </button>
      </form>
    </section>
  );
}

function Answer({ response }) {
  const percent = Math.round(response.confidence * 100);

  return (
    <div className="bubble bubble-bot">
      <p className="answer">{response.answer}</p>

      <div className="confidence" title="The model's own estimate, so treat it as a rough signal">
        <span className="muted">Confidence {percent}%</span>
        <div className="meter">
          <div className="meter-fill" style={{ width: `${percent}%` }} />
        </div>
      </div>

      {response.sources.length === 0 ? (
        <p className="muted small">No sources used.</p>
      ) : (
        <div className="sources">
          <span className="muted small">Sources</span>
          <ul>
            {response.sources.map((s) => (
              <li key={`${s.source}-${s.chunkIndex}`} className="chip">
                {s.source} · chunk {s.chunkIndex} · {s.score.toFixed(2)}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
