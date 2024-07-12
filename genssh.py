import json
import sys
import argparse
import re

def parse_ssh_info(user_host, port):
    user, host = user_host.split('@')

    return {
        "host": host,
        "port": int(port),
        "user": user,
        "pass": "****"
    }

def generate_template():
    return {
        "host": "example.com",
        "port": 22,
        "user": "username",
        "pass": "****"
    }

def main():
    parser = argparse.ArgumentParser(description='Generate SSH JSON from SSH connection info')
    parser.add_argument('ssh_info', type=str, nargs='?', help='SSH connection info in the format user@host:port')
    parser.add_argument('-o', '--output', type=str, default='ssh.json', help='Output JSON file name')

    args = parser.parse_args()

    if args.ssh_info:
        match = re.search(r'(.+?)[\s:]+(\d+)', args.ssh_info)
        if match:
            user_host = match.group(1)
            port = match.group(2)
            ssh_info = parse_ssh_info(user_host, port)
        else:
            raise ValueError("Invalid SSH info format. Use 'user@host -p port' or 'user@host:port'.")
    else:
        ssh_info = generate_template()

    output_file = args.output

    with open(output_file, 'w') as json_file:
        json.dump(ssh_info, json_file, indent=4)

    print(f'SSH information saved to {output_file}')

if __name__ == '__main__':
    main()
