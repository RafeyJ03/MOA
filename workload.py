import json
import sys
import requests


def load_config(config_file):
    try:
        with open(config_file, 'r') as file:
            return json.load(file)
    except FileNotFoundError:
        print(f"Error: Config file '{config_file}' not found.")
        sys.exit(1)

def read_workload_file(lines, operation):
    # Implement logic to read and parse the workload file
    # line = lines.split()
    servicetarget = lines
    dp = {}
    if operation == "user":
        if servicetarget[1].lower() == "create" or servicetarget[1].lower() == "delete":
            dp["command"] = servicetarget[1].lower()
            dp["uid"] = int(servicetarget[2])
            dp["username"] = servicetarget[3]
            dp["email"] = servicetarget[4]
            dp["password"] = servicetarget[5]
            
        # Handling 'update' command for product
        elif servicetarget[1].lower() == "update":
            dp["command"] = servicetarget[1].lower()
            dp["uid"] = int(servicetarget[2])
            # Loop through the rest of the parameters to update fields except for 'pid'
            for param in servicetarget[3:]:
                key, value = param.split(":")
                dp[key] = value

    elif operation == "product":
        if servicetarget[1].lower() == "create" or servicetarget[1].lower() == "delete":
            dp["command"] = servicetarget[1].lower()
            dp["pid"] = int(servicetarget[2])
            dp["productname"] = servicetarget[3]
            dp["description"] = servicetarget[4]
            dp["price"] = float(servicetarget[5])
            dp["quantity"] = int(servicetarget[6])

        # Handling 'update' command for product
        elif servicetarget[1].lower() == "update":
            dp["command"] = servicetarget[1].lower()
            dp["pid"] = servicetarget[2]
            # Loop through the rest of the parameters to update fields except for 'pid'
            for param in servicetarget[3:]:
                key, value = param.split(":")
                if key == "price":
                    dp[key] = float(value)
                elif key == "quantity":
                    dp[key] = int(value)
                else:
                    dp[key] = value

    elif operation == "order":
        dp["command"] = servicetarget[1].lower()
        dp["uid"] = int(servicetarget[2])
        dp["pid"] = int(servicetarget[3])
        dp["quantity"] = int(servicetarget[2])
    return dp


def make_post_request(url, command):
    try:
        work = read_workload_file(command, command[0].lower())
        print(work, url)
        response = requests.post(url, data=str(work))
        if response.status_code == 200:
            print(f"POST request did work: {response.status_code}")
            print("Response: ", response.text)
        else:
            print(f"POST request did not work: {response.status_code}")
            print("Response: ", response.text)
    except Exception as e:
        print(e)



def make_get_request(url, command):
    try:
        response = requests.get(url + str(command[2]))
        if response.status_code == 200:
            print(f"POST request did work: {response.status_code}")
            print("Response: ", response.text)
        else:
            print(f"POST request did not work: {response.status_code}")
            print("Response: ", response.text)
    except Exception as e:
        print(e)


def main(config_file, workload_file_path):
    action_to_method_post = {
        "user": ["create","update", "delete"],
        "order": ["place"],
        "product": ["create","update", "delete"]
    }
    action_to_method_get = {
        "info": ["product"],
        "get": ["user"]
    }

    config = load_config(config_file)
    ip = config['OrderService']['ip']
    port =config['OrderService']['port']

    try:
        f = open(workload_file_path, 'r')
        Lines = f.readlines()
        for nline in Lines:
            line = nline.split()
            command = line[1]
            target_service = line[0].lower().strip()
            order_service_url = 'http://{}:{}/{}'.format(ip, port,target_service)
            if command in action_to_method_get:
                if target_service in action_to_method_get[command]:
                    make_get_request(order_service_url, line)
                else:
                    print(target_service)
                    raise ValueError(f"Invalid service target")
            elif target_service in action_to_method_post:
                if command in action_to_method_post[target_service]:
                    make_post_request(order_service_url, line)
                else:
                    raise ValueError(f"Invalid service target 1")
            else:
                raise ValueError(f"Invalid service target 2")
    except FileNotFoundError:
        print(f"Error: workload file '{workload_file_path}' not found.")
        sys.exit(1)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python workload_parser.py <config_file.json> <workload_file.txt>")
        sys.exit(1)
    config_file_path = sys.argv[1]
    workload_file_path = sys.argv[2]
    main(config_file_path, workload_file_path)

