/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#e6f3ff',
          100: '#cce7ff',
          200: '#99cfff',
          300: '#66b7ff',
          400: '#339fff',
          500: '#0087ff',
          600: '#006acc',
          700: '#004d99',
          800: '#003066',
          900: '#001333',
        },
        retro: {
          bg: '#0f0f23',
          panel: '#10101a',
          border: '#333366',
          text: '#cccccc',
          accent: '#00ff00',
          warning: '#ffff00',
          danger: '#ff0000',
          success: '#00ff00',
        }
      },
      fontFamily: {
        mono: ['JetBrains Mono', 'Space Mono', 'monospace'],
        display: ['Space Mono', 'monospace'],
      },
      boxShadow: {
        'retro': '4px 4px 0px 0px rgba(0, 0, 0, 0.25)',
        'retro-lg': '8px 8px 0px 0px rgba(0, 0, 0, 0.25)',
      },
    },
  },
  plugins: [],
}
