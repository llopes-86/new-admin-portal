# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Primary: event staff operator (e.g. hired helper / ceremonialist) triaging for the family around the Bodas de Esmeralda M&L event.

Situation: reviews RSVPs and mural messages in batches, on desktop (sidebar layout) and on phone (bottom-nav layout, PWA installable with offline page). One shared admin role with the same approve and export powers.

## Product Purpose

Private operations tool for the M&L emerald anniversary — not a public site. It lets staff approve/reject guest RSVPs and mural messages and export the final lists for the event. Success means a clean, approved guest list and a moderated mural with no pendencies leaking to the public experience.

## Positioning

A single-purpose event ops console backed directly by PocketBase `rsvp` / `messages` / `users` collections (including legacy `sheet_row_id` IDs) — not a generic admin template. Its difference is the tight RSVP + mural-moderation + XLSX/PDF export loop for one event.

## Operating Context

Workflows: login with e-mail + senha (forced password change via `must_change_password`); Convidados tab with search (nome, responsável, telefone), status filter (Todos/Pendentes/Confirmados), category filter (adultos/crianças/necessidades especiais/veganos), paginated table on desktop + cards on mobile, approve-all card; Mural tab with status filter (Todas/Pendentes/Aprovadas/Rejeitadas), approve-all card, message grid; Exportar (XLSX + jsPDF + autotable) from sidebar and mobile nav; light/dark theme toggle.

Environments/tools: static HTML/CSS/JS in `public/` (`index.html`, `dashboard.html`, `js/`, `css/`, `lib/pocketbase`, `lib/xlsx`, `lib/jspdf`), PocketBase SDK proxied via nginx `/api/pb/api/`, nginx `admin-portal_nginx.conf` with `no-store` static caching, strict CSP, `limit_req` + method allowlist (GET/POST/PATCH), PWA (`manifest.json`, `sw.js`, `offline.html`). Current version 2.0.6. Language pt-BR.

## Capabilities and Constraints

Confirmed functionality: auth + forced password change; guest list management with the fields nome/responsável/tipo/idade/nome da criança/restrições/contato/data/status; mural moderation with Pendente/Aprovado/Rejeitado; XLSX/PDF export; theme toggle; responsive sidebar + bottom-nav; PWA install/offline.

Technical constraints to preserve: PocketBase schema (`users`, `rsvp` with Pendente/Confirmado, `messages` with Pendente/Aprovado/Rejeitado); nginx security posture (no-store for sensitive data, DENY framing, strict CSP, rate limits); pt-BR copy.

Terminology: Convidados, Mural, Exportar, Pendente/Confirmado (guests), Pendente/Aprovado/Rejeitado (messages).

Open decisions: whether family members get a separate read-only role vs the single shared admin role; retention/archival policy after the event. Recorded as undecided — do not invent.

## Brand Commitments

Name: Portal Admin — Bodas de Esmeralda M&L ("Admin M&L"). Asset: `public/images/Sankofa.png`. Voice: pt-BR, formal-operational. Identity constraints: keep Sankofa mark, event name, installed-PWA name "Admin M&L", theme-color `#0F1A2E`, and the existing light/dark theme behavior. Beyond the items listed here and in Capabilities, no additional binding visual constraints.

## Evidence on Hand

Real implementation: `public/index.html`, `public/dashboard.html`, `public/css/`, `public/js/` (incl. `features/guests.js`, `features/messages.js`, `features/export.js`, `utils/auth.js`), `public/images/`, `public/manifest.json`, `pb_schema.json`, `admin-portal_nginx.conf`, `tools/bump-version.ps1`.

Absences future work must not fabricate: no testimonials, case studies, press, pricing, benchmarks, or additional customers. No DESIGN.md yet — incumbent CSS/components are the visual authority for refinements until a redesign explicitly replaces them.

## Product Principles

1. Private accuracy over public spectacle: the portal is never the party; clean moderation state wins.
2. Batch triage speed: approve-all, filters, search, and pagination must keep large RSVP/message queues manageable.
3. No pendency leaks: nothing marked Pendente/Rejeitado surfaces as approved anywhere.
4. Preserve trust and security: auth, forced password rotation, and no-store handling of sensitive guest data are non-negotiable.
5. One event, fully exportable: every list the staff sees must leave as a reliable XLSX/PDF artifact.
