import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function Sidebar() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <span className="sidebar-logo-icon">⚡</span>
        <span className="sidebar-logo-text">RAG Pipeline</span>
      </div>
      <nav className="sidebar-nav">
        <NavLink to="/upload" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
          <span className="nav-icon">📄</span>
          Upload Documents
        </NavLink>
        <NavLink to="/chat" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
          <span className="nav-icon">💬</span>
          AI Chat
        </NavLink>
      </nav>
      <button className="sidebar-logout" onClick={handleLogout}>
        <span className="nav-icon">🚪</span>
        Logout
      </button>
    </aside>
  );
}