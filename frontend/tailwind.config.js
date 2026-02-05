/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{html,js,ts,jsx,tsx}"
  ],
  theme: {
      extend: {
        colors: {
          primary: "#6A8571",
          "accent-orange": "#fb923c",
          "olive-cobalt-light": "#f8faf8",
          "olive-cobalt-muted": "#8ca391",
          "olive-cobalt-dark": "#3d4940",
        },
        fontFamily: {
          display: ["Plus Jakarta Sans", "sans-serif"],
        },
      },
    },
  plugins: [],
}
