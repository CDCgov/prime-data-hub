#!/usr/bin/env python3

# Put the settings in the organization.json and senders.json, and the receivers.json files
# into the local settings service
import json
import requests
import yaml
import time
import argparse

parser = argparse.ArgumentParser(description='Put org file into a prime service')
parser.add_argument('host', default='localhost')
parser.add_argument('org_file', default='organizations-local.yml')
parser.add_argument('--wait', type=int, default=0)
args = parser.parse_args()
host = args.host
protocol = "http"
org_file = args.org_file

base_url = f"{protocol}://{host}:7071/api"
headers = {"Authorization": "bearer xyz", "Content-Type": "application/json", "LocalNoAuth": "true"}


def parse_file(file_name):
    with open(file_name) as file:
        data = yaml.full_load(file)
        return data


def put_org(org):
    justOrg = org.copy()
    if "senders" in justOrg:
        del justOrg["senders"]
    if "receivers" in justOrg:
        del justOrg["receivers"]
    print(f'PUT: {json.dumps(justOrg)}')
    url = f'{base_url}/settings/organizations/{org["name"]}'
    r = requests.put(url, data=json.dumps(justOrg), headers=headers)
    r.raise_for_status()


def put_sender(sender):
    print(f'PUT: {json.dumps(sender)}')
    url = f'{base_url}/settings/organizations/{sender["organizationName"]}/senders/{sender["name"]}'
    r = requests.put(url, data=json.dumps(sender), headers=headers)
    r.raise_for_status()


def put_receiver(receiver):
    print(f'PUT: {json.dumps(receiver)}')
    url = f'{base_url}/settings/organizations/{receiver["organizationName"]}/receivers/{receiver["name"]}'
    r = requests.put(url, data=json.dumps(receiver), headers=headers)
    r.raise_for_status()
    

def main():
    try:
        if args.wait > 0:
            print(f"Waiting for {args.wait} seconds")
            time.sleep(args.wait)

        print(f"Loading {org_file} into {base_url}")
        orgs = parse_file(org_file)
        for org in orgs:
            put_org(org)
        for org in orgs:
            for sender in (org["senders"] if "senders" in org else []):
                put_sender(sender)
        for org in orgs:
            for receiver in (org["receivers"] if "receivers" in org else []):
                put_receiver(receiver)
    except Exception as err:
        print(err)

if __name__ == "__main__":
    main()

