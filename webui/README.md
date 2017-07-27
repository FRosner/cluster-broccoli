## Elm Front-End Code

To build and package the front-end of Cluster Broccoli, you need to have Elm
and NPM installed.

### Setup the Development Environment

- `yarn install`
- `yarn setup`

## Develop

Start the backend with `sbt server/run` from the project root directory, on <http://localhost:9000>.

Then run `yarn start` in this directory to run a hot-reloading development server for the frontend
on <http://localhost:8080>.  This server watches all files and automatically reloads when you make
changes to the code.

### Run the Tests

- `yarn test`

### Compile and Package

- `yarn package`
