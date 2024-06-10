Compile and Execute:

1. Ensure you are located in the root directory. To compile every service (Order, Product, ISCS, User) run the command:

    ./runme.sh -c

This will also make a compiled directory.. If one would like to recompile the code they must delete the created compiled directory.

   2. To execute and start Product Service, run the command:

    ./runme.sh -p

   This handles product requests.


3. To execute and start User Service, run the command:

    ./runme.sh -u

  This handles user requests.


4. To execute and start InterService Communication Service (ISCS), run the command:

    ./runme.sh -i

   InterService Communication Service mediates communication in between services.

5. To execute and start Order Service, run the command:

    ./runme.sh -o

   This command handles orders through Order Service

6. In order to run workload.py with a workload file, run the command:

    ./runme.sh -w <workloadfile>

   Where `<workloadfile>` is the path to the workload file to be used. Workload.py is a parser which simulates the given workload.

Permissions might need to be granted in order to execute ‘runme.sh’. This can be achieved with the following command: chmod +x runme.sh