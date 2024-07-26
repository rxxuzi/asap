#!/bin/bash

output_file="ssh_config.json"

echo "Enter SSH connection details:"
read -p "Hostname: " hostname
read -p "Username: " username
read -p "Port (default 22): " port
read -s -p "Password: " password
echo

# Set default port if not provided
port=${port:-22}

# Create JSON file
cat > "$output_file" << EOF
{
  "host": "$hostname",
  "port": $port,
  "user": "$username",
  "pass": "$password"
}
EOF

echo "SSH configuration has been saved to $output_file"
echo
echo "Content of $output_file:"
cat "$output_file"