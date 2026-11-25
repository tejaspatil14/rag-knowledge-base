// All calls to the Spring Boot backend live in this one file.
// Paths are relative ("/api/...") so the Vite proxy (dev) or a reverse proxy (production) can forward them.
const BASE = import.meta.env.VITE_API_BASE || '';

async function request(path, options) {
  const res = await fetch(BASE + path, options);

  if (!res.ok) {
    // Our backend always returns { timestamp, status, error, message } for errors.
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body.message) message = body.message;
    } catch {
      // the response had no JSON body, keep the default message
    }
    throw new Error(message);
  }

  return res.status === 204 ? null : res.json();
}

export const getHealth = () => request('/api/health');

export const listDocuments = () => request('/api/documents');

export function uploadDocument(file) {
  const form = new FormData();
  form.append('file', file);
  return request('/api/documents', { method: 'POST', body: form });
}

export const deleteDocument = (source) =>
  request(`/api/documents?source=${encodeURIComponent(source)}`, { method: 'DELETE' });

export const ask = (question) =>
  request('/api/ask', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question }),
  });
