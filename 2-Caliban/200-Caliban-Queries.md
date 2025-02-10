# -----------------------------------------
# GraphQL introspection query.
# (return all types defined in the schema.)
# -----------------------------------------
query IntrospectSchema {
  __schema {
    types {
      name
    }
   }
}


# Introspection: Get all available queries:
query GetQueries {
  __schema {
    queryType {
      fields {
        name
        description
        args {
          name
          type {
            name
          }
        }
      }
    }
  }
}


# ------------------------
# Get all DevOps employees
# ------------------------
query GetDevOpsTeam {
  employees(role: DevOps) {
    name
    role
  }
}
# Expected response:
# {
#   "data": {
#     "employees": [
#       {
#         "name": "Alex",
#         "role": "DevOps"
#       },
#       {
#         "name": "Roberta",
#         "role": "DevOps"
#       }
#     ]
#   }
# }

# ---------------------------
# Get all Software Developers
# ---------------------------
query {
  employees(role: SoftwareDeveloper) {
    name
    role
  }
}

query GetDevelopers {
  employees(role: SoftwareDeveloper) {
    name
    role
  }
}
# Expected response:
# {
#   "data": {
#     "employees": [
#       {
#         "name": "Maria",
#         "role": "SoftwareDeveloper"
#       },
#       {
#         "name": "Peter",
#         "role": "SoftwareDeveloper"
#       }
#     ]
#   }
# }

# ----------------------------------
# Get all Site Reliability Engineers
# ----------------------------------
query GetSREs {
  employees(role: SiteReliabilityEngineer) {
    name
    role
  }
}
# Expected response:
# {
#   "data": {
#     "employees": [
#       {
#         "name": "James",
#         "role": "SiteReliabilityEngineer"
#       },
#       {
#         "name": "Julia",
#         "role": "SiteReliabilityEngineer"
#       }
#     ]
#   }
# }

# ------------------------------
# Find specific employee by name
# ------------------------------
query FindEmployee {
  employee(name: "Maria") {
    name
    role
  }
}
# Expected response:
# {
#   "data": {
#     "employee": {
#       "name": "Maria",
#       "role": "SoftwareDeveloper"
#     }
#   }
# }

# -----------------------------------------
# Find non-existent employee (returns null)
# -----------------------------------------
query FindNonExistentEmployee {
  employee(name: "John") {
    name
    role
  }
}
# Expected response:
# {
#   "data": {
#     "employee": null
#   }
# }

# -------------------------------
# Multiple queries in one request
# -------------------------------
query GetMultipleQueries {
  devOpsTeam: employees(role: DevOps) {
    name
    role
  }
  developers: employees(role: SoftwareDeveloper) {
    name
    role
  }
  siteReliability: employees(role: SiteReliabilityEngineer) {
    name
    role
  }
  specificPerson: employee(name: "James") {
    name
    role
  }
}
# Expected response:
# {
#   "data": {
#     "devOpsTeam": [
#       {
#         "name": "Alex",
#         "role": "DevOps"
#       },
#       {
#         "name": "Roberta",
#         "role": "DevOps"
#       }
#     ],
#     "developers": [
#       {
#         "name": "Maria",
#         "role": "SoftwareDeveloper"
#       },
#       {
#         "name": "Peter",
#         "role": "SoftwareDeveloper"
#       }
#     ],
#     "siteReliability": [
#       {
#         "name": "James",
#         "role": "SiteReliabilityEngineer"
#       },
#       {
#         "name": "Julia",
#         "role": "SiteReliabilityEngineer"
#       }
#     ],
#     "specificPerson": {
#       "name": "James",
#       "role": "SiteReliabilityEngineer"
#     }
#   }
# }