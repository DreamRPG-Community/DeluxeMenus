# Paper 1.12.2 compatibility branch

This branch follows HelpChat's upstream source. The initial port is based on upstream
commit `f19dfc536951b5c41f1d65d7c30798134ef031d9`. Keep `main` on the upstream line;
merge upstream updates into this branch and rerun the Paper 1.12.2 checks.

## Build and run

Use JDK 21 and the checked-in Gradle wrapper: `./gradlew shadowJar`
(`gradlew.bat shadowJar` on Windows). Install the `*-Paper112.jar` from
`build/libs/` on Paper 1.12.2 (tested on build 1620), with PlaceholderAPI.
Do not install it next to another DeluxeMenus jar. Existing menu files use the
same directory, identifiers, commands and permissions.

## Compatibility boundaries

- Paper-only command registration and component delivery use the Bukkit
  descriptor, command executor and legacy text APIs. Legacy material data is
  applied to the item stack; the anti-duplication marker remains an item NBT tag.
- Modern item components, persistent player metadata and modern-only item
  hooks are not provided by this artifact. Their upstream code stays in the
  tree. A persistent-meta requirement fails closed.
- A single, immediate `[openguimenu]` click on the server thread runs its
  existing action without an extra tick. Menu rendering is still scheduled
  asynchronously and the inventory still opens on the main thread. Ordered
  action lists, delays and non-navigation actions retain their scheduling.

## Release check

Run old and new jars separately on the same Paper 1.12.2 build with the same
configuration. Compare visible menu titles, occupied slots, item ID/data/count,
rendered name/lore styles and enchantments. Exercise the public menu command,
a return path, locked/disabled entries and menu reload. Measure click-to-open
at the Bukkit events so the comparison does not depend on client RTT.
