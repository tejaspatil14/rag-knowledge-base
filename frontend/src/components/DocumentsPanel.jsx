import { useRef, useState } from 'react';
import { deleteDocument, uploadDocument } from '../api';

const ACCEPTED = ['pdf', 'docx', 'txt'];

export default function DocumentsPanel({ documents, error, onChanged }) {
  const inputRef = useRef(null);
  const [busy, setBusy] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const [message, setMessage] = useState(null); // { type: 'ok' | 'error', text }

  async function handleFiles(fileList) {
    const files = Array.from(fileList);
    if (files.length === 0) return;

    setBusy(true);
    setMessage(null);
    try {
      for (const file of files) {
        const ext = file.name.split('.').pop().toLowerCase();
        if (!ACCEPTED.includes(ext)) {
          throw new Error(`${file.name}: only PDF, DOCX and TXT files are supported`);
        }
        const result = await uploadDocument(file);
        setMessage({ type: 'ok', text: `${result.source}: ${result.message}` });
      }
    } catch (e) {
      setMessage({ type: 'error', text: e.message });
    } finally {
      setBusy(false);
      if (inputRef.current) inputRef.current.value = '';
      onChanged();
    }
  }

  async function handleDelete(source) {
    if (!window.confirm(`Delete "${source}" and all its chunks?`)) return;
    try {
      await deleteDocument(source);
      setMessage({ type: 'ok', text: `Deleted ${source}` });
    } catch (e) {
      setMessage({ type: 'error', text: e.message });
    }
    onChanged();
  }

  return (
    <section className="card">
      <h2>Documents</h2>

      <button
        type="button"
        className={`dropzone ${dragOver ? 'dropzone-active' : ''}`}
        disabled={busy}
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragOver(false);
          handleFiles(e.dataTransfer.files);
        }}
      >
        {busy ? 'Uploading and embedding... this can take a minute' : 'Drop files here or click to choose (PDF, DOCX, TXT)'}
      </button>
      <input
        ref={inputRef}
        type="file"
        multiple
        accept=".pdf,.docx,.txt"
        hidden
        onChange={(e) => handleFiles(e.target.files)}
      />

      {message && <p className={`notice notice-${message.type}`}>{message.text}</p>}
      {error && <p className="notice notice-error">{error}</p>}

      {documents.length === 0 && !error ? (
        <p className="muted">No documents yet. Upload one to get started.</p>
      ) : (
        <ul className="doc-list">
          {documents.map((doc) => (
            <li key={doc.source}>
              <div>
                <strong>{doc.source}</strong>
                <span className="muted"> · {doc.chunks} chunks</span>
              </div>
              <button type="button" className="link-danger" onClick={() => handleDelete(doc.source)}>
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
