<div align="center">
<img alt="ASAP Logo" src="doc/asap.svg" width="600"/>

![GitHub License](https://img.shields.io/github/license/rxxuzi/asap)
![GitHub Tag](https://img.shields.io/github/v/tag/rxxuzi/asap)
![Author](https://img.shields.io/badge/author-rxxuzi-70f)

</div>

# asap

ASAP is a modern, GUI-based SSH client built with Scala 3, JavaFX, and Apache SSHD. It provides an intuitive interface for managing SSH connections, file transfers, and remote command execution.



<details>
<summary>Table of Contents</summary>

- [Features](#features)
- [Prerequisites](#Prerequisites)
- [Setup](#environment-setup)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Usage](#usage)
- [Build](#building-from-source)
- [Structure](#project-structure)
- [License](#license)


</details>



## Features

- 🖥️ User-friendly GUI for SSH operations
- 🔐 Secure SSH connections using Apache SSHD
- 📁 Remote file browsing and management
- 🚀 Easy file transfer between local and remote systems
- 💻 Remote command execution with real-time output
- 🌓 Dark mode support for comfortable viewing
- 🛠️ Built with Scala 3 and SBT for modern development practices
- ⚙️ Customizable settings via JSON configuration files

## Prerequisites

- Java Development Kit (JDK) 11 or higher
- Scala 3.3.1
- SBT (Scala Build Tool)
- JavaFX SDK 22.0.1 or compatible version

## Environment Setup

1. Install JDK 11 or higher
2. Install Scala 3.3.1 and SBT
3. Download and install JavaFX SDK
4. Set the `JAVAFX_HOME` environment variable to point to your JavaFX SDK directory

## Quick Start

1. Clone the repository:
   ```bash
   git clone https://github.com/rxxuzi/asap.git
   cd asap
   ```
2. Configure your SSH settings:
   - For Windows users:
     Use the provided create_ssh_config.bat script:
     ```
     create_ssh_config.bat
     ```
   - For Linux/macOS users:
     Use the provided create_ssh_config.sh script:
     ```
     chmod +x create_ssh_config.sh
     ./create_ssh_config.sh
     ```
   - Follow the prompts to enter your SSH connection details.
   - The script will generate an ssh_config.json file.

3. Run the application:
   ```bash
   sbt run
   ```

## Configuration

ASAP uses JSON files for configuration. The main configuration file is typically named `asap.json`, and SSH connection details are stored in separate JSON files.

For detailed information about configuration options and file formats, please refer to the [Configuration Guide](doc/config.md).

## Usage

1. **Connect to SSH**:
   - In the Home tab, enter the path to your SSH JSON configuration file.
   - Click "Connect" to establish the SSH connection instantly.
2. **File Transfer**: Use the Send tab to upload files to the remote server.
3. **File Browsing**: Navigate remote files in the View tab.
4. **Remote Execution**: Run commands on the remote server using the Exec tab.

## Building from Source

To build ASAP from source:

```bash
sbt assembly
```

This will create a fat JAR in the `target/scala-3.3.1/` directory.

## Project Structure

The project uses SBT for build management. Key dependencies include:

- JavaFX 22.0.1
- Apache SSHD 2.13.1
- Gson 2.10.1

For a complete list of dependencies, please refer to the [build.sbt](build.sbt) file.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
