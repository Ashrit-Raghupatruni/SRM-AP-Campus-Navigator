/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        srm: {
          navy: '#172B4D',
          darkNavy: '#111C2E',
          teal: '#0F766E',
          crimson: '#A61B35',
          lightBg: '#F5F7FA',
          slate: '#64748B',
          border: '#E2E8F0',
        }
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      }
    },
  },
  plugins: [],
}
