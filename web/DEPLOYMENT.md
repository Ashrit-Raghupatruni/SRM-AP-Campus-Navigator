# Deployment Guide: SRM AP Campus Navigator Web Application

The web version of **SRM AP Campus Navigator** is configured for instant, zero-cost production deployment to **Vercel**, **GitHub Pages**, **Netlify**, or **Cloudflare Pages**.

---

## 🚀 1. Deploying to Vercel (Recommended)

1. Push your branch `feature/web-campus-navigator` to GitHub.
2. In [Vercel Dashboard](https://vercel.com/dashboard), click **Add New Project** and select this GitHub repository.
3. In **Project Settings**:
   - **Root Directory:** `web`
   - **Framework Preset:** `Vite`
   - **Build Command:** `npm run build`
   - **Output Directory:** `dist`
4. Click **Deploy**. Your campus web app will be live with full SSL, global CDN, and automated preview deployments on every push.

---

## ⚡ 2. Deploying to GitHub Pages (Automated via Actions)

A GitHub Actions workflow is provided at `.github/workflows/deploy-web.yml`:
- Every push to `feature/web-campus-navigator` or `main` automatically runs TypeScript compilation, produces the production bundle in `web/dist`, and publishes to GitHub Pages.

---

## 💻 3. Local Development & Preview

```bash
cd web
npm install
npm run dev
```

To test the production build locally:
```bash
npm run build
npm run preview
```
