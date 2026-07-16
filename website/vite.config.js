import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    // Output directory
    outDir: 'dist',
    // Generate manifest for asset mapping
    manifest: false,
    // Clean output directory before build
    emptyOutDir: true,
    // Optimize for production
    minify: 'terser',
    // Configure asset handling
    assetsDir: 'static',
    rollupOptions: {
      output: {
        // Organize output files
        assetFileNames: (assetInfo) => {
          const info = assetInfo.name.split('.');
          const ext = info[info.length - 1];
          if (/\.(css)$/.test(assetInfo.name)) {
            return `static/css/[name]-[hash].${ext}`;
          }
          if (/\.(png|jpe?g|gif|svg|ico|webp)$/.test(assetInfo.name)) {
            return `static/media/[name]-[hash].${ext}`;
          }
          return `static/[name]-[hash].${ext}`;
        },
        chunkFileNames: 'static/js/[name]-[hash].js',
        entryFileNames: 'static/js/[name]-[hash].js',
      },
    },
  },
  // Configure dev server
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      // Proxy API calls to whichever backend is on :8080 (Java, Python, or Rust)
      '/rest': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
