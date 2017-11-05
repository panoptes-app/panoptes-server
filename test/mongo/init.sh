#!/usr/bin/env bash
echo "Creating mongo users..."
mongo admin --host localhost -u root -p root << EOF
use panoptes
db.createUser({user: 'panoptes', pwd: 'panoptes', roles: [{role: 'dbOwner', db: 'panoptes'}]});
EOF
echo "Mongo users created."