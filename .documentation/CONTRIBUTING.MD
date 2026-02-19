# Contributing to FactoryCore

First off, thank you for considering contributing to FactoryCore! It's people like you that make FactoryCore such a great tool for the Minecraft community.

## 📋 Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [How Can I Contribute?](#how-can-i-contribute)
4. [Development Setup](#development-setup)
5. [Coding Guidelines](#coding-guidelines)
6. [Commit Guidelines](#commit-guidelines)
7. [Pull Request Process](#pull-request-process)
8. [Testing](#testing)
9. [Documentation](#documentation)

---

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code.

### Our Standards

**Positive behavior includes:**
- Using welcoming and inclusive language
- Being respectful of differing viewpoints
- Gracefully accepting constructive criticism
- Focusing on what is best for the community
- Showing empathy towards other community members

**Unacceptable behavior includes:**
- Trolling, insulting/derogatory comments, and personal attacks
- Public or private harassment
- Publishing others' private information without permission
- Other conduct which could reasonably be considered inappropriate

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- IntelliJ IDEA or Eclipse (recommended)
- Git
- A Minecraft 1.21.4 test server
- Basic knowledge of Spigot API

### First Time Setup

1. **Fork the repository**
   ```bash
   # Click the "Fork" button on GitHub
   ```

2. **Clone your fork**
   ```bash
   git clone https://github.com/YOUR_USERNAME/FactoryCore.git
   cd FactoryCore
   ```

3. **Add upstream remote**
   ```bash
   git remote add upstream https://github.com/aithor/FactoryCore.git
   ```

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Set up your IDE**
   - Import as Maven project
   - Set JDK to Java 17
   - Enable annotation processing

---

## How Can I Contribute?

### Reporting Bugs

**Before submitting a bug report:**
- Check the documentation
- Search existing issues
- Test on the latest version
- Verify it's not a configuration issue

**When submitting a bug report, include:**
- **Clear title**: Brief description of the issue
- **Environment**: Server version, Java version, plugin version
- **Steps to reproduce**: Detailed steps to reproduce the behavior
- **Expected behavior**: What you expected to happen
- **Actual behavior**: What actually happened
- **Logs**: Relevant console logs and error messages
- **Screenshots**: If applicable

**Bug Report Template:**
```markdown
**Plugin Version:** 1.21.4
**Server Version:** Paper 1.21.4
**Java Version:** 17

**Description:**
A clear and concise description of what the bug is.

**To Reproduce:**
1. Go to '...'
2. Click on '....'
3. See error

**Expected Behavior:**
A clear description of what you expected to happen.

**Logs:**
```
Paste relevant logs here
```

**Additional Context:**
Add any other context about the problem here.
```

### Suggesting Features

**Before suggesting a feature:**
- Check if it's already planned (see CHANGELOG.md)
- Search existing feature requests
- Consider if it fits the plugin's scope

**Feature Request Template:**
```markdown
**Feature Title:** Brief title

**Problem Statement:**
Describe the problem this feature would solve.

**Proposed Solution:**
Describe your proposed solution in detail.

**Alternatives Considered:**
What other solutions have you considered?

**Additional Context:**
Any other context, mockups, or examples.
```

### Code Contributions

Areas where we welcome contributions:
- Bug fixes
- New features (discuss first!)
- Performance improvements
- Documentation improvements
- Test coverage improvements
- Localization/translations
- Example configurations

---

## Development Setup

### Project Structure
```
FactoryCore/
├── src/main/java/           # Java source files
│   └── me/aithor/factorycore/
│       ├── commands/        # Command handlers
│       ├── gui/            # GUI implementations
│       ├── listeners/      # Event listeners
│       ├── managers/       # Manager classes
│       ├── models/         # Data models
│       └── utils/          # Utility classes
├── src/main/resources/     # Resource files
│   ├── plugin.yml
│   ├── config.yml
│   ├── resources.yml
│   ├── recipes.yml
│   ├── language.yml
│   └── npc.yml
└── pom.xml                 # Maven configuration
```

### Setting Up Test Server

1. **Create test server directory**
   ```bash
   mkdir test-server
   cd test-server
   ```

2. **Download Paper**
   ```bash
   wget https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds/.../paper-1.21.4.jar
   ```

3. **Install dependencies**
   - Vault
   - EssentialsX (for economy)
   - WorldEdit
   - WorldGuard

4. **Run server once**
   ```bash
   java -Xms2G -Xmx4G -jar paper-1.21.4.jar nogui
   ```

5. **Accept EULA**
   ```bash
   echo "eula=true" > eula.txt
   ```

### Development Workflow

1. **Create a branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make changes**
   - Write code
   - Test thoroughly
   - Update documentation

3. **Build and test**
   ```bash
   mvn clean package
   cp target/FactoryCore-1.21.4.jar test-server/plugins/
   # Test in-game
   ```

4. **Commit changes**
   ```bash
   git add .
   git commit -m "feat: add your feature"
   ```

5. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create Pull Request**
   - Go to GitHub
   - Click "New Pull Request"
   - Fill in the template

---

## Coding Guidelines

### Java Style Guide

**General Rules:**
- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- Use meaningful variable names
- Add comments for complex logic
- Follow Java naming conventions

**Naming Conventions:**
```java
// Classes: PascalCase
public class FactoryManager { }

// Methods: camelCase
public void startProduction() { }

// Variables: camelCase
private String factoryId;

// Constants: UPPER_SNAKE_CASE
private static final int MAX_LEVEL = 5;

// Packages: lowercase
package me.aithor.factorycore.managers;
```

**Code Example:**
```java
public class FactoryManager {
    
    private final FactoryCore plugin;
    private final Map<String, Factory> factories;
    
    /**
     * Constructor for FactoryManager
     * @param plugin Main plugin instance
     */
    public FactoryManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.factories = new HashMap<>();
        loadFactories();
    }
    
    /**
     * Get a factory by ID
     * @param id Factory ID
     * @return Factory or null if not found
     */
    public Factory getFactory(String id) {
        return factories.get(id);
    }
    
    /**
     * Create a new factory
     * @param id Factory ID
     * @param regionName WorldGuard region name
     * @param type Factory type
     * @param price Factory price
     * @return Created factory or null if failed
     */
    public Factory createFactory(String id, String regionName, 
                                 FactoryType type, double price) {
        if (factories.containsKey(id)) {
            return null;
        }
        
        // Validate region exists
        if (!WorldGuardUtils.regionExists(regionName)) {
            return null;
        }
        
        Factory factory = new Factory(id, regionName, type, null, price, 1);
        factories.put(id, factory);
        saveAll();
        
        return factory;
    }
}
```

### Best Practices

**Do's:**
- ✅ Use dependency injection
- ✅ Handle exceptions properly
- ✅ Validate input parameters
- ✅ Use Optional for nullable returns
- ✅ Close resources properly (try-with-resources)
- ✅ Use meaningful names
- ✅ Keep methods small and focused
- ✅ Write self-documenting code
- ✅ Add JavaDoc for public methods
- ✅ Use logging instead of System.out

**Don'ts:**
- ❌ Don't use static mutable state
- ❌ Don't catch generic Exception
- ❌ Don't use magic numbers
- ❌ Don't ignore warnings
- ❌ Don't leak resources
- ❌ Don't use System.out/err
- ❌ Don't expose internal collections
- ❌ Don't use raw types

**Error Handling:**
```java
// Good
public Factory getFactory(String id) {
    if (id == null || id.isEmpty()) {
        plugin.getLogger().warning("Invalid factory ID: " + id);
        return null;
    }
    return factories.get(id);
}

// Bad
public Factory getFactory(String id) {
    return factories.get(id); // No validation!
}
```

**Logging:**
```java
// Good
plugin.getLogger().info("Factory created: " + factoryId);
Logger.log("Admin " + admin + " created factory " + factoryId);

// Bad
System.out.println("Factory created"); // Don't use System.out!
```

---

## Commit Guidelines

### Commit Message Format

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```bash
feat(factory): add multi-owner support
fix(production): resolve race condition in production queue
docs(readme): update installation instructions
refactor(managers): simplify factory creation logic
perf(storage): optimize storage lookup performance
```

---

## Pull Request Process

### Before Submitting

1. **Update documentation**
   - Update README.md if needed
   - Update CHANGELOG.md
   - Add/update JavaDoc

2. **Test thoroughly**
   - Test all affected features
   - Test on a live server
   - Check for console errors

3. **Code quality**
   - No compiler warnings
   - Code follows style guide
   - Passes all tests

### Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
How has this been tested?

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No new warnings
- [ ] Tests added/updated
- [ ] Tested on live server
```

### Review Process

1. **Automated checks** must pass
2. **Code review** by maintainer
3. **Testing** by maintainer
4. **Approval** and merge

---

## Testing

### Manual Testing

**Test Checklist:**
- [ ] Plugin loads without errors
- [ ] All commands work correctly
- [ ] GUIs open and function properly
- [ ] Production system works
- [ ] Storage system works
- [ ] Invoice system works
- [ ] NPC interaction works
- [ ] Permissions work correctly
- [ ] Configuration loads properly
- [ ] No memory leaks
- [ ] Performance is acceptable

### Testing Commands

```bash
# Factory creation
/fc admin create test_region steel_mill test_factory 100000

# NPC spawn
/fc admin npc spawn test_factory

# Buy factory
/fc buy test_factory

# Test production
# (Use GUI to test)

# Test storage
# (Use GUI to test)

# Test upgrade
# (Use GUI to test)

# Clean up
/fc admin remove test_factory
```

---

## Documentation

### Types of Documentation

1. **Code Documentation (JavaDoc)**
   ```java
   /**
    * Creates a new factory in the specified region
    * 
    * @param id Unique factory identifier
    * @param regionName WorldGuard region name
    * @param type Type of factory to create
    * @param price Purchase price for the factory
    * @return Created Factory object, or null if creation failed
    * @throws IllegalArgumentException if id is null or empty
    */
   public Factory createFactory(String id, String regionName, 
                                FactoryType type, double price) {
       // Implementation
   }
   ```

2. **User Documentation (README.md)**
   - Keep README up to date
   - Add examples for new features
   - Update command list

3. **Configuration Documentation**
   - Comment all config options
   - Provide examples
   - Explain defaults

4. **API Documentation**
   - Document public API methods
   - Provide usage examples
   - Keep API changelog

---

## Questions?

If you have questions:
- Open a GitHub Discussion
- Join our Discord server
- Email: dev@factorycore.example

---

## Recognition

Contributors will be:
- Listed in README.md
- Mentioned in release notes
- Added to CONTRIBUTORS.md

Thank you for contributing to FactoryCore! 🎉