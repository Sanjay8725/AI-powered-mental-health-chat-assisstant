/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        botanical: {
          50: '#F9F7F2',
          100: '#F0ECE1',
          200: '#E2DDD2',
          300: '#C7DBCF',
          400: '#8FBDA1',
          500: '#437A5B',
          600: '#26533A',
          700: '#1D402C',
          800: '#183324',
          900: '#102218',
        },
        terracotta: {
          500: '#D97736',
          600: '#B85E24',
        }
      }
    },
  },
  plugins: [],
}
