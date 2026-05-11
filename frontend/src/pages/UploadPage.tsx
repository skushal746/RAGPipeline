import { useState, useRef, DragEvent, ChangeEvent } from 'react';
import api from '../api/client';

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [dragover, setDragover] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  function selectFile(f: File) {
    setFile(f);
    setSuccess('');
    setError('');
    setProgress(0);
  }

  function onDrop(e: DragEvent) {
    e.preventDefault();
    setDragover(false);
    const f = e.dataTransfer.files[0];
    if (f) selectFile(f);
  }

  function onInputChange(e: ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    if (f) selectFile(f);
  }

  async function handleUpload() {
    if (!file) return;
    setUploading(true);
    setError('');
    setSuccess('');
    const form = new FormData();
    form.append('file', file);
    try {
      await api.post('/documents/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (evt) => {
          if (evt.total) setProgress(Math.round((evt.loaded / evt.total) * 100));
        },
      });
      setSuccess(`"${file.name}" uploaded and queued for processing.`);
      setFile(null);
      setProgress(0);
      if (inputRef.current) inputRef.current.value = '';
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message;
      setError(msg ?? 'Upload failed. Please try again.');
    } finally {
      setUploading(false);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Upload Documents</h1>
        <p className="page-subtitle">
          Upload PDF, DOCX, or TXT files to be processed and indexed for RAG queries.
        </p>
      </div>

      <div className="card">
        <div
          className={`dropzone${dragover ? ' dragover' : ''}`}
          onDragOver={(e) => { e.preventDefault(); setDragover(true); }}
          onDragLeave={() => setDragover(false)}
          onDrop={onDrop}
          onClick={() => inputRef.current?.click()}
        >
          <div className="dropzone-icon">📂</div>
          <p className="dropzone-title">Drop a file here, or click to browse</p>
          <p className="dropzone-hint">PDF, DOCX, TXT — up to 50 MB</p>
          <input
            ref={inputRef}
            type="file"
            accept=".pdf,.docx,.txt"
            style={{ display: 'none' }}
            onChange={onInputChange}
          />
        </div>

        {file && (
          <div className="file-selected">
            <div>
              <div className="file-name">📄 {file.name}</div>
              <div className="file-size">{formatBytes(file.size)}</div>
            </div>
            <button
              className="btn btn-outline"
              style={{ padding: '6px 12px', fontSize: '13px' }}
              onClick={() => { setFile(null); setProgress(0); }}
            >
              Remove
            </button>
          </div>
        )}

        {uploading && (
          <div className="progress-bar-wrap">
            <div className="progress-bar" style={{ width: `${progress}%` }} />
          </div>
        )}

        {error && <div className="error-banner" style={{ marginTop: 16 }}>{error}</div>}

        {success && (
          <div className="success-banner">
            <span>✅</span> {success}
          </div>
        )}

        <button
          className="btn btn-primary upload-btn"
          disabled={!file || uploading}
          onClick={handleUpload}
        >
          {uploading ? `Uploading… ${progress}%` : 'Upload Document'}
        </button>
      </div>
    </div>
  );
}