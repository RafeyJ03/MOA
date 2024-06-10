#!/bin/bash

# Compile
compile_helper() {
  echo "Compiling the code..."
  # Assuming the Java files are in the 'src' directory and output to the 'compiled' directory
    mkdir compiled
    
    mkdir compiled/OrderService
    mkdir compiled/ProductService
    mkdir compiled/UserService
    mkdir compiled/ISCS
    cp src/database.db compiled
    javac -cp .:./docs/* -d compiled src/DatabaseManager/DatabaseManager.java
    javac -cp .:./docs/*:compiled -d compiled/OrderService src/OrderService/*.java
    javac -cp .:./docs/*:compiled -d compiled/ProductService src/ProductService/*.java
    javac -cp .:./docs/*:compiled -d compiled/UserService src/UserService/*.java
    javac -cp .:./docs/*:compiled -d compiled/ISCS src/ISCS/*.java
   
  echo "Compilation finished."
}

# Run the workload using workload.txt
run_workload.py() {
  echo "Running workload with file: $1..."
  # Assuming 'workload.py' is designed to take a workload file as an argument
  python3 workload.py config.json "$1"
  echo "Workload finished."
}


# Check the first argument
case "$1" in
  -c)
    compile_helper;;
  -w)
    if [ -z "$2" ]; then
      echo "Please provide a workload file."
      exit 1
    fi
    run_workload.py "$2"
    ;;

  -u)
    # Start User Service
    echo "Starting User Service..."
    java -cp compiled/UserService:compiled:docs/* UserService.UserService config.json
    ;;
  -p)
    # Start Product Service
    echo "Starting Product Service..."
    java -cp compiled/ProductService:compiled:docs/* ProductService.ProductService config.json
    ;;
  -i)
    # Start ISCS
    echo "Starting ISCS..."
    java -cp compiled/ISCS:compiled:docs/* ISCS.ISCS config.json
    ;;
  -o)
    # Start Order Service
    echo "Starting Order Service..."
    java -cp compiled/OrderService:compiled:docs/* OrderService.OrderHttpServer config.json
    ;;

  *)
    echo "Usage: $0 {-c|-w|-u|-p|-i|-o workloadfile.txt}"
    exit 1
    ;;
esac
