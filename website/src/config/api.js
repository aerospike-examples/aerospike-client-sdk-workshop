// Relative URLs work in both modes:
// - Dev (http://localhost:5173): Vite proxies /rest → backend on :8080
// - Bundled (http://localhost:8080): same-origin /rest requests
export const API_V1 = '/rest/v1';

export function apiUrl(path) {
    const normalized = path.startsWith('/') ? path.slice(1) : path;
    return `${API_V1}/${normalized}`;
}
