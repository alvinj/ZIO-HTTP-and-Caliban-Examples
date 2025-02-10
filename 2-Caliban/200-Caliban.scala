//> using scala "3.3"
//> using dep "dev.zio::zio::2.1.14"
//> using dep "dev.zio::zio-http:3.0.1"
//> using dep "com.github.ghostdogpr::caliban::2.9.1"
//> using dep "com.github.ghostdogpr::caliban-quick:2.9.1"
//> using dep "com.github.ghostdogpr::caliban-zio-http:2.9.1"

/**
* PURPOSE: A first Caliban example.

* NOTES: This example comes from the Caliban docs:
         https://zio.dev/ecosystem/community/caliban/

* HOW TO TEST:

# from a web browser:
http://localhost:8080/api/graphiql

# an introspection query to list all the types:
curl -X POST \
    -H "Content-Type: application/json" \
    -d '{"query": "{ __schema { types { name } } }"}' \
    http://localhost:8080/api/graphql

# GraphQL:
query {
    employees(role: SoftwareDeveloper) {
        name
        role
    }
}

# CURL:
curl 'http://localhost:8080/api/graphql' \
      --data-binary '{"query":"query{\n employees(role: SoftwareDeveloper){\n name\n role\n}\n}"}'

 *
 */

import zio.*
import zio.Console.*
import caliban._
import caliban.quick._
import caliban.schema.{Schema, ArgBuilder}
import caliban.schema.Annotations.GQLDescription

/** enumeration representing the different roles an employee can have */
enum Role:
    case SoftwareDeveloper, SiteReliabilityEngineer, DevOps

/** case class representing an Employee with a name and a role */
case class Employee(
    name: String,
    role: Role
)

/** arguments for querying a single employee by their name */
case class EmployeeArgs(name: String)

/** arguments for querying multiple employees by their role */
case class EmployeesArgs(role: Role)

/**
 * GraphQL queries available in the API.
 *
 * @param employees Query to return all employees with a specific role.
 *                  It takes `EmployeesArgs` as input and returns a
 *                  list of matching `Employee`.
 * @param employee Query to find an employee by its name.
 *                 It takes `EmployeeArgs` as input and returns an
 *                 optional `Employee`.
 */
case class Queries(
    @GQLDescription("Return all employees with specific role")
    employees: EmployeesArgs => List[Employee],
    @GQLDescription("Find an employee by its name")
    employee: EmployeeArgs => Option[Employee]
)

/**
 * Main application object for running the Caliban GraphQL server using ZIO.
 */
object CalibanExample101 extends ZIOAppDefault:

    // automatically derive the necessary GraphQL schema and argument builders
    import ArgBuilder.auto.*
    import Schema.auto.*

    /** sample in-memory list of employees to serve as our data source */
    val employees = List(
        Employee("Alex",    Role.DevOps),
        Employee("Maria",   Role.SoftwareDeveloper),
        Employee("James",   Role.SiteReliabilityEngineer),
        Employee("Peter",   Role.SoftwareDeveloper),
        Employee("Julia",   Role.SiteReliabilityEngineer),
        Employee("Roberta", Role.DevOps)
    )

    /**
     * The entry point of the application.
     *
     * This method sets up the GraphQL API with the defined queries
     * and starts the server.
     * It listens on port 8088, exposes the GraphQL API at `/api/graphql`,
     * and provides a GraphiQL interface at `/api/graphiql`.
     */
    override def run =
        // create a GraphQL interpreter using the provided queries
        graphQL(
            RootResolver(
                Queries(
                    // Resolver for the "employees" query: filters the employee list by role
                    args => employees.filter(e => args.role == e.role),
                    // Resolver for the "employee" query: finds an employee by name
                    args => employees.find(e => e.name == args.name)
                )
            )
        ).runServer(  // Run the GraphQL server with the specified configuration
            port = 8080,
            apiPath = "/api/graphql",
            graphiqlPath = Some("/api/graphiql")
        ).exitCode  // Convert the server's result into an exit code for the application




