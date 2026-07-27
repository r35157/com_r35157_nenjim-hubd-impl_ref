# Module artifact 'com_r35157_nenjim-hubd-impl-ref'
Artifact description comes here

## Project setup
This module was initially created with the following command:
```bash
/usr/local/software/bin/module_setup -V -g com.r35157.nenjim -t libjdk -n hubd
(Script version: 0.0.2.4)
```

## Usage
Explain how to use this module

## Configuration format versions

Every application-owned, human-maintained runtime configuration file starts
with an explicit format version:

```text
FORMAT_VERSION=1
```

Blank lines and full-line comments may precede the declaration, but it must be
the first actual configuration entry. Loading fails before any other content
is parsed when the declaration is missing, duplicated, malformed, misplaced,
or different from the version supported by the corresponding loader.

Versions are independent for each configuration format. A breaking change to
one format, such as `alarms.conf`, does not change any other format's version.
When a breaking format change is made, update both the loader's hardcoded
`SUPPORTED_FORMAT_VERSION` and the declaration in the runtime and example
configuration files. Compatible additions do not require a version increment.

This policy currently applies to:

- `alarms.conf`
- `alarmaction_Console.conf`
- `alarmaction_Pushover.conf`
- `alarmaction_JupiterPerpsPositionIncreaseAlarmAction.conf`
- `alarmaction_JupiterPerpsPositionDecreaseAlarmAction.conf`
- `evelyn.conf`
- `positions.conf`

Third-party configuration formats such as Log4j's XML configuration are
versioned by their owning library and do not use this declaration.

## Release log:
| Date             |   Version      | Description          |
|:-----------------|:--------------:|----------------------|
| 2025/12/26 | v0.0.0 | First dummy release |
