#!/usr/bin/env bash
set -euo pipefail

if [[ $EUID -ne 0 ]]; then
  echo "This script must be run as root (use sudo)." >&2
  exit 1
fi

CEPH_RELEASE=${CEPH_RELEASE:-reef}
DASHBOARD_USER=${DASHBOARD_USER:-admin}
DASHBOARD_PASSWORD=${DASHBOARD_PASSWORD:-ChangeMe123}

if [[ -z "${MON_IP:-}" ]]; then
  # grab the first non-loopback IPv4 address
  MON_IP=$(hostname -I | awk '{print $1}')
fi

if [[ -z "$MON_IP" ]]; then
  echo "Unable to determine monitor IP address. Set MON_IP environment variable." >&2
  exit 1
fi

APT_KEYRING_DIR=${APT_KEYRING_DIR:-/etc/apt/keyrings}
APT_LIST_FILE=/etc/apt/sources.list.d/ceph.list

apt-get update
apt-get install -y curl gnupg lvm2

mkdir -p "$APT_KEYRING_DIR"

if [[ ! -f "$APT_KEYRING_DIR/ceph.gpg" ]]; then
  echo "Adding Ceph release key..."
  curl -fsSL https://download.ceph.com/keys/release.asc | gpg --dearmor -o "$APT_KEYRING_DIR/ceph.gpg"
fi

echo "deb [signed-by=$APT_KEYRING_DIR/ceph.gpg] https://download.ceph.com/debian-$CEPH_RELEASE/ $(. /etc/os-release && echo "$VERSION_CODENAME") main" \
  | tee "$APT_LIST_FILE" > /dev/null

apt-get update
apt-get install -y cephadm ceph-common podman

if ! systemctl is-enabled --quiet podman.socket; then
  systemctl enable --now podman.socket
fi

CLUSTER_NAME=${CLUSTER_NAME:-ceph}
DASHBOARD_PORT=${DASHBOARD_PORT:-8443}
SSH_USER=${SSH_USER:-${SUDO_USER:-$(logname 2>/dev/null || echo root)}}

if [[ -z "$SSH_USER" ]]; then
  SSH_USER=root
fi

echo "Bootstrapping Ceph cluster '$CLUSTER_NAME' on $MON_IP..."
cephadm bootstrap \
  --mon-ip "$MON_IP" \
  --cluster-name "$CLUSTER_NAME" \
  --initial-dashboard-user "$DASHBOARD_USER" \
  --initial-dashboard-password "$DASHBOARD_PASSWORD" \
  --dashboard-port "$DASHBOARD_PORT" \
  --ssh-user "$SSH_USER"

ceph mgr module enable dashboard || true
ceph config set mgr mgr/dashboard/ssl false

MGR_UNIT="ceph-${CLUSTER_NAME}@mgr.$(hostname).service"
if systemctl list-units --all --full | grep -Fq "$MGR_UNIT"; then
  systemctl restart "$MGR_UNIT"
fi

cat <<EOM
Ceph cluster bootstrapped successfully.
Dashboard URL: http://$MON_IP:$DASHBOARD_PORT/
Username: $DASHBOARD_USER
Password: $DASHBOARD_PASSWORD
EOM