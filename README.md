# Fess Data Store Example

[![Java CI with Maven](https://github.com/codelibs/fess-ds-example/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-ds-example/actions/workflows/maven.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.codelibs.fess/fess-ds-example/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.codelibs.fess/fess-ds-example)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Overview

This project provides an example implementation of a Data Store extension for [Fess](https://github.com/codelibs/fess), the Enterprise Search Server. The Example Data Store generates synthetic source records and maps them to index fields through the standard Fess data store pipeline, serving as a copy-from template for developers who want to create their own custom data store crawlers.

## Features

- **Synthetic Source Generation**: Creates a configurable number of in-memory source records that stand in for rows/objects retrieved from an external system
- **Script-based Field Mapping**: Maps source fields to index fields via the admin-configured script map (`scriptMap`), exactly like the real data store plugins
- **Configurable Data Size**: Control the number of generated records via the `data.size` parameter
- **Complete Data Store Implementation**: Demonstrates the full `storeData` lifecycle
- **Error Handling**: Includes proper exception handling, abort support, and failure URL management
- **Stats Integration**: Integrates with the Fess crawler statistics system (`CrawlerStatsHelper`)

## Requirements

- Java 21 or higher
- Maven 3.x
- Fess 15.0.0 or higher

## Installation

### Option 1: Download from Maven Repository

Download the latest JAR from [Maven Central](https://repo1.maven.org/maven2/org/codelibs/fess/fess-ds-example/).

### Option 2: Build from Source

```bash
git clone https://github.com/codelibs/fess-ds-example.git
cd fess-ds-example
mvn clean package
```

### Option 3: Plugin Installation

1. Download or build the JAR file
2. Copy the JAR to your Fess plugin directory
3. Restart Fess
4. Follow the [Plugin Administration Guide](https://fess.codelibs.org/15.7/admin/plugin-guide.html) for detailed installation instructions

## Usage

### Configuration

1. In Fess Administration Console, navigate to **Crawl > Data Store**
2. Create a new Data Store configuration
3. Set the **Handler Name** to `ExampleDataStore`
4. Configure parameters:
   - `data.size`: Number of source records to generate (default: 10)
   - `readInterval`: Interval in milliseconds to wait between records (default: 0)
5. Configure the script map to map source fields to index fields

### Example Configuration

**Parameters:**

```
data.size=50
```

**Script:**

```
title=title
content=body
url=url
```

Each generated source record exposes the following fields, which can be referenced from the script map:

| Source field | Description |
| ------------ | ----------- |
| `id`         | Sequential identifier (`0`, `1`, ...) |
| `title`      | `Sample {index}` |
| `body`       | `Sample body text for record {index}` |
| `url`        | `http://fess.codelibs.org/?sample={index}` |
| `created`    | Timestamp when the record was generated |

With the example script above, each of the 50 generated records is indexed as a document whose `title`, `content`, and `url` index fields are derived from the source `title`, `body`, and `url` fields respectively. The mapping is entirely defined by the script map, not hard-coded in the data store.

## Development

### Project Structure

```
src/
├── main/
│   ├── java/org/codelibs/fess/ds/example/
│   │   └── ExampleDataStore.java         # Main data store implementation
│   └── resources/
│       └── fess_ds++.xml                 # Lasta Di component registration
└── test/
    ├── java/org/codelibs/fess/ds/example/
    │   ├── ExampleDataStoreTest.java      # Unit tests
    │   └── UnitDsTestCase.java           # UTFlute base test case (LastaDiTestCase)
    └── resources/
        └── test_app.xml                  # DI configuration for tests
```

### Key Components

- **ExampleDataStore**: Extends `AbstractDataStore` and implements the `storeData` pipeline (source acquisition -> script-based field mapping -> callback store)
- **Component Registration**: Registered as the `exampleDataStore` component via `fess_ds++.xml` for dependency injection
- **Framework Integration**: Built on LastaFlute / Lasta Di, tested with UTFlute (`LastaDiTestCase`)

### Building and Testing

```bash
# Clean build
mvn clean package

# Run tests
mvn test

# Format code
mvn formatter:format

# Check license headers
mvn license:check
```

### Creating Custom Data Stores

This project serves as a template for creating custom data store implementations. Key implementation points:

1. Extend `AbstractDataStore`
2. Implement `getName()` method
3. Implement `storeData()` method:
   - Acquire the raw source records from the external system (replace `createSourceRecord`)
   - Build a `resultMap` from `paramMap` plus the source fields
   - Evaluate each `scriptMap` entry with `convertValue(scriptType, template, resultMap)` and put the results into the `dataMap`
   - Call `callback.store(paramMap, dataMap)` to index the document
4. Register the component in `fess_ds++.xml`
5. Handle crawler statistics (`CrawlerStatsHelper`) and failure URLs (`FailureUrlService`)

The two places you typically customize are marked with `// CUSTOMIZE:` comments in `ExampleDataStore.java`: the source data acquisition and the script type.

## API Reference

### ExampleDataStore Methods

#### `getName()`
Returns the simple class name (`ExampleDataStore`) used as the handler name.

#### `storeData(DataConfig, IndexUpdateCallback, DataStoreParams, Map, Map)`
Generates source records and stores them as documents after applying the script-based field mapping.

**Parameters:**
- `dataConfig`: Data store configuration
- `callback`: Callback for storing generated documents
- `paramMap`: Configuration parameters (including `data.size` and `readInterval`)
- `scriptMap`: Mapping of index field name to a script template evaluated against the source record
- `defaultDataMap`: Default field values copied into every generated document

#### `createSourceRecord(int)`
Builds one synthetic source record. Override or replace this to read from a real external system.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

```
Copyright 2012-2025 CodeLibs Project and the Others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Support

- **Documentation**: [Fess Documentation](https://fess.codelibs.org/)
- **Issues**: [GitHub Issues](https://github.com/codelibs/fess-ds-example/issues)
- **Community**: [Fess Community](https://github.com/codelibs/fess/discussions)

## Related Projects

- [Fess](https://github.com/codelibs/fess) - Enterprise Search Server
- [Fess Data Store Plugins](https://github.com/codelibs?q=fess-ds) - Other data store implementations