#!/usr/bin/env bash
# One-time setup for a fresh Ubuntu 24.04 VM: Docker, a swap file (the VM has only 1 GB of RAM),
# unattended security updates, SSH hardening with fail2ban, and a deploy directory.
# Run as the admin user: bash setup-vm.sh
set -euo pipefail

echo "==> Docker"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
fi

echo "==> 2 GB swap"
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
  echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-swappiness.conf
  sudo sysctl -p /etc/sysctl.d/99-swappiness.conf
fi

echo "==> Automatic security updates"
sudo apt-get update -y
sudo apt-get install -y unattended-upgrades
sudo dpkg-reconfigure -f noninteractive unattended-upgrades

echo "==> SSH hardening"
# Port 22 stays open because GitHub Actions deploys over SSH from changing IPs, so: keys only (the
# Azure image already disables passwords), no root login, and fail2ban for brute-force attempts.
echo "PermitRootLogin no" | sudo tee /etc/ssh/sshd_config.d/90-kren-hardening.conf >/dev/null
sudo sshd -t && sudo systemctl reload ssh
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y fail2ban
sudo tee /etc/fail2ban/jail.d/sshd.local >/dev/null <<'JAIL'
[sshd]
enabled = true
backend = systemd
maxretry = 5
findtime = 10m
bantime = 1h
JAIL
sudo systemctl enable --now fail2ban
sudo systemctl restart fail2ban

echo "==> Deploy directory"
mkdir -p ~/kren

echo "Done. Log out and back in (for the docker group), then put compose.prod.yaml, Caddyfile and .env in ~/kren."
