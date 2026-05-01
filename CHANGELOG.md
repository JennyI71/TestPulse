# Changelog

All notable changes to TestPulse Flakiness Detector will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-01

### Added
- Initial release of TestPulse Flakiness Detector
- CLI interface with configurable options (--input, --output-dir, --min-runs, --format)
- JSON and text report generation with timestamps
- Flakiness scoring algorithm with configurable weights
- Trend classification (STABLE, NEWLY_FLAKY, CONSISTENTLY_FLAKY, RECOVERING)
- Modular architecture with separate packages for CLI, config, analyser, scoring, reader, report
- Comprehensive unit tests covering analyser, reader, and CLI functionality
- Custom exception handling for configuration, data reading, and help requests
- Javadoc documentation for key classes

### Changed
- Improved error handling with structured exceptions instead of generic IllegalArgumentException
- Stable tests now correctly classified as STABLE rather than flaky
- Invalid timestamps rejected instead of silently replaced
- Flaky-only sections exclude stable tests while overall averages include them

### Fixed
- Trend classification logic for recovering tests
- CLI argument validation and help display