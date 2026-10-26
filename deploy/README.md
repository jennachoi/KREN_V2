# Deployment

One small VM runs the app container behind Caddy (automatic HTTPS); PostgreSQL is a managed
service. Every push to `main` that passes CI builds an image, pushes it to GHCR and rolls it out.

```
GitHub push ─▶ CI: lint/build frontend, test backend (Testcontainers)
            ─▶ image: docker build ─▶ ghcr.io/jennachoi/kren_v2:{sha,latest}
            ─▶ deploy: scp compose + Caddyfile, ssh "docker compose pull && up -d"

Browser ─HTTPS─▶ Caddy :443 ─▶ app :8080 ─TLS─▶ Azure Database for PostgreSQL
```

## Current target: Azure (student subscription)

| Piece | Service |
|---|---|
| VM | Ubuntu 24.04 Arm64, B2pts v2 (2 vCPU, 1 GiB), Canada Central |
| Database | Azure Database for PostgreSQL – Flexible Server, Burstable B1ms |
| Domain | Azure DNS name label (`<label>.canadacentral.cloudapp.azure.com`) |
| TLS | Caddy + Let's Encrypt |

The student subscription only allows a few regions (westus2, canadacentral and three in Europe), the
managed PostgreSQL service is further limited (it is refused in westus2), and in those regions the only
free-eligible VM size is the Arm64 B2pts v2. Both resources therefore live in Canada Central, so CI builds a `linux/arm64` image on
GitHub's Arm runner. The app container is capped at 640 MB (it uses ~340 MB with the demo data); the VM also gets a
2 GB swap file.

## First-time setup

1. Create the PostgreSQL flexible server (public access, firewall rule for the VM's public IP only,
   TLS enforced) and a `kren` database.
2. Create the VM with an SSH key; open ports 80 and 443 (and 22, ideally only from your IP); set a
   DNS name label on its public IP.
3. On the VM: `bash setup-vm.sh`, log out/in.
4. Put `deploy/compose.prod.yaml` and `deploy/Caddyfile` in `~/kren`, and create `~/kren/.env`
   from `.env.example`.
5. Make the GHCR package public (or `docker login ghcr.io` on the VM), then
   `docker compose -f compose.prod.yaml up -d`.
6. In GitHub: secrets `VM_HOST`, `VM_USER`, `VM_SSH_KEY` (a dedicated deploy key, not the admin
   key); repository variable `DEPLOY_ENABLED=true`. From then on, pushes to `main` deploy
   automatically.

SSH stays reachable from anywhere because GitHub-hosted runners use changing IP addresses. It
accepts keys only, refuses root, and fail2ban bans an address after five failed attempts.

## Operations

```bash
docker compose -f compose.prod.yaml logs -f app      # app logs
docker compose -f compose.prod.yaml ps               # health
KREN_IMAGE_TAG=<older sha> docker compose -f compose.prod.yaml up -d   # roll back
```

## Moving off the managed database

To fit the yearly student credit after the 12-month free services end, run PostgreSQL in the same
compose file (a `postgres:18-alpine` service with a volume), restore a `pg_dump`, and point
`SPRING_DATASOURCE_URL` at it. Nothing in the app is Azure-specific.
