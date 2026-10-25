#!/usr/bin/env bash
# One-time setup for a fresh Ubuntu 24.04 VM: Docker, a swap file (the VM has only 1 GB of RAM),
# unattended security updates and a deploy directory. Run as the admin user: bash setup-vm.sh
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

echo "==> Deploy directory"
mkdir -p ~/kren

echo "Done. Log out and back in (for the docker group), then put compose.prod.yaml, Caddyfile and .env in ~/kren."
