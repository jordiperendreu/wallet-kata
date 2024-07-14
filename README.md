# Wallet Kata

This Kata implements the implementation of a Wallet service

Index:
* [Problem definition](./doc/PROBLEM.md)
* [Introduction](#introduction)
  * [How to run](#how-to-run)
  * [CI/CD](#ci-cd)
* [Solution](#solution)
  * [Architecture](#architecture)
  * [Security](#security)
  * [Scalability and concurrency](#scalability-and-concurrency)

# Introduction
The objective of this solution is to build a simplified service for managing a Wallet system. 
Initially only the endpoints include:
* Create wallet (not in the requirements but implemented for easier testing and realism)
* Top-Up providing a Wallet, Amount and CardNumber
* Get Wallet information

Based in the code provided, I decided to use the **SQL version** and followed a layered
structure similar at the one you provided with the existing code

## How to run
To simplify the execution in different environments and avoid the dependency with Java and
maven of your computer or environment, I've prepared a Makefile that only requires Docker. 

Prerequisites:
- Make
- Docker

This Makefile supports the following commands:
* `make dev`: Starts the application in `http://localhost:8090`
* `make test`: Executes all the tests, unit and integration
* `make test-unit`: Executes only the unit tests
* `make test-class`: Executes only the tests of an specific class

Even with the make commands ready to simplify your work, you can still use maven or your
IDE to execute the application or run the tests

## CI/CD
To simplify the process during the development proccess and detect potential errors as soon as 
possible, I've added a Github Action that executes all the tests each time the code is pushed.

# Solution
Based on my experience with Layered Architecture and Hexagonal Architecture + DDD,
I've decided that in order to simplify the readability and simplicity of the kata, the
Layered Architecture is the Go To for the solution.

The implementation of the solution has been done following ATDD (Acceptance Test Driven
Development). ATDD consist in create an Acceptance Tests covering the End To End, to cover
the basic functionality to implement at the beginning, and then use TDD to build the
feature with Unit Tests
TODO: ADD IMAGE

Following this approach helped me to build the Kata with all the code covered by Unit and
Acceptance(Integration) tests

## Architecture

The implementation of the solution is based on a layered architecture. I've doubts if to implement
a Hexagonal Architecture with DDD or a layered architecture, but for simplicity and readability for
this small kata, I thought that will be the best option.

The application is divided in two packages and a shared package:
* `stripeclient`: contains all features related with the Stripe connection (charge, refund)
* `wallet`: contains all features related with the Wallet (create, top-up, get)
* `shared`: contains infrastructure needed related with the application (generic exception handlers)

The layers defined in the production code are:
* api: contains the HTTP communication
* service: contains the business logic
* repository: contains the data access
* dto: contains the data transfer objects
* exception: contains the errors or exceptions
* infrastructure: contains the configuration or handlers

For the tests, I've used the same structure as the production code but adding:
* fakes: contains fake implementations

A typical flow of the application is:
TODO: ADD IMAGE

All the communication between modules is done by the services(and DTOs) to reduce
the coupling between modules. This could be useful in the future in case that we 
want to split the application in multiple services, with that, changing the call to the
service to a HTTP call the information will be the same.

### StripeClient

I did change a little bit the structure of the provided code of the StripeService to allow me to
create more robust tests.
* Created a package `stripeclient` that contains all the code related with the StripeClient
* Extracted the `ChargeRequest` from the `StripeService`. This was important because it allowed me
  to make the unit tests more robust because I could validate the parameters of the API call
  I suppose that the objective of having it internally is because we don't want anyone from outside the module uses this DTO, but
  adding it as protected can solve the problem
* Created different layers to structure the information

I've created two fakes for the testing:
* FakeStripeController: used in the `test-stripe` profile, it simulates the behaviour of the simulated
  stripe server. It allowed me to create IntegrationTests to test the behaviour of the StripeService
* FakeStripeService: used in the `test` profile, it is a simplification of the StripeService. Using
  the same port for all the tests was giving me an error in the tests, so I couldn't use the StripeService
  implemented in this project because the URL is from the configuration. When you put a RANDOM_PORT
  in the tests executions you cannot set the port used in a configuration. So, in order to be able
  to implement IntegrationTests for Wallet, I created a simplified fake version of StripeService

To avoid to create the FakeStripeController and FakeStripeService, having an external service running
in a lightweight docker in local could allow to use the URL in the configuration and all the
IntegrationTests that interact with Stripe will use the StripeService implementation and not a fake.

### Wallet
For the Wallet, I've created the next models:
* Wallet: contains the information of the Wallet
* Transaction: belongs to a Wallet and contains the information of the Transaction

#### Transaction Flow
For the Transactions I've defined a flow in order to make it more real and robust. In production is
very useful to have a transaction flow that helps to make async processes, it helps to recover in 
case of any error, and also to have visiblity of the progress of the transaction.

Transaction flow:
* INITIATED: Just when the Transaction is created. It has defined the Wallet and the amount
* PROCESSED: Once the Transaction is processed in the payment provider but not yet in the Wallet 
* SUCCESS: After being processed by the payment provider, the Transaction is marked as SUCCESS and 
  the Wallet **is updated in the same transaction to ensure the consistency of the data**

TODO: Add IMAGE

### API
The Wallet service exposes the following endpoints:

- **Create Wallet**: `POST /wallets` - Creates a new wallet.
  - **userId**: The ID of the user that owns the wallet.
- **TopUp Wallet**: `POST /wallets/{id}/actions/topup` - Adds funds to a wallet.
  - **cardNumber**: The credit card number associated with the wallet.
  - **amount**: The amount to be added to the wallet.
- **Get Wallet Info**: `GET /wallets/{id}` - Retrieves information about a wallet.

For detailed API specifications, refer to the [OpenAPI specification](./doc/api.yml).

TODO Makefile with the OpenAPI specification
docker run -p 80:8080 -e SWAGGER_JSON=/app/openapi.yaml -v /path/to/openapi.yaml:/app/openapi.yaml swaggerapi/swagger-ui

In the past, I've used in the past dependencies to implement the API documentation in the code, but 
it is to verbose.  You can generate an interface with the definition and then have the 
implementation clean, but I think that will complicate the readability for this kata

## Security
As not requested not user based authentication neither authorization is implemented. 
However, for a production application, you could consider using Spring Security to secure your 
endpoints. 

Spring Security can help you implement authentication (using JWT tokens or OAuth2) and authorization 
(role-based) to ensure that only authorized users can access certain endpoints.

All database interactions in this application are protected against SQL injection by using the ORM 
, which prevent direct execution of SQL queries with user-provided data. Ensuring that all data 
access operations are safe and secure.

## Scalability and concurrency
With the current implementation in terms of concurrency and scalability:

### Concurrent top-ups 
The process of the top-up reads the current wallet amount and updates it adding the top-up amount.
In these cases the two top-ups are executed concurrently, the second top-up that will write the
amount can update the amount based on an outdated value

To solve this problem I've implemented the `version` in the Wallet (optimistic locking). This implies
that when the second top-up is executed in case that the wallet is updated between the read and the write
the update will fail.

In case of optimistic locking exception, the process will read the wallet again will retry up to 3
times in total to update the Wallet amount

### Inconsistencies between Wallet and Transaction
If it is not managed, could happen that the Transaction status and the Wallet amount are not consistent.

To avoid this, I've implemented the different states of the Transaction, so, from PROCESSED to SUCCESS,
the amount and the transaction status are updated in the same transaction.

### Multiple user wallets
Solved with a unique key in the wallet

### Missing transaction ID
With the current solution can happen that the top-up is initiated, the charge in Stripe is done,
but for some reason the PaymentID is not saved in the Transaction (DB connection problems, out of
memory, process killed, ...)

This will cause that we cannot link the payment charge and the information that we have in the database.

Most of the providers offers two solutions (or at least one of the two)
* You "prepare" the payment, and the provider gives you the `payment_id`, so you can link it before
  doing the payment
* You can provide your `order_id` to the payment, so you can link payment and your transaction

### Performance
The indexes of the database currently are the needed ones:
* Wallet: id(primary), userId (unique)
* Transaction: id (primary), walletId (foreign)

### Error handling
Right now **all the errors from the DB or the StripeService are handled**. To simplify the management
in the controller, the error responses are managed by the 'infrastructure' layer in the GlobalExceptionHandler
