import { useState, useRef, useEffect, KeyboardEvent, FormEvent } from 'react';
import api from '../api/client';

interface Message {
  role: 'user' | 'assistant';
  content: string;
}

export default function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  async function send(e?: FormEvent) {
    e?.preventDefault();
    const question = input.trim();
    if (!question || loading) return;

    setInput('');
    setMessages((prev) => [...prev, { role: 'user', content: question }]);
    setLoading(true);

    try {
      const { data } = await api.post<{ answer: string }>('/chat', { question });
      setMessages((prev) => [...prev, { role: 'assistant', content: data.answer }]);
    } catch {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: '⚠️ Something went wrong. Please try again.' },
      ]);
    } finally {
      setLoading(false);
    }
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">AI Chat</h1>
        <p className="page-subtitle">Ask questions about your uploaded documents.</p>
      </div>

      <div className="card chat-container">
        <div className="messages">
          {messages.length === 0 && !loading && (
            <div className="empty-chat">
              <div className="empty-chat-icon">💬</div>
              <div className="empty-chat-title">Ask anything</div>
              <div className="empty-chat-hint">
                Questions are answered using your uploaded documents as context.
              </div>
            </div>
          )}

          {messages.map((msg, i) => (
            <div key={i} className={`message ${msg.role}`}>
              <div className="message-avatar">
                {msg.role === 'user' ? '👤' : '🤖'}
              </div>
              <div className="message-bubble">{msg.content}</div>
            </div>
          ))}

          {loading && (
            <div className="message assistant">
              <div className="message-avatar">🤖</div>
              <div className="message-bubble" style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                <span className="typing-dot" />
                <span className="typing-dot" />
                <span className="typing-dot" />
              </div>
            </div>
          )}

          <div ref={bottomRef} />
        </div>

        <form className="chat-input-area" onSubmit={send}>
          <textarea
            className="chat-textarea"
            placeholder="Ask a question… (Enter to send, Shift+Enter for new line)"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKeyDown}
            rows={1}
            disabled={loading}
          />
          <button
            type="submit"
            className="chat-send-btn"
            disabled={!input.trim() || loading}
            title="Send"
          >
            ➤
          </button>
        </form>
      </div>
    </div>
  );
}