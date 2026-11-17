<p align="center"><img width="125px" src="./public/SlimeBlock.png" alt="SlimeLink Logo"></p>

<h1 align="center">SlimeLink</h1>
<p align="center">A Skript addon for AdvancedSlimePaper.</p>
<p align="center">
  <a href="https://modrinth.com/plugin/slimelink"><img src="https://img.shields.io/badge/Modrinth-SlimeLink-brightgreen?logo=modrinth&style=for-the-badge" alt="SlimeLink on Modrinth"></a>
</p>

---

## Status

> [!NOTE]
> The current release is **2.0.0-rc.2**, a release candidate ([Modrinth](https://modrinth.com/plugin/slimelink), [GitHub tag `v2.0.0-rc.2`](https://github.com/danielmillar/SlimeLink/releases/tag/v2.0.0-rc.2)). Back up worlds before use.

SlimeLink exposes the AdvancedSlimePaper API to Skript so scripts can create, load, save, and delete worlds.

---

## Requirements

Taken from `build.gradle.kts` and `paper-plugin.yml` for `2.0.0-rc.2`. The [Modrinth version](https://modrinth.com/plugin/slimelink/version/2.0.0-rc.2) lists Minecraft 1.21–1.21.11 on the Paper loader.

- **Java 21**
- **AdvancedSlimePaper** API `4.0.0-SNAPSHOT` (`com.infernalsuite.asp:api` and `com.infernalsuite.asp:loaders`). On startup the plugin loads `com.infernalsuite.asp.AdvancedSlimePaper` and disables itself if that class is missing.
- **Paper** API `1.21.11-R0.1-SNAPSHOT`, with `api-version: '1.21'`
- **Skript 2.14.1**, required, loaded before SlimeLink

## Installation

1. Run AdvancedSlimePaper on Java 21.
2. Install Skript 2.14.1 in `plugins/`.
3. Download [SlimeLink 2.0.0-rc.2](https://modrinth.com/plugin/slimelink) and place the jar in `plugins/`.
4. Start the server. SlimeLink writes `plugins/SlimeLink/sources.yml`. The file loader is available immediately and stores worlds in `slime_worlds` (relative to the server directory). MySQL and MongoDB stay disabled until `enabled` is set in that file.

## Example

These lines use the registered effect and loader syntax. World names may contain only lowercase letters, numbers, hyphens, underscores, periods, and slashes. Create does not load the world. Delete requires the world to be unloaded.

```skript
set {_loader} to new slime loader from file
create slime world named "my_world" with {_loader}
load slime world "my_world" with {_loader}
save slime world "my_world"
unload slime world "my_world"
delete slime world "my_world" from {_loader}
```

A longer script is in [SlimeExample.sk](./SlimeExample.sk).

---

## Documentation

[![SkriptHubViewTheDocs](https://skripthub.net/static/addon/ViewTheDocsButton.png)](https://skripthub.net/docs/?addon=SlimeLink)

---

## Third-Party Code

Portions of this project (configuration structures, utilities, etc.) are derived from  
[AdvancedSlimePaper](https://github.com/InfernalSuite/AdvancedSlimePaper) (GPL v3.0).

---

## Credits

- **ASWM Team & Contributors**
- Paul19988 – ASWM Creator
- ComputerNerd100 – Contributor & Maintainer
- b0ykoe – Build Services & Repository Provider
- Owen1212055 – Contributor & Maintainer
- Gerolmed – Contributor & Maintainer
- Grinderwolf – Original Creator
- Glare – Maven Repository Provider
- Minikloon & Hypixel Team – SRF Development

---

## License

This project is licensed under the GPL v3.0. See [LICENSE](https://github.com/danielmillar/SlimeLink/blob/master/LICENSE) for details.
