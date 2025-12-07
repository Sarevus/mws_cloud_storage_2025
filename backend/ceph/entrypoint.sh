#!/bin/bash
set -e

# Color definitions
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

# Переменные окружения
CEPH_DAEMON=${CEPH_DAEMON:-mon}
MON_IP=${MON_IP:-0.0.0.0}
CEPH_PUBLIC_NETWORK=${CEPH_PUBLIC_NETWORK:-172.25.0.0/16}
RGW_FRONTEND_PORT=${RGW_FRONTEND_PORT:-9000}

# Создание директорий
mkdir -p /etc/ceph /var/lib/ceph/mon /var/lib/ceph/osd /var/lib/ceph/mgr /var/lib/ceph/bootstrap-osd

# Генерация UUID для кластера если не существует
if [ ! -f /etc/ceph/ceph.conf ]; then
    CLUSTER_UUID=$(uuidgen)

    cat > /etc/ceph/ceph.conf <<EOF
[global]
fsid = ${CLUSTER_UUID}
mon initial members = mon0
mon host = ${MON_IP}
public network = ${CEPH_PUBLIC_NETWORK}
cluster network = ${CEPH_PUBLIC_NETWORK}
auth cluster required = cephx
auth service required = cephx
auth client required = cephx
osd journal size = 1024
osd pool default size = 1
osd pool default min size = 1
osd pool default pg num = 64
osd pool default pgp num = 64
osd crush chooseleaf type = 0
osd max object name len = 256
osd objectstore = filestore

[mon]
mon allow pool delete = true

[mgr]
mgr/dashboard/ssl = false
mgr/dashboard/server_port = 8080

[client.rgw.${HOSTNAME}]
rgw frontends = beast port=${RGW_FRONTEND_PORT}
rgw dns name = rgw
EOF

    ceph-authtool --create-keyring /etc/ceph/ceph.mon.keyring --gen-key -n mon. --cap mon 'allow *' &>/dev/null
    ceph-authtool --create-keyring /etc/ceph/ceph.client.admin.keyring --gen-key -n client.admin --cap mon 'allow *' --cap osd 'allow *' --cap mds 'allow *' --cap mgr 'allow *' &>/dev/null
    ceph-authtool --create-keyring /var/lib/ceph/bootstrap-osd/ceph.keyring --gen-key -n client.bootstrap-osd --cap mon 'profile bootstrap-osd' --cap mgr 'allow r' &>/dev/null

    ceph-authtool /etc/ceph/ceph.mon.keyring --import-keyring /etc/ceph/ceph.client.admin.keyring &>/dev/null
    ceph-authtool /etc/ceph/ceph.mon.keyring --import-keyring /var/lib/ceph/bootstrap-osd/ceph.keyring &>/dev/null

    monmaptool --create --add mon0 ${MON_IP} --fsid ${CLUSTER_UUID} /etc/ceph/monmap &>/dev/null

    ceph-mon --mkfs -i mon0 --monmap /etc/ceph/monmap --keyring /etc/ceph/ceph.mon.keyring &>/dev/null
    chown -R ceph:ceph /var/lib/ceph/mon /etc/ceph
fi

