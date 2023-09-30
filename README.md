# rankquest-cli

[![CI Build](https://github.com/jillesvangurp/rankquest-cli/actions/workflows/pr_master.yaml/badge.svg)](https://github.com/jillesvangurp/rankquest-cli/actions/workflows/pr_master.yaml)

Companion to [rankquest-core](https://github.com/jillesvangurp/rankquest-core) and [rankquest-studio](https://github.com/jillesvangurp/rankquest-studio) that allows you to evaluate search ranking test cases that you create with rankquest-studio on the command line. 

- Create your test cases with [Rankquest Studio](https://rankquest.jillesvangurp.com)
- Export your configuration and testcases as json
- And use this tool to run them

## Docker

```bash
# help
docker run -it --network host -v $(pwd):/rankquest jillesvangurp/rankquest-cli --help
# run the demo scripts (requires elasticsearch)
docker run -it --network host -v $(pwd):/rankquest jillesvangurp/rankquest-cli -c demo/movies-config.json -t demo/testcases.json -v -f
```

## Usage

```bash
./gradlew assemble
java -jar build/libs/rankquest-cli.jar -c demo/movies-config.json -t demo/testcases.json -f -v
```


