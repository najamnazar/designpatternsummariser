# Design Pattern Summariser (DPS) Project

A Java-based tool for automatically detecting design patterns in Java codebases and generating natural language summaries using SimpleNLG. This project analyzes Java source code, identifies common design patterns (Singleton, Factory, Observer, etc.), and produces comprehensive summaries in both JSON and CSV formats.

## 🚀 Quick Start

### Prerequisites

- **Java 8+** (Java 11+ recommended) - [Download here](https://www.oracle.com/java/technologies/downloads/)
- **Maven** (included as wrapper) or standalone installation
- **Git** for cloning the repository

### Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/najamnazar/designpatternsummariser.git
   cd designpatternsummariser
   ```

2. **Prepare your input data:**
   - Place Java project folders in the `input/` directory
   - Each subfolder should contain a complete Java project with source files
   - Note: This summariser processes the projects placed under `input/` and
     produces outputs under `output/` and `swum-output/`. The SWUM-specific
     project (and its generated outputs) is provided in the repository for
     reference only and is excluded from the DPS summarisation run. If you
     want to parse SWUM outputs separately, handle them with their own
     pipeline.

## 🖥️ Running on Different Platforms

### Windows (PowerShell/Command Prompt)

```batch
# Build the project
.\mvnw.cmd clean dependency:copy-dependencies package

# Run the application
java -cp "target/classes;target/dependency/*" dps.Application
```

### Linux/macOS (Bash/Terminal)

```bash
# Build the project
./mvnw clean dependency:copy-dependencies package

# Run the application
java -cp "target/classes:target/dependency/*" dps.Application
```

### Alternative Build Commands

**Using Maven Wrapper (Recommended):**
```bash
# Windows
.\mvnw.cmd clean install
java -cp "target\dependency\*;target\code-summarisation-1.0.0.jar" dps.Application

# Linux/macOS
./mvnw clean install
java -cp "target/dependency/*:target/code-summarisation-1.0.0.jar" dps.Application
```

**Using System Maven:**
```bash
mvn clean dependency:copy-dependencies package
java -cp "target/classes:target/dependency/*" dps.Application
```

## 📁 Project Structure & Output

```
DPS-Design-Pattern-Summariser/
├── input/                          # Place your Java projects here
│   ├── project1/
│   └── project2/
├── output/
│   ├── json-output/                # Detailed JSON analysis results
│   └── summary-output/             # CSV summaries
│       └── project_summary_improved.csv
├── src/main/java/dps/              # Core DPS source code
├── swum-output/                    # SWUM project outputs (excluded from DPS runs)
└── target/                         # Built artifacts
```

### Input Requirements

- **Java source files**: Place complete Java projects in `input/` subdirectories
- **Project structure**: Each input project should maintain its original package structure
- **File format**: `.java` files with standard Java syntax

### Output Formats

1. **JSON Output** (`output/json-output/`):
   - Detailed analysis for each project
   - Complete AST information, method details, and pattern detection results

2. **CSV Summary** (`output/summary-output/project_summary_improved.csv`):
   - Consolidated summaries across all projects
   - Columns: Project Name, Filename, Summary
   - Ready for spreadsheet analysis and reporting

## 🔧 Customization & Reusability

### Supported Design Patterns

- **Creational**: Singleton, Factory Method, Abstract Factory
- **Structural**: Adapter, Decorator, Facade
- **Behavioral**: Observer, Visitor, Memento

### Key Components for Reuse

1. **Pattern Detection** (`src/main/java/dps/designpatternidentifier/`):
   - Modular pattern detection classes
   - Easy to extend with new patterns

2. **Code Analysis** (`src/main/java/dps/projectparser/`):
   - JavaParser-based AST analysis
   - Reusable for other static analysis tools

3. **Summary Generation** (`src/main/java/dps/summarygenerator/`):
   - SimpleNLG-based natural language generation
   - Configurable output formats

### Integration Examples

**As a Library:**
```java
// Example: Using DPS components in your project
// import the classes from their packages
// import dps.projectparser.ParseProject;
// import dps.summarygenerator.Summarise;

ParseProject parser = new ParseProject();
HashMap<String, HashMap> results = parser.parseProject(inputDirectory);

Summarise summariser = new Summarise();
String summary = summariser.summarise(results, patterns, summaries, projectName);
```

**Batch Processing:**
```bash
# Process multiple projects
for project in input/*/; do
    echo "Processing $project"
    java -cp "target/classes:target/dependency/*" dps.Application
done
```

## 📋 Configuration & Advanced Usage

### Memory Configuration

For large codebases, increase JVM memory:
```bash
java -Xmx4g -cp "target/classes:target/dependency/*" dps.Application
```

### Logging & Debugging

The application provides console output for:
- Project processing progress
- Pattern detection results
- Symbol resolution warnings
- CSV generation status

### Performance Considerations

- **Processing time**: Varies with codebase size (typically 1-5 minutes per project)
- **Memory usage**: Scales with number of classes and complexity
- **Output size**: JSON files can be large for complex projects

## 🔍 Troubleshooting

### Common Issues

1. **Symbol Resolution Warnings**: JavaParser may show "Unsolved symbol" warnings - these don't prevent processing
2. **Missing Dependencies**: Run `dependency:copy-dependencies` if classpath issues occur
3. **Java Version**: Ensure Java 8+ is installed and available in PATH

### Platform-Specific Notes

**Windows:**
- Use backslashes (`\`) in file paths within batch scripts
- PowerShell may require execution policy changes: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`

**Linux/macOS:**
- Ensure `mvnw` has execute permissions: `chmod +x mvnw`
- Use forward slashes (`/`) in all path specifications

## 📜 License & Usage Rights

This project is licensed under the **BSD 2-Clause License** (BSD-2-Clause).

### What this means for reuse:

✅ **You CAN:**
- Use this software for any purpose (commercial or non-commercial)
- Study and modify the source code
- Distribute original or modified versions
- Use components in your own projects
- Create proprietary software using this code
- Sell software that includes this code

⚠️ **You MUST:**
- Include the original license and copyright notice in redistributions
- Include the disclaimer of warranties

✅ **You DON'T NEED TO:**
- Make your source code available when distributing
- License your derived works under the same license
- Share modifications or improvements

📖 **For More Details:**
- Full license text: See [LICENSE](LICENSE) file
- BSD License FAQ: https://opensource.org/licenses/BSD-2-Clause
- Open Source Initiative: https://opensource.org/licenses

### Attribution

When using or referencing this work, please cite:
```
DPS-Design Pattern Summariser
Copyright (C) 2025 Najam Nazar
Licensed under BSD 2-Clause License
GitHub: https://github.com/najamnazar/designpatternsummariser
```

## 🤝 Contributing & Support

### Contributing
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Support
- **Issues**: Report bugs or request features via GitHub Issues
- **Documentation**: Additional documentation available in project files
- **Community**: Contributions and improvements welcome!

## ⚠️ Known Limitations

1. **JavaSymbolSolver**: May show import-related warnings that don't affect processing
2. **Pattern Recognition**: Patterns must adhere to standard specifications (Shvets, 2018)
3. **Language Support**: Currently supports Java only
4. **Memory Usage**: Large codebases may require increased JVM heap size

---

**Note**: This README covers the core DPS functionality excluding the experimental SWUM (Software Word Usage Model) components. For SWUM-specific documentation, refer to the SWUM implementation files.
