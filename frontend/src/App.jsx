import { useCallback, useEffect, useState } from 'react';
import { getHealth, listDocuments } from './api';
import DocumentsPanel from './components/DocumentsPanel.jsx';
import ChatPanel from './components/ChatPanel.jsx';

export default function App() {
  const [documents, setDocuments] = useState([]);
  const [docError, setDocError] = useState('');
  const [online, setOnline] = useState(null); // null = checking, true = up, false = down

  const refreshDocuments = useCallback(async () => {
    try {
      setDocuments(await listDocuments());
      setDocError('');
    } catch (e) {
      setDocError(e.message);
    }
  }, []);

  useEffect(() => {
    refreshDocuments();
    getHealth()
      .then(() => setOnline(true))
      .catch(() => setOnline(false));
  }, [refreshDocuments]);

  const status = online === null ? 'checking' : online ? 'online' : 'offline';

  return (
    <div className="app">
      <header className="header">
        <div>
          <h1>Knowledge Base</h1>
          <p className="muted">Private answers from your own documents, with sources.</p>
        </div>
        <span className={`status status-${status}`}>
          <span className="dot" /> Backend {status}
        </span>
      </header>

      <main className="layout">
        <DocumentsPanel documents={documents} error={docError} onChanged={refreshDocuments} />
        <ChatPanel hasDocuments={documents.length > 0} />
      </main>
    </div>
  );
}
