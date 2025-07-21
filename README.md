# Fess Data Store Example

[![Java CI with Maven](https://github.com/codelibs/fess-ds-example/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-ds-example/actions/workflows/maven.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.codelibs.fess/fess-ds-example/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.codelibs.fess/fess-ds-example)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Overview

This project provides a sample implementation of a Data Store extension for [Fess](https://github.com/codelibs/fess), the Enterprise Search Server. The Sample Data Store generates mock documents for testing and demonstration purposes, allowing developers to understand how to create custom data store crawlers for Fess.

## Features

- **Sample Document Generation**: Creates configurable number of mock documents with realistic metadata
- **Configurable Data Size**: Control the number of generated documents via the `data.size` parameter
- **Complete Data Store Implementation**: Demonstrates all aspects of implementing a Fess data store
- **Error Handling**: Includes proper exception handling and failure URL management
- **Stats Integration**: Integrates with Fess crawler statistics system

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
4. Follow the [Plugin Administration Guide](https://fess.codelibs.org/13.11/admin/plugin-guide.html) for detailed installation instructions

## Usage

### Configuration

1. In Fess Administration Console, navigate to **Crawl > Data Store**
2. Create a new Data Store configuration
3. Set the **Handler Name** to `SampleDataStore`
4. Configure parameters:
   - `data.size`: Number of sample documents to generate (default: 10)

### Example Configuration

```
Handler Name: SampleDataStore
Parameters:
data.size=50
```

This configuration will generate 50 sample documents with the following structure:

- **URL**: `http://fess.codelibs.org/?sample={index}`
- **Title**: `Sample {index}`
- **Content**: `Sample Test{index}`
- **Host**: `fess.codelibs.org`
- **Site**: `fess.codelibs.org/{index}`

## Development

### Project Structure

```
src/
├── main/java/org/codelibs/fess/ds/sample/
│   └── SampleDataStore.java          # Main data store implementation
└── test/java/org/codelibs/fess/ds/sample/
    └── SampleDataStoreTest.java      # Unit tests
```

### Key Components

- **SampleDataStore**: Extends `AbstractDataStore` and implements the core data generation logic
- **Component Registration**: Configured via `fess_ds++.xml` for dependency injection
- **Framework Integration**: Built on LastaFlute framework with DBFlute support

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
3. Implement `storeData()` method with proper error handling
4. Register component in `fess_ds++.xml`
5. Handle crawler statistics and failure URLs

## API Reference

### SampleDataStore Methods

#### `getName()`
Returns the simple class name for identification.

#### `storeData(DataConfig, IndexUpdateCallback, DataStoreParams, Map, Map)`
Generates and stores sample documents based on configuration parameters.

**Parameters:**
- `dataConfig`: Data store configuration
- `callback`: Callback for storing generated documents
- `paramMap`: Configuration parameters (including `data.size`)
- `scriptMap`: Script mapping (unused in this implementation)
- `defaultDataMap`: Default data mapping (unused in this implementation)

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