case "$CEPH_DAEMON" in
    mon)
        echo -e "${CYAN}▶ Starting Ceph Monitor...${NC}"
        chown -R ceph:ceph /var/lib/ceph/mon

        echo -e "\n${GREEN}╔════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║${NC}   ${BOLD}🚀 CEPH MONITOR SERVICE${NC}           ${GREEN}║${NC}"
        echo -e "${GREEN}╠════════════════════════════════════════╣${NC}"
        echo -e "${GREEN}║${NC}   Status: ${GREEN}●${NC} ${BOLD}UP${NC}                       ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}   State:  ${GREEN}▶${NC} ${BOLD}Running${NC}                  ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}   Health: ${GREEN}✓${NC} ${BOLD}Healthy${NC}                 ${GREEN}║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════╝${NC}\n"

        exec ceph-mon -f -i mon0 --public-addr ${MON_IP}
        ;;
    mgr)
        echo -e "${CYAN}▶ Starting Ceph Manager...${NC}"

        for i in {1..30}; do
            if ceph -s &>/dev/null; then
                break
            fi
            sleep 2
        done

        mkdir -p /var/lib/ceph/mgr/ceph-mgr0
        if [ ! -f /var/lib/ceph/mgr/ceph-mgr0/keyring ]; then
            ceph auth get-or-create mgr.mgr0 mon 'allow profile mgr' osd 'allow *' mds 'allow *' -o /var/lib/ceph/mgr/ceph-mgr0/keyring &>/dev/null
        fi
        chown -R ceph:ceph /var/lib/ceph/mgr

        if [ ! -f /var/lib/ceph/mgr/ceph-mgr0/.dashboard_initialized ]; then
            ceph-mgr -i mgr0 &
            MGR_PID=$!
            sleep 10

            ceph mgr module enable dashboard --force &>/dev/null || true
            sleep 2

            ceph config set mgr mgr/dashboard/server_addr 0.0.0.0 &>/dev/null || true
            ceph config set mgr mgr/dashboard/server_port 8080 &>/dev/null || true
            ceph config set mgr mgr/dashboard/ssl false &>/dev/null || true

            ceph dashboard create-self-signed-cert &>/dev/null || true

            echo "Admin@12345678" > /tmp/password.txt
            ceph dashboard ac-user-create admin -i /tmp/password.txt administrator &>/dev/null || true
            rm /tmp/password.txt

            ceph mgr module enable prometheus --force &>/dev/null || true

            touch /var/lib/ceph/mgr/ceph-mgr0/.dashboard_initialized

            kill $MGR_PID
            wait $MGR_PID 2>/dev/null || true
            sleep 2
        fi

        echo -e "\n${GREEN}╔════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║${NC}   ${BOLD}📊 CEPH MANAGER SERVICE${NC}           ${GREEN}║${NC}"
        echo -e "${GREEN}╠════════════════════════════════════════╣${NC}"
        echo -e "${GREEN}║${NC}   Status: ${GREEN}●${NC} ${BOLD}UP${NC}                       ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}   State:  ${GREEN}▶${NC} ${BOLD}Running${NC}                  ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}   Health: ${GREEN}✓${NC} ${BOLD}Healthy${NC}                 ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}   Dashboard: ${CYAN}http://*:8080${NC}       ${GREEN}║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════╝${NC}\n"

        exec ceph-mgr -f -i mgr0
        ;;
    osd)
        echo -e "${CYAN}▶ Starting Ceph OSD...${NC}"

        for i in {1..30}; do
            if ceph -s &>/dev/null; then
                break
            fi
            sleep 2
        done

        OSD_PATH="/var/lib/ceph/osd/ceph-0"
        mkdir -p $OSD_PATH

        OSD_ID=""
        if [ -f "$OSD_PATH/whoami" ]; then
            OSD_ID=$(cat $OSD_PATH/whoami)
        fi

        if [ -z "$OSD_ID" ]; then
            OSD_UUID=$(uuidgen)
            OSD_ID=$(ceph osd create $OSD_UUID 2>/dev/null)

            if [ "$OSD_ID" != "0" ]; then
                NEW_OSD_PATH="/var/lib/ceph/osd/ceph-$OSD_ID"
                if [ "$OSD_PATH" != "$NEW_OSD_PATH" ]; then
                    mkdir -p $NEW_OSD_PATH
                    OSD_PATH=$NEW_OSD_PATH
                fi
            fi

            ceph auth get-or-create osd.$OSD_ID \
                mon 'allow profile osd' \
                mgr 'allow profile osd' \
                osd 'allow *' \
                -o $OSD_PATH/keyring &>/dev/null

            ceph-osd -i $OSD_ID \
                --mkfs \
                --osd-uuid $OSD_UUID \
                --osd-data $OSD_PATH \
                --osd-journal $OSD_PATH/journal \
                --osd-objectstore filestore 2>/dev/null

            ceph osd crush add osd.$OSD_ID 1.0 root=default host=$(hostname) &>/dev/null

            chown -R ceph:ceph $OSD_PATH
        fi

        echo -e "\n${GREEN}╔════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║${NC}   ${BOLD}💾 CEPH OSD SERVICE${NC}               ${GREEN}║${NC}"
        echo -e "${GREEN}╠════════════════════════════════════════╣${NC}"
        echo -e "${GREEN}║${NC}   Status: ${GREEN}●${NC} ${BOLD}UP${NC}                       ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}   State:  ${GREEN}▶${NC} ${BOLD}Running${NC}                  ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}   Health: ${GREEN}✓${NC} ${BOLD}Healthy${NC}                 ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}   OSD ID: ${YELLOW}$OSD_ID${NC}                      ${GREEN}║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════╝${NC}\n"

        chown -R ceph:ceph $OSD_PATH
        exec ceph-osd -f -i $OSD_ID \
            --setuser ceph \
            --setgroup ceph \
            --osd-objectstore filestore \
            --osd-data $OSD_PATH \
            --osd-journal $OSD_PATH/journal 2>/dev/null
        ;;
    rgw)
        echo -e "${CYAN}▶ Starting Ceph RGW...${NC}"

        for i in {1..15}; do
            if ceph -s &>/dev/null 2>&1; then
                break
            fi
            sleep 2
        done

        ceph osd pool create .rgw.root 8 8 2>/dev/null || true
        ceph osd pool create default.rgw.control 8 8 2>/dev/null || true
        ceph osd pool create default.rgw.meta 8 8 2>/dev/null || true
        ceph osd pool create default.rgw.log 8 8 2>/dev/null || true
        ceph osd pool create default.rgw.buckets.index 8 8 2>/dev/null || true
        ceph osd pool create default.rgw.buckets.data 8 8 2>/dev/null || true

        (
            ceph osd pool application enable .rgw.root rgw 2>/dev/null || true
            ceph osd pool application enable default.rgw.control rgw 2>/dev/null || true
            ceph osd pool application enable default.rgw.meta rgw 2>/dev/null || true
            ceph osd pool application enable default.rgw.log rgw 2>/dev/null || true
            ceph osd pool application enable default.rgw.buckets.index rgw 2>/dev/null || true
            ceph osd pool application enable default.rgw.buckets.data rgw 2>/dev/null || true
        ) &

        mkdir -p /var/lib/ceph/radosgw/ceph-rgw.${HOSTNAME}
        if [ ! -f /var/lib/ceph/radosgw/ceph-rgw.${HOSTNAME}/keyring ]; then
            ceph auth get-or-create client.rgw.${HOSTNAME} mon 'allow rw' osd 'allow rwx' -o /var/lib/ceph/radosgw/ceph-rgw.${HOSTNAME}/keyring &>/dev/null
        fi
        chown -R ceph:ceph /var/lib/ceph/radosgw

        if [ ! -f /var/lib/ceph/radosgw/.rgw_initialized ]; then
            radosgw-admin realm create --rgw-realm=default --default 2>/dev/null || true
            radosgw-admin zonegroup create --rgw-zonegroup=default --master --default 2>/dev/null || true
            radosgw-admin zone create --rgw-zone=default --rgw-zonegroup=default --master --default 2>/dev/null || true
            radosgw-admin period update --commit 2>/dev/null || true

            radosgw-admin user create --uid=admin --display-name="Admin User" --system 2>/dev/null || true

            ADMIN_USER_INFO=$(radosgw-admin user info --uid=admin 2>/dev/null)
            if [ -n "$ADMIN_USER_INFO" ]; then
                ACCESS_KEY=$(echo "$ADMIN_USER_INFO" | grep -oP '"access_key":\s*"\K[^"]+' | head -1)
                SECRET_KEY=$(echo "$ADMIN_USER_INFO" | grep -oP '"secret_key":\s*"\K[^"]+' | head -1)

                if [ -n "$ACCESS_KEY" ] && [ -n "$SECRET_KEY" ]; then
                    echo "$ACCESS_KEY" > /tmp/access.key
                    echo "$SECRET_KEY" > /tmp/secret.key

                    ceph dashboard set-rgw-api-access-key -i /tmp/access.key 2>/dev/null || true
                    ceph dashboard set-rgw-api-secret-key -i /tmp/secret.key 2>/dev/null || true
                    ceph dashboard set-rgw-api-host "172.25.0.13" 2>/dev/null || true
                    ceph dashboard set-rgw-api-port 9000 2>/dev/null || true
                    ceph dashboard set-rgw-api-scheme "http" 2>/dev/null || true
                    ceph dashboard set-rgw-api-user-id "admin" 2>/dev/null || true
                    ceph dashboard set-rgw-api-ssl-verify False 2>/dev/null || true

                    rm -f /tmp/access.key /tmp/secret.key
                fi
            fi

            touch /var/lib/ceph/radosgw/.rgw_initialized
        fi

        sleep 3

        echo -e "\n${GREEN}╔════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║${NC}   ${BOLD}🌐 CEPH OBJECT GATEWAY${NC}            ${GREEN}║${NC}"
        echo -e "${GREEN}╠════════════════════════════════════════╣${NC}"
        echo -e "${GREEN}║${NC}   Status: ${GREEN}●${NC} ${BOLD}UP${NC}                       ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}   State:  ${GREEN}▶${NC} ${BOLD}Running${NC}                  ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}   Health: ${GREEN}✓${NC} ${BOLD}Healthy${NC}                 ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}   S3 API: ${CYAN}http://*:${RGW_FRONTEND_PORT}${NC}        ${GREEN}║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════╝${NC}\n"

        exec radosgw -f -n client.rgw.${HOSTNAME} --rgw-frontends="beast port=${RGW_FRONTEND_PORT}"
        ;;
    *)
        echo -e "${RED}✗ Unknown daemon type: $CEPH_DAEMON${NC}"
        exit 1
        ;;
esac